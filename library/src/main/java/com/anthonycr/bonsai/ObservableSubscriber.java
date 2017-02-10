package com.anthonycr.bonsai;

import android.support.annotation.Nullable;

/**
 * Created by anthonycr on 2/8/17.
 */

public interface ObservableSubscriber<T> extends CompletableSubscriber {

    /**
     * Called when the Observer emits an
     * item. It can be called multiple times.
     * It cannot be called after onComplete
     * has been called.
     *
     * @param item the item that has been emitted,
     *             can be null.
     */
    void onNext(@Nullable T item);

}
