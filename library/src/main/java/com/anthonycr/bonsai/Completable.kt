package com.anthonycr.bonsai

/**
 * Created by anthonycr on 9/9/17.
 */
class Completable private constructor(private val onSubscribe: (Subscriber) -> Unit) {

    companion object {
        @JvmStatic
        fun complete() = Completable({ it.onComplete() })

        @JvmStatic
        fun defer(block: () -> Unit) = Completable({
            block()
            it.onComplete()
        })

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

    fun subscribeOn(scheduler: Scheduler): Completable {
        subscriptionScheduler = scheduler
        return this
    }

    fun observeOn(scheduler: Scheduler): Completable {
        observationScheduler = scheduler
        return this
    }

    fun subscribe(onComplete: () -> Unit = {},
                  onError: (Throwable) -> Unit = { throw ReactiveEventException("No error handler supplied", it) }) =
            performSubscribe(subscriptionScheduler, observationScheduler, onSubscribe, onComplete, onError)

}