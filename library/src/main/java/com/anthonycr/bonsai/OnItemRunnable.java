package com.anthonycr.bonsai;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by anthonycr on 2/8/17.
 */
class OnItemRunnable<T> implements Runnable {
    @NonNull private final SingleOnSubscribe<T> onSubscribe;
    @Nullable private final T item;

    OnItemRunnable(@NonNull SingleOnSubscribe<T> onSubscribe, @Nullable T item) {
        this.onSubscribe = onSubscribe;
        this.item = item;
    }

    @Override
    public void run() {
        onSubscribe.onItem(item);
    }
}
