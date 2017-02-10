package com.anthonycr.bonsai;

import android.support.annotation.Nullable;

/**
 * Created by anthonycr on 2/8/17.
 */

public interface SingleSubscriber<T> extends CompletableSubscriber {

    /**
     * Called when the Observer emits an
     * item. It can be called one time.
     * It cannot be called after onComplete
     * has been called.
     *
     * @param item the item that has been emitted,
     *             can be null.
     */
    void onItem(@Nullable T item);

}
