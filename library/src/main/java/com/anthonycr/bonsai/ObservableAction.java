package com.anthonycr.bonsai;

import android.support.annotation.NonNull;

/**
 * Created by anthonycr on 2/8/17.
 */
public interface ObservableAction<T> {
    /**
     * Should be overridden to send the subscriber
     * events such as {@link CompletableSubscriber#onNext(Object)}
     * or {@link CompletableSubscriber#onComplete()}.
     *
     * @param subscriber the subscriber that is sent in
     *                   when the user of the Observable
     *                   subscribes.
     */
    void onSubscribe(@NonNull ObservableSubscriber<T> subscriber);
}
