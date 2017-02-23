/*
 * Copyright (C) 2017 Anthony C. Restaino
 * <p/>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.anthonycr.bonsai;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * An implementation of the {@link ObservableSubscriber}
 * that wraps a {@link ObservableOnSubscribe}, executes
 * the callbacks on the correct threads, and throws the
 * appropriate errors when certain rules are violated.
 */
@SuppressWarnings("WeakerAccess")
public class ObservableSubscriberWrapper<T extends ObservableOnSubscribe> implements ObservableSubscriber {

    private volatile boolean onStartExecuted = false;
    volatile boolean onErrorExecuted = false;

    @Nullable private final Scheduler observerThread;
    @NonNull private final Scheduler defaultThread;
    @Nullable protected volatile T onSubscribe;

    public ObservableSubscriberWrapper(@Nullable T onSubscribe,
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
    public boolean isUnsubscribed() {
        return onSubscribe == null;
    }

    @Override
    public void onStart() {
        T onSubscribe = this.onSubscribe;

        if (onStartExecuted) {
            throw new RuntimeException("onStart is called internally, do not call it yourself");
        } else if (onSubscribe != null) {
            executeOnObserverThread(new OnStartRunnable(onSubscribe));
        }

        onStartExecuted = true;
    }

    @Override
    public void onError(@NonNull final Throwable throwable) {
        T onSubscribe = this.onSubscribe;

        if (onSubscribe != null) {
            executeOnObserverThread(new OnErrorRunnable(onSubscribe, throwable));
        }

        onErrorExecuted = true;

        unsubscribe();
    }

}
