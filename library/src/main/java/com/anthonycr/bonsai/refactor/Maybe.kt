package com.anthonycr.bonsai.refactor

import com.anthonycr.bonsai.*

/**
 * Created by anthonycr on 9/9/17.
 */
class Maybe<T> private constructor(private val onSubscribe: (Subscriber<T>) -> Unit) {

    companion object {
        @JvmStatic
        fun <R> empty() = Maybe<R>({ it.onComplete() })

        @JvmStatic
        fun <R> just(value: R) = Maybe<R>({ it.onSuccess(value) })

        @JvmStatic
        fun <R> defer(block: () -> R) = Maybe<R>({ it.onSuccess(block()) })

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

    fun subscribeOn(scheduler: Scheduler): Maybe<T> {
        subscriptionScheduler = scheduler
        return this
    }

    fun observeOn(scheduler: Scheduler): Maybe<T> {
        observationScheduler = scheduler
        return this
    }

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

    fun subscribe(onSuccess: (T) -> Unit = {},
                  onComplete: () -> Unit = {},
                  onError: (Throwable) -> Unit = { throw ReactiveEventException("No error handler supplied", it) }) =
            performSubscribe(subscriptionScheduler, observationScheduler, onSubscribe, onSuccess, onComplete, onError)

}