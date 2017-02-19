package com.anthonycr.bonsai;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * An implementation of the {@link StreamSubscriber}
 * that wraps a {@link StreamOnSubscribe}, executes
 * the callbacks on the correct threads, and throws the
 * appropriate errors when certain rules are violated.
 */
class StreamSubscriberWrapper<T> extends CompletableSubscriberWrapper<StreamOnSubscribe<T>> implements StreamSubscriber<T> {

    StreamSubscriberWrapper(@Nullable StreamOnSubscribe<T> onSubscribe, @Nullable Scheduler observerThread, @NonNull Scheduler defaultThread) {
        super(onSubscribe, observerThread, defaultThread);
    }

    @Override
    public void onNext(@Nullable T item) {
        StreamOnSubscribe<T> onSubscribe = this.onSubscribe;

        if (onCompleteExecuted) {
            throw new RuntimeException("onNext should not be called after onComplete has been called");
        } else if (onSubscribe != null && !onErrorExecuted) {
            executeOnObserverThread(new OnNextRunnable<>(onSubscribe, item));
        } else {
            // Subscription has been unsubscribed, ignore it
        }
    }
}
