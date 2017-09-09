package com.anthonycr.bonsai.refactor

import com.anthonycr.bonsai.Scheduler
import com.anthonycr.bonsai.Schedulers
import com.anthonycr.bonsai.Subscription

/**
 * Created by anthonycr on 9/9/17.
 */
class Stream<T> private constructor(private val onSubscribe: (Subscriber<T>) -> Unit) {

    companion object {
        @JvmStatic
        fun <R> create(block: (Subscriber<R>) -> Unit = { it.onComplete() }) = Stream(block)

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
                        if (schedulingSubscriber.isUnsubscribed) {
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

        fun onNext(t: T)

        fun onComplete()

        fun onError(throwable: Throwable)

    }

    private class ComposingSubscriber<in T>(private var onNext: (T) -> Unit,
                                            private var onComplete: () -> Unit,
                                            private var onError: (Throwable) -> Unit) : Subscriber<T> {
        override fun onNext(t: T) = onNext.invoke(t)

        override fun onComplete() = onComplete.invoke()

        override fun onError(throwable: Throwable) = onError.invoke(throwable)
    }

    private class SchedulingSubscriber<in T>(private val scheduler: Scheduler,
                                             private var composingSubscriber: ComposingSubscriber<T>?) : Subscriber<T>, Subscription {

        private var onCompleteExecuted = false
        private var onErrorExecuted = false

        override fun unsubscribe() {
            composingSubscriber = null
        }

        override fun isUnsubscribed() = composingSubscriber == null

        override fun onNext(t: T) {
            requireCondition(!onCompleteExecuted) { "onNext must not be called after onComplete has been called" }
            requireCondition(!onErrorExecuted) { "onNext must not be called after onError has been called" }
            composingSubscriber?.onNext(t)
        }

        override fun onComplete() {
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

    fun subscribeOn(scheduler: Scheduler): Stream<T> {
        subscriptionScheduler = scheduler
        return this
    }

    fun observeOn(scheduler: Scheduler): Stream<T> {
        observationScheduler = scheduler
        return this
    }

    fun <R> map(map: (T) -> R): Stream<R> {
        return create<R>({ newOnSubscribe ->
            performSubscribe(
                    Schedulers.immediate(),
                    Schedulers.immediate(),
                    onSubscribe,
                    onNext = { newOnSubscribe.onNext(map(it)) },
                    onComplete = { newOnSubscribe.onComplete() },
                    onError = { newOnSubscribe.onError(it) }
            )
        }).subscribeOn(subscriptionScheduler)
                .observeOn(observationScheduler)
    }

    fun subscribe(onNext: (T) -> Unit = {},
                  onComplete: () -> Unit = {},
                  onError: (Throwable) -> Unit = { throw IllegalStateException("No error handler supplied", it) }) =
            performSubscribe(subscriptionScheduler, observationScheduler, onSubscribe, onNext, onComplete, onError)

}