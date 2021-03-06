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
 * A Reactive Streams Kotlin implementation of a publisher that emits one item or one error. This
 * class allows work to be done on a certain thread and then allows an item to be emitted on a
 * different thread.
 *
 * It allows the caller of this class to create a single task that emits a single item and then
 * completes, or if no item can be emitted, then an error must be emitted. The consumer of the
 * [Single] will be notified of the success of the subscription or the failure of it.
 *
 * @param [T] the type that the [Single] will emit.
 */
class Single<T> private constructor(private val onSubscribe: (Subscriber<T>) -> Unit) {

    companion object {
        /**
         * Creates a [Single] that emits an error, a [RuntimeException].
         *
         * @param [R] the type emitted by this [Single].
         */
        @JvmStatic
        fun <R> error() = Single<R>({ it.onError(RuntimeException("No item emitted")) })

        /**
         * Creates a [Single] that emits the item passed as the parameter.
         *
         * @param value the value to emit.
         * @param [R] the type emitted by this [Single].
         */
        @JvmStatic
        fun <R> just(value: R) = Single<R>({ it.onSuccess(value) })

        /**
         * Creates a [Single] that emits the value returned by the lambda.
         *
         * @param [R] the type emitted by this [Single].
         */
        @JvmStatic
        fun <R> defer(block: () -> R) = Single<R>({ it.onSuccess(block()) })

        /**
         * Creates a [Single] from the [(Subscriber<R>) -> Unit] block. Callers of this method must
         * manually call item emission, error, and completion events correctly, unlike [defer].
         *
         * @param [R] the type emitted by this [Single].
         */
        @JvmStatic
        fun <R> create(block: (Subscriber<R>) -> Unit) = Single(block)

        @JvmStatic
        private fun <R> performSubscribe(subscriptionScheduler: Scheduler,
                                         observationScheduler: Scheduler,
                                         onSubscribe: (Subscriber<R>) -> Unit,
                                         onSuccess: (R) -> Unit,
                                         onError: (Throwable) -> Unit): Subscription {
            val composingSubscriber = ComposingSubscriber(onSuccess, onError)
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
        fun onSuccess(t: T)

        fun onError(throwable: Throwable)
    }

    interface Subscriber<in T> : Consumer<T>, Subscription

    private class ComposingSubscriber<in T>(private var onSuccess: (T) -> Unit,
                                            private var onError: (Throwable) -> Unit) : Consumer<T> {
        override fun onSuccess(t: T) = onSuccess.invoke(t)

        override fun onError(throwable: Throwable) = onError.invoke(throwable)
    }

    private class SchedulingSubscriber<in T>(private val scheduler: Scheduler,
                                             private var composingSubscriber: ComposingSubscriber<T>?) : Subscriber<T> {

        private var onSuccessExecuted = false
        private var onErrorExecuted = false

        override fun unsubscribe() {
            composingSubscriber = null
        }

        override fun isUnsubscribed() = composingSubscriber == null

        override fun onSuccess(t: T) = scheduler.execute {
            requireCondition(!onSuccessExecuted) { "onSuccess must not be called multiple times" }
            requireCondition(!onErrorExecuted) { "onSuccess must not be called after onError" }
            onSuccessExecuted = true
            composingSubscriber?.onSuccess(t)
            unsubscribe()
        }

        override fun onError(throwable: Throwable) = scheduler.execute {
            requireCondition(!onErrorExecuted) { "onError must not be called multiple times" }
            requireCondition(!onSuccessExecuted) { "onError must not be called after onSuccess" }
            onErrorExecuted = true
            composingSubscriber?.onError(throwable)
            unsubscribe()
        }
    }

    private var subscriptionScheduler = Schedulers.immediate()
    private var observationScheduler = Schedulers.immediate()

    /**
     * Causes the [Single] to perform work on the provided [Scheduler]. If no [Scheduler] is
     * provided, then the work is performed synchronously.
     */
    fun subscribeOn(scheduler: Scheduler): Single<T> {
        subscriptionScheduler = scheduler
        return this
    }

    /**
     * Causes the [Single] to run emission events on the provided [Scheduler]. If no [Scheduler] is
     * provided, then the events are emitted on the [Scheduler] provided by [subscribeOn].
     */
    fun observeOn(scheduler: Scheduler): Single<T> {
        observationScheduler = scheduler
        return this
    }

    /**
     * Maps from the current [Single] of type [T] to a new [Single] of type [R].
     *
     * @param [R] the type to be emitted by the new [Single].
     */
    fun <R> map(map: (T) -> R): Single<R> {
        return create<R>({ newOnSubscribe ->
            performSubscribe(
                    Schedulers.immediate(),
                    Schedulers.immediate(),
                    onSubscribe,
                    onSuccess = { newOnSubscribe.onSuccess(map(it)) },
                    onError = { newOnSubscribe.onError(it) }
            )
        }).subscribeOn(subscriptionScheduler)
                .observeOn(observationScheduler)
    }

    /**
     * Filters the [Single] using the filter provided to this function and returns a [Maybe].
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
                    onError = { newOnSubscribe.onError(it) }
            )
        }.subscribeOn(subscriptionScheduler)
                .observeOn(observationScheduler)
    }

    /**
     * Subscribes the consumer to receive success and error events. If no [onError] is provided and
     * an error is emitted, then an exception is thrown.
     */
    @JvmOverloads
    fun subscribe(onSuccess: (T) -> Unit = {},
                  onError: (Throwable) -> Unit = { throw ReactiveEventException("No error handler supplied", it) }) =
            performSubscribe(subscriptionScheduler, observationScheduler, onSubscribe, onSuccess, onError)

}