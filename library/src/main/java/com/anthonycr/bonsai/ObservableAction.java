package com.anthonycr.bonsai;

import android.support.annotation.NonNull;

/**
 * Created by anthonycr on 2/8/17.
 */
public interface ObservableAction<T> {
    /**
     * Should be overridden to send the subscriber
     * events such as {@link ObservableSubscriber#onNext(Object)}
     * or {@link ObservableSubscriber#onComplete()}.
     *
     * @param subscriber the subscriber that is sent in
     *                   when the user of the Observable
     *                   subscribes.
     */
    void onSubscribe(@NonNull ObservableSubscriber<T> subscriber);
}
