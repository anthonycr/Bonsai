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
 * An implementation of the {@link CompletableSubscriber}
 * that wraps a {@link CompletableOnSubscribe}, executes
 * the callbacks on the correct threads, and throws the
 * appropriate errors when certain rules are violated.
 */
@SuppressWarnings("WeakerAccess")
class CompletableSubscriberWrapper<T extends CompletableOnSubscribe> extends ObservableSubscriberWrapper<T> implements CompletableSubscriber {

    protected volatile boolean onCompleteExecuted;

    CompletableSubscriberWrapper(@Nullable T onSubscribe,
                                 @Nullable Scheduler observerThread,
                                 @NonNull Scheduler defaultThread) {
        super(onSubscribe, observerThread, defaultThread);
    }

    @Override
    public void onComplete() {
        T onSubscribe = this.onSubscribe;

        if (onCompleteExecuted) {
            throw new RuntimeException("onComplete called more than once");
        } else if (onSubscribe != null && !onErrorExecuted) {
            executeOnObserverThread(new OnCompleteRunnable(onSubscribe));
        }

        onCompleteExecuted = true;

        unsubscribe();
    }
}
