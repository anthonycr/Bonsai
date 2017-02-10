package com.anthonycr.bonsai;

import android.support.annotation.NonNull;

/**
 * Created by anthonycr on 2/8/17.
 */
public interface SingleAction<T> {

    /**
     * Should be overridden to send the subscriber
     * events such as {@link SingleSubscriber#onItem(Object)}
     * or {@link SingleSubscriber#onComplete()}.
     *
     * @param subscriber the subscriber that is sent in
     *                   when the user of the Observable
     *                   subscribes.
     */
    void onSubscribe(@NonNull SingleSubscriber<T> subscriber);
}
