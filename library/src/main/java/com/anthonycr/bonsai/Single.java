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
 * A reactive Java implementation. This class allows work
 * to be done on a certain thread and then allows
 * items to be emitted on a different thread. It is
 * a replacement for {@link android.os.AsyncTask}.
 * <p>
 * It allows the caller of this class to create a single
 * task that optionally emits a single item and then completes.
 * The consumer of the {@link Single} will be notified
 * of the start and completion of the action, as well as the
 * item that could be emitted, or errors that occur.
 *
 * @param <T> the type that the Single will emit.
 */
@SuppressWarnings("WeakerAccess")
public class Single<T> extends Observable<SingleAction<T>, SingleOnSubscribe<T>, SingleSubscriber<T>> {

    private Single(@NonNull SingleAction<T> action) {
        super(action);
    }

    /**
     * Static creator method that creates an Single from the
     * {@link SingleAction} that is passed in as the parameter. Action
     * must not be null.
     *
     * @param action the Action to perform
     * @param <T>    the type that will be emitted to the onSubscribe
     * @return a valid non-null Single.
     */
    @NonNull
    public static <T> Single<T> create(@NonNull SingleAction<T> action) {
        Preconditions.checkNonNull(action);
        return new Single<>(action);
    }

    /**
     * Static creator that creates an Single that is empty
     * and emits no items, but completes immediately.
     *
     * @param <T> the type that will be emitted to the onSubscribe
     * @return a valid non-null empty Single.
     */
    @NonNull
    public static <T> Single<T> empty() {
        return new Single<>(new SingleAction<T>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<T> subscriber) {
                subscriber.onComplete();
            }
        });
    }

    @NonNull
    @Override
    protected SingleSubscriber<T> createSubscriberWrapper(@Nullable SingleOnSubscribe<T> onSubscribe, @Nullable Scheduler observerThread, @NonNull Scheduler defaultThread) {
        return new SingleSubscriberWrapper<>(onSubscribe, observerThread, defaultThread);
    }

}
