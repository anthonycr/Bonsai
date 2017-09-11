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
 * A Reactive Streams Kotlin implementation of a publisher that emits a completion event or an
 * error. This class allows work to be done on a certain thread and then allows the completion or
 * error event to be emitted on a different thread.
 */
class Completable private constructor(private val onSubscribe: (Subscriber) -> Unit) {

    companion object {

        /**
         * Creates a [Completable] that completes immediately.
         */
        @JvmStatic
        fun complete() = Completable({ it.onComplete() })

        /**
         * Creates a [Completable] that runs the deferred work and then completes.
         */
        @JvmStatic
        fun defer(block: () -> Unit) = Completable({
            block()
            it.onComplete()
        })

        /**
         * Creates a [Completable] that requires the creator to manually handle notifying of
         * completion and error event.
         */
        @JvmStatic
        fun create(block: (Subscriber) -> Unit) = Completable(block)

        @JvmStatic
        private fun performSubscribe(subscriptionScheduler: Scheduler,
                                     observationScheduler: Scheduler,
                                     onSubscribe: (Subscriber) -> Unit,
                                     onComplete: () -> Unit,
                                     onError: (Throwable) -> Unit): Subscription {
            val composingSubscriber = ComposingSubscriber(onComplete, onError)
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

    interface Consumer {

        fun onComplete()

        fun onError(throwable: Throwable)

    }

    interface Subscriber : Consumer, Subscription

    private class ComposingSubscriber(private var onComplete: () -> Unit,
                                      private var onError: (Throwable) -> Unit) : Consumer {
        override fun onComplete() = onComplete.invoke()

        override fun onError(throwable: Throwable) = onError.invoke(throwable)
    }

    private class SchedulingSubscriber(private val scheduler: Scheduler,
                                       private var composingSubscriber: ComposingSubscriber?) : Subscriber {

        private var onCompleteExecuted = false
        private var onErrorExecuted = false

        override fun unsubscribe() {
            composingSubscriber = null
        }

        override fun isUnsubscribed() = composingSubscriber == null

        override fun onComplete() = scheduler.execute {
            requireCondition(!onCompleteExecuted) { "onComplete must not be called multiple times" }
            requireCondition(!onErrorExecuted) { "onComplete must not be called after onError" }
            onCompleteExecuted = true
            composingSubscriber?.onComplete()
            unsubscribe()
        }

        override fun onError(throwable: Throwable) = scheduler.execute {
            requireCondition(!onErrorExecuted) { "onError must not be called multiple times" }
            requireCondition(!onCompleteExecuted) { "onError must not be called after onComplete" }
            onErrorExecuted = true
            composingSubscriber?.onError(throwable)
            unsubscribe()
        }
    }

    private var subscriptionScheduler = Schedulers.immediate()
    private var observationScheduler = Schedulers.immediate()

    /**
     * Causes the [Completable] to perform work on the provided [Scheduler]. If no [Scheduler] is
     * provided, then the work is performed synchronously.
     */
    fun subscribeOn(scheduler: Scheduler): Completable {
        subscriptionScheduler = scheduler
        return this
    }

    /**
     * Causes the [Completable] to run emission events on the provided [Scheduler]. If no
     * [Scheduler] is provided, then the items are emitted on the [Scheduler] provided by
     * [subscribeOn].
     */
    fun observeOn(scheduler: Scheduler): Completable {
        observationScheduler = scheduler
        return this
    }

    /**
     * Subscribes the consumer to receive completion and error events. If no [onError] is provided
     * and an error is emitted, then an exception is thrown.
     */
    fun subscribe(onComplete: () -> Unit = {},
                  onError: (Throwable) -> Unit = { throw ReactiveEventException("No error handler supplied", it) }) =
            performSubscribe(subscriptionScheduler, observationScheduler, onSubscribe, onComplete, onError)

}