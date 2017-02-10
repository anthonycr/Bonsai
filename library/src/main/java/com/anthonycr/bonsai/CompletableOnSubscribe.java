package com.anthonycr.bonsai;

import android.support.annotation.NonNull;

/**
 * Created by anthonycr on 2/8/17.
 */
public abstract class CompletableOnSubscribe {

    /**
     * Called when the observable runs into an error
     * that will cause it to abort and not finish.
     * Receiving this callback means that the observable
     * is dead and no {@link #onComplete()} or other callbacks
     * callbacks will be called. Default implementation
     * throws an exception, so you will get a crash
     * if the Observable throws an exception and this
     * method is not overridden. Do not call super when
     * you override this method.
     *
     * @param throwable an optional throwable that could
     *                  be sent.
     */
    public void onError(@NonNull Throwable throwable) {
        throw new RuntimeException("Exception thrown: override onError to handle it", throwable);
    }

    /**
     * Called before the observer begins
     * to process and emit items or complete.
     */
    public void onStart() {}

    /**
     * This method is called when the observer is
     * finished sending the subscriber events. It
     * is guaranteed that no other methods will be
     * called on the OnSubscribe after this method
     * has been called.
     */
    public void onComplete() {}
}
