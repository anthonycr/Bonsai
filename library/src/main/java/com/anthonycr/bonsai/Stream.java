/*
 * Copyright (C) 2016 Anthony C. Restaino
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
 * It allows the caller of this class to create an stream
 * task that can emit multiple items and then complete.
 * The consumer of the {@link Stream} will be notified
 * of the start and completion of the action, as well as the
 * items that can be emitted, or errors that occur.
 *
 * @param <T> the type that the stream will emit.
 */
@SuppressWarnings("WeakerAccess")
public class Stream<T> extends Observable<StreamAction<T>, StreamOnSubscribe<T>, StreamSubscriber<T>> {

    private Stream(@NonNull StreamAction<T> action) {
        super(action);
    }

    /**
     * Static creator method that creates an stream from the
     * {@link StreamAction} that is passed in as the parameter. Action
     * must not be null.
     *
     * @param action the Action to perform
     * @param <T>    the type that will be emitted to the onSubscribe
     * @return a valid non-null stream.
     */
    @NonNull
    public static <T> Stream<T> create(@NonNull StreamAction<T> action) {
        Preconditions.checkNonNull(action);
        return new Stream<>(action);
    }

    /**
     * Static creator that creates an stream that is empty
     * and emits no items, but completes immediately.
     *
     * @param <T> the type that will be emitted to the onSubscribe
     * @return a valid non-null empty stream.
     */
    @NonNull
    public static <T> Stream<T> empty() {
        return new Stream<>(new StreamAction<T>() {
            @Override
            public void onSubscribe(@NonNull StreamSubscriber<T> subscriber) {
                subscriber.onComplete();
            }
        });
    }

    /**
     * Tells the observable what {@link Scheduler} that
     * the onSubscribe work should run on.
     *
     * @param subscribeScheduler the {@link Scheduler} to run the work on.
     * @return returns itself so that calls can be conveniently chained.
     */
    @NonNull
    public final Stream<T> subscribeOn(@NonNull Scheduler subscribeScheduler) {
        setActionScheduler(subscribeScheduler);
        return this;
    }

    /**
     * Tells the observable what {@link Scheduler} that
     * the onSubscribe should observe the work on.
     *
     * @param observerScheduler the {@link Scheduler} to run to callback on.
     * @return returns itself so that calls can be conveniently chained.
     */
    @NonNull
    public final Stream<T> observeOn(@NonNull Scheduler observerScheduler) {
        setObserverScheduler(observerScheduler);
        return this;
    }

    @NonNull
    @Override
    protected StreamSubscriber<T> createSubscriberWrapper(@Nullable StreamOnSubscribe<T> onSubscribe,
                                                          @Nullable Scheduler observerThread,
                                                          @NonNull Scheduler defaultThread) {
        return new StreamSubscriberWrapper<>(onSubscribe, observerThread, defaultThread);
    }
}

