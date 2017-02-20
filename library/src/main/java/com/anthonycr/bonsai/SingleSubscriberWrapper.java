package com.anthonycr.bonsai;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * An implementation of the {@link SingleSubscriber}
 * that wraps a {@link SingleOnSubscribe}, executes
 * the callbacks on the correct threads, and throws the
 * appropriate errors when certain rules are violated.
 */
class SingleSubscriberWrapper<T> extends CompletableSubscriberWrapper<SingleOnSubscribe<T>> implements SingleSubscriber<T> {

    private volatile boolean onItemExecuted = false;

    SingleSubscriberWrapper(@Nullable SingleOnSubscribe<T> onSubscribe,
                            @Nullable Scheduler observerThread,
                            @NonNull Scheduler defaultThread) {
        super(onSubscribe, observerThread, defaultThread);
    }

    @Override
    public void onItem(@Nullable T item) {
        SingleOnSubscribe<T> onSubscribe = this.onSubscribe;

        if (onCompleteExecuted) {
            throw new RuntimeException("onItem should not be called after onComplete has been called");
        } else if (onItemExecuted) {
            throw new RuntimeException("onItem should not be called multiple times");
        } else if (onSubscribe != null && !onErrorExecuted) {
            executeOnObserverThread(new OnItemRunnable<>(onSubscribe, item));
        } else {
            // Subscription has been unsubscribed, ignore it
        }

        onItemExecuted = true;
    }
}
