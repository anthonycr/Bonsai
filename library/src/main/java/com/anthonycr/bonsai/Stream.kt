/*
 * Copyright (C) 2017 Anthony C. Restaino
 * <p/>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.anthonycr.bonsai

import com.anthonycr.bonsai.Maybe.Companion.defer

/**
 * A Reactive Streams Kotlin implementation of a publisher that can emit multiple items followed by
 * a completion event or one error. This class allows the work to be done on a certain thread and
 * then allows the events to be emitted on a different thread.
 *
 * It allows the caller of this class to create a single task that emits no or one or many items and
 * then completes, or an error can be emitted.
 *
 * @param [T] the type that the [Stream] will emit.
 */
class Stream<T> private constructor(private val onSubscribe: (Subscriber<T>) -> Unit) {

    companion object {

        /**
         * Creates a [Stream] that emits no items and immediately completes.
         */
        @JvmStatic
        fun <R> empty() = Stream<R>({ it.onComplete() })

        /**
         * Creates a [Stream] that emits all the items passed to it and then completes.
         */
        @JvmStatic
        fun <R> just(vararg values: R) = Stream<R>({ subscriber ->
            for (r in values) {
                subscriber.onNext(r)
            }
            subscriber.onComplete()
        })

        /**
         * Creates a [Stream] which emits all the items held in the collection.
         */
        @JvmStatic
        fun <R> defer(block: () -> Collection<R>) = Stream<R>({ subscriber ->
            block().forEach { subscriber.onNext(it) }
            subscriber.onComplete()
        })

        /**
         * Creates a [Stream] from the [Subscriber<R> -> Unit] block, which requires that the
         * creator notify the consumer of all items, errors, and the completion event manually.
         * If fine grained control over the emission lifecycle is not needed, [defer] offers a less
         * error prone way to create a similar [Stream].
         */
        @JvmStatic
        fun <R> create(block: (Subscriber<R>) -> Unit) = Stream(block)

        @JvmStatic
        private fun <R> performSubscribe(subscriptionScheduler: Scheduler,
                                         observationScheduler: Scheduler,
                                         onSubscribe: (Subscriber<R>) -> Unit,
                                         onNext: (R) -> Unit,
                                         onComplete: () -> Unit,
                                         onError: (Throwable) -> Unit): Subscription {
            val composingSubscriber = ComposingSubscriber(onNext, onComplete, onError)
            val schedulingSubscriber = SchedulingSubscriber(observationScheduler, composingSubscriber)
            subscriptionScheduler.execute {
                try {
                    onSubscribe(schedulingSubscriber)
                } catch (exception: Exception) {
                    if (exception is ReactiveEventException) {
                        throw exception
                    } else {
                        if (schedulingSubscriber.isUnsubscribed()) {
                            throw ReactiveEventException("Exception thrown after unsubscribe", exception)
                        } else {
                            schedulingSubscriber.onError(exception)
                        }
                    }
                }
            }

            return schedulingSubscriber
        }
    }

    interface Consumer<in T> {
        fun onNext(t: T)

        fun onComplete()

        fun onError(throwable: Throwable)
    }

    interface Subscriber<in T> : Consumer<T>, Subscription

    private class ComposingSubscriber<in T>(private var onNext: (T) -> Unit,
                                            private var onComplete: () -> Unit,
                                            private var onError: (Throwable) -> Unit) : Consumer<T> {
        override fun onNext(t: T) = onNext.invoke(t)

        override fun onComplete() = onComplete.invoke()

        override fun onError(throwable: Throwable) = onError.invoke(throwable)
    }

    private class SchedulingSubscriber<in T>(private val scheduler: Scheduler,
                                             private var composingSubscriber: ComposingSubscriber<T>?) : Subscriber<T> {

        private var onCompleteExecuted = false
        private var onErrorExecuted = false

        override fun unsubscribe() {
            composingSubscriber = null
        }

        override fun isUnsubscribed() = composingSubscriber == null

        override fun onNext(t: T) = scheduler.execute {
            requireCondition(!onCompleteExecuted) { "onNext must not be called after onComplete has been called" }
            requireCondition(!onErrorExecuted) { "onNext must not be called after onError has been called" }
            composingSubscriber?.onNext(t)
        }

        override fun onComplete() = scheduler.execute {
            requireCondition(!onCompleteExecuted) { "onComplete must not be called multiple times" }
            requireCondition(!onErrorExecuted) { "onComplete must not be called after onError" }
            onCompleteExecuted = true
            composingSubscriber?.onComplete()
            unsubscribe()
        }

        override fun onError(throwable: Throwable) = scheduler.execute {
            requireCondition(!onErrorExecuted) { "onError must not be called multiple times" }
            requireCondition(!onCompleteExecuted) { "onError must not be called after onSuccess" }
            onErrorExecuted = true
            composingSubscriber?.onError(throwable)
            unsubscribe()
        }
    }

    private var subscriptionScheduler = Schedulers.immediate()
    private var observationScheduler = Schedulers.immediate()

    /**
     * Causes the [Stream] to perform work on the provided [Scheduler]. If no [Scheduler] is
     * provided, then the work is performed synchronously.
     */
    fun subscribeOn(scheduler: Scheduler): Stream<T> {
        subscriptionScheduler = scheduler
        return this
    }

    /**
     * Causes the [Stream] to run emission events on the provided [Scheduler]. If no [Scheduler] is
     * provided, then the events are emitted on the [Scheduler] provided by [subscribeOn].
     */
    fun observeOn(scheduler: Scheduler): Stream<T> {
        observationScheduler = scheduler
        return this
    }

    /**
     * Maps from the current [Stream] of type [T] to a new [Stream] of type [R].
     */
    fun <R> map(map: (T) -> R): Stream<R> {
        return create<R> { newOnSubscribe ->
            performSubscribe(
                    Schedulers.immediate(),
                    Schedulers.immediate(),
                    onSubscribe,
                    onNext = { newOnSubscribe.onNext(map(it)) },
                    onComplete = { newOnSubscribe.onComplete() },
                    onError = { newOnSubscribe.onError(it) }
            )
        }.subscribeOn(subscriptionScheduler)
                .observeOn(observationScheduler)
    }

    /**
     * Filters the [Stream] using the filter provided to this function.
     */
    fun filter(predicate: (T) -> Boolean): Stream<T> {
        return create<T> { newOnSubscribe ->
            performSubscribe(
                    Schedulers.immediate(),
                    Schedulers.immediate(),
                    onSubscribe,
                    onNext = { item ->
                        if (predicate(item)) {
                            newOnSubscribe.onNext(item)
                        }
                    },
                    onComplete = { newOnSubscribe.onComplete() },
                    onError = { newOnSubscribe.onError(it) }
            )
        }.subscribeOn(subscriptionScheduler)
                .observeOn(observationScheduler)
    }

    /**
     * Subscribes the consumer to receive next, completion, and error events. If no [onError] is
     * provided and an error is emitted, then an exception is thrown.
     */
    @JvmOverloads
    fun subscribe(onNext: (T) -> Unit = {},
                  onComplete: () -> Unit = {},
                  onError: (Throwable) -> Unit = { throw ReactiveEventException("No error handler supplied", it) }) =
            performSubscribe(subscriptionScheduler, observationScheduler, onSubscribe, onNext, onComplete, onError)

}