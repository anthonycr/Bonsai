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

/**
 * A Reactive Streams Kotlin implementation of a publisher that might emit an item, or a completion
 * event, or an exception. One of these events is guaranteed to emitted by this observable. This
 * class allows work to be done on a certain thread and then allows the item to be emitted on a
 * different thread.
 *
 * It allows the caller of this class to create a single task that could emit a single item or
 * complete, or emit an error.
 *
 * @param [T] the type that the [Maybe] will emit.
 */
class Maybe<T> private constructor(private val onSubscribe: (Subscriber<T>) -> Unit) {

    companion object {

        /**
         * Creates a [Maybe] that doesn't emit an item.
         */
        @JvmStatic
        fun <R> empty() = Maybe<R>({ it.onComplete() })

        /**
         * Creates a [Maybe] that emits the provided item.
         */
        @JvmStatic
        fun <R> just(value: R) = Maybe<R>({ it.onSuccess(value) })

        /**
         * Creates a [Maybe] that lazily executes the block and emits the value returned by it, or a
         * completion event if no item was returned.
         */
        @JvmStatic
        fun <R> defer(block: () -> R?) = Maybe<R>({ subscriber ->
            block()?.let { subscriber.onSuccess(it) } ?: subscriber.onComplete()
        })

        /**
         * Creates a [Maybe] that requires the creator to manually notify of item emission,
         * completion, or error events. If fine grained control over the emission lifecycle is not
         * needed, [defer] offers a less error prone way to create a similar [Maybe].
         */
        @JvmStatic
        fun <R> create(block: (Subscriber<R>) -> Unit) = Maybe(block)

        @JvmStatic
        private fun <R> performSubscribe(subscriptionScheduler: Scheduler,
                                         observationScheduler: Scheduler,
                                         onSubscribe: (Subscriber<R>) -> Unit,
                                         onSuccess: (R) -> Unit,
                                         onComplete: () -> Unit,
                                         onError: (Throwable) -> Unit): Subscription {
            val composingSubscriber = ComposingSubscriber(onSuccess, onComplete, onError)
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

    interface Subscriber<in T> {

        fun onSuccess(t: T)

        fun onComplete()

        fun onError(throwable: Throwable)

    }

    private class ComposingSubscriber<in T>(private val onSuccess: (T) -> Unit,
                                            private val onComplete: () -> Unit,
                                            private val onError: (Throwable) -> Unit) : Subscriber<T> {
        override fun onSuccess(t: T) = onSuccess.invoke(t)

        override fun onComplete() = onComplete.invoke()

        override fun onError(throwable: Throwable) = onError.invoke(throwable)
    }

    private class SchedulingSubscriber<in T>(private val scheduler: Scheduler,
                                             private var composingSubscriber: ComposingSubscriber<T>?) : Subscriber<T>, Subscription {

        private var onSuccessExecuted = false
        private var onCompleteExecuted = false
        private var onErrorExecuted = false

        override fun unsubscribe() {
            composingSubscriber = null
        }

        override fun isUnsubscribed() = composingSubscriber == null

        override fun onSuccess(t: T) = scheduler.execute {
            requireCondition(!onSuccessExecuted) { "onSuccess must not be called multiple times" }
            requireCondition(!onErrorExecuted) { "onSuccess must not be called after onError" }
            requireCondition(!onCompleteExecuted) { "onSuccess must not be called after onComplete" }
            onSuccessExecuted = true
            composingSubscriber?.onSuccess(t)
            unsubscribe()
        }

        override fun onComplete() = scheduler.execute {
            requireCondition(!onCompleteExecuted) { "onComplete must not be called multiple times" }
            requireCondition(!onErrorExecuted) { "onComplete must not be called after onError" }
            requireCondition(!onSuccessExecuted) { "onComplete must not be called after onSuccess" }
            onCompleteExecuted = true
            composingSubscriber?.onComplete()
            unsubscribe()
        }

        override fun onError(throwable: Throwable) = scheduler.execute {
            requireCondition(!onErrorExecuted) { "onError must not be called multiple times" }
            requireCondition(!onSuccessExecuted) { "onError must not be called after onSuccess" }
            requireCondition(!onCompleteExecuted) { "onError must not be called after onComplete" }
            onErrorExecuted = true
            composingSubscriber?.onError(throwable)
            unsubscribe()
        }
    }

    private var subscriptionScheduler = Schedulers.immediate()
    private var observationScheduler = Schedulers.immediate()

    /**
     * Causes the [Maybe] to perform work on the provided [Scheduler]. If no [Scheduler] is
     * provided, then the work is performed synchronously.
     */
    fun subscribeOn(scheduler: Scheduler): Maybe<T> {
        subscriptionScheduler = scheduler
        return this
    }

    /**
     * Causes the [Maybe] to run emission events on the provided [Scheduler]. If no [Scheduler] is
     * provided, then the events are emitted on the [Scheduler] provided by [subscribeOn].
     */
    fun observeOn(scheduler: Scheduler): Maybe<T> {
        observationScheduler = scheduler
        return this
    }

    /**
     * Maps from the current [Maybe] of type [T] to a new [Maybe] of type [R].
     *
     * @param [R] the type to be emitted by the new [Maybe].
     */
    fun <R> map(map: (T) -> R): Maybe<R> {
        return create<R>({ newOnSubscribe ->
            performSubscribe(
                    Schedulers.immediate(),
                    Schedulers.immediate(),
                    onSubscribe,
                    onSuccess = { newOnSubscribe.onSuccess(map(it)) },
                    onComplete = { newOnSubscribe.onComplete() },
                    onError = { newOnSubscribe.onError(it) }
            )
        }).subscribeOn(subscriptionScheduler)
                .observeOn(observationScheduler)
    }

    /**
     * Filters the [Maybe] using the filter provided to this function.
     */
    fun filter(predicate: (T) -> Boolean): Maybe<T> {
        return Maybe.create<T> { newOnSubscribe ->
            performSubscribe(
                    Schedulers.immediate(),
                    Schedulers.immediate(),
                    onSubscribe,
                    onSuccess = { item ->
                        if (predicate(item)) {
                            newOnSubscribe.onSuccess(item)
                        } else {
                            newOnSubscribe.onComplete()
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
    fun subscribe(onSuccess: (T) -> Unit = {},
                  onComplete: () -> Unit = {},
                  onError: (Throwable) -> Unit = { throw ReactiveEventException("No error handler supplied", it) }) =
            performSubscribe(subscriptionScheduler, observationScheduler, onSubscribe, onSuccess, onComplete, onError)

}