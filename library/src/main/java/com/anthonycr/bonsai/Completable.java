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
 * It allows the caller of this class to create a completable
 * task that runs and then finishes with a completion event.
 * The consumer of the {@link Completable} will be notified
 * of the start and completion of the action, as well as any
 * errors that occur.
 */
@SuppressWarnings("WeakerAccess")
public class Completable extends Observable<CompletableAction, CompletableOnSubscribe, CompletableSubscriber> {

    private Completable(@NonNull CompletableAction action) {
        super(action);
    }

    /**
     * Static creator method that creates a Completable from the
     * {@link CompletableAction} that is passed in as the parameter. Action
     * must not be null.
     *
     * @param action the Action to perform
     * @return a valid non-null Completable.
     */
    @NonNull
    public static Completable create(@NonNull CompletableAction action) {
        Preconditions.checkNonNull(action);
        return new Completable(action);
    }

    /**
     * Static creator that creates a Completable that is empty
     * and emits no items, but completes immediately.
     *
     * @return a valid non-null empty Completable.
     */
    @NonNull
    public static Completable empty() {
        return new Completable(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
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
    public final Completable subscribeOn(@NonNull Scheduler subscribeScheduler) {
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
    public final Completable observeOn(@NonNull Scheduler observerScheduler) {
        setObserverScheduler(observerScheduler);
        return this;
    }

    @NonNull
    @Override
    protected CompletableSubscriber createSubscriberWrapper(@Nullable CompletableOnSubscribe onSubscribe,
                                                            @Nullable Scheduler observerThread,
                                                            @NonNull Scheduler defaultThread) {
        return new CompletableSubscriberWrapper<>(onSubscribe, observerThread, defaultThread);
    }

}
