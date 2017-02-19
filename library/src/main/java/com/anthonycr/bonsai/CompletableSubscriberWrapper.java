package com.anthonycr.bonsai;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * An implementation of the {@link CompletableSubscriber}
 * that wraps a {@link CompletableOnSubscribe}, executes
 * the callbacks on the correct threads, and throws the
 * appropriate errors when certain rules are violated.
 */
class CompletableSubscriberWrapper<T extends CompletableOnSubscribe> implements CompletableSubscriber {

    private volatile boolean onStartExecuted = false;
    volatile boolean onCompleteExecuted = false;
    volatile boolean onErrorExecuted = false;

    @Nullable private final Scheduler observerThread;
    @NonNull private final Scheduler defaultThread;
    @Nullable protected volatile T onSubscribe;

    CompletableSubscriberWrapper(@Nullable T onSubscribe,
                                 @Nullable Scheduler observerThread,
                                 @NonNull Scheduler defaultThread) {
        this.onSubscribe = onSubscribe;
        this.observerThread = observerThread;
        this.defaultThread = defaultThread;
    }

    void executeOnObserverThread(@NonNull Runnable runnable) {
        if (observerThread != null) {
            observerThread.execute(runnable);
        } else {
            defaultThread.execute(runnable);
        }
    }

    @Override
    public void unsubscribe() {
        onSubscribe = null;
    }

    @Override
    public void onComplete() {
        CompletableOnSubscribe onSubscribe = this.onSubscribe;

        if (onCompleteExecuted) {
            throw new RuntimeException("onComplete called more than once");
        } else if (onSubscribe != null && !onErrorExecuted) {
            executeOnObserverThread(new OnCompleteRunnable(onSubscribe));
        }

        onCompleteExecuted = true;

        unsubscribe();
    }

    @Override
    public void onStart() {
        CompletableOnSubscribe onSubscribe = this.onSubscribe;

        if (onStartExecuted) {
            throw new RuntimeException("onStart is called internally, do not call it yourself");
        } else if (onSubscribe != null) {
            executeOnObserverThread(new OnStartRunnable(onSubscribe));
        }

        onStartExecuted = true;
    }

    @Override
    public void onError(@NonNull final Throwable throwable) {
        CompletableOnSubscribe onSubscribe = this.onSubscribe;

        if (onSubscribe != null) {
            executeOnObserverThread(new OnErrorRunnable(onSubscribe, throwable));
        }

        onErrorExecuted = true;

        unsubscribe();
    }

    @Override
    public boolean isUnsubscribed() {
        return onSubscribe == null;
    }
}