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

import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * @param <ActionType>      The {@link Action} that will be provided
 *                          to the {@link OnSubscribeType} when the
 *                          consumer subscribes.
 * @param <OnSubscribeType> The {@link CompletableOnSubscribe} or type that
 *                          extends it that will be supplied when the consumer
 *                          subscribes.
 * @param <SubscriberType>  The {@link CompletableSubscriber} or type that
 *                          extends it that will be supplied to the {@link Action}
 *                          when the consumer subscribes.
 */
@SuppressWarnings("WeakerAccess")
public abstract class Observable<ActionType extends Action<SubscriberType>,
    OnSubscribeType extends CompletableOnSubscribe,
    SubscriberType extends CompletableSubscriber> {

    @NonNull private final ActionType action;
    @Nullable private Scheduler subscriberThread;
    @Nullable private Scheduler observerThread;
    @NonNull private final Scheduler defaultThread;

    protected Observable(@NonNull ActionType action) {
        this.action = action;
        this.defaultThread = getCurrentScheduler();
    }

    @NonNull
    private Scheduler getCurrentScheduler() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        Looper looper = Looper.myLooper();
        Preconditions.checkNonNull(looper);

        return new ThreadScheduler(looper);
    }

    /**
     * Tells the observable what {@link Scheduler} that
     * the onSubscribe work should run on.
     *
     * @param subscribeScheduler the {@link Scheduler} to run the work on.
     * @return returns itself so that calls can be conveniently chained.
     */
    @NonNull
    public final Observable<ActionType, OnSubscribeType, SubscriberType> subscribeOn(@NonNull Scheduler subscribeScheduler) {
        subscriberThread = subscribeScheduler;
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
    public final Observable<ActionType, OnSubscribeType, SubscriberType> observeOn(@NonNull Scheduler observerScheduler) {
        observerThread = observerScheduler;
        return this;
    }

    /**
     * Subscribes immediately to the {@link Observable} and
     * ignores all onComplete calls.
     *
     * @return a work subscription that can be cancelled.
     */
    @NonNull
    public final Subscription subscribe() {
        return startSubscription(null);
    }

    /**
     * Immediately subscribes to the {@link Observable} and
     * starts sending events from the {@link Observable} to the
     * {@link OnSubscribeType}.
     *
     * @param onSubscribe the class that wishes to receive onComplete
     *                    callbacks from the Completable.
     * @return a work subscription that can be cancelled.
     */
    @NonNull
    public final Subscription subscribe(@NonNull OnSubscribeType onSubscribe) {
        Preconditions.checkNonNull(onSubscribe);

        return startSubscription(onSubscribe);
    }

    /**
     * Creates a {@link SubscriberType} that
     * wraps the {@link OnSubscribeType} in order
     * to properly execute method calls on the
     * appropriate observer thread.
     *
     * @param onSubscribe    the {@link OnSubscribeType} supplied when
     *                       the consumer subscribed.
     * @param observerThread the thread that the {@link OnSubscribeType}
     *                       should be notified on, may be null.
     * @param defaultThread  the thread to notify the {@link OnSubscribeType}
     *                       on if the provided observer is null.
     * @return a valid {@link SubscriberType} that wraps the {@link OnSubscribeType}.
     */
    @NonNull
    protected abstract SubscriberType createSubscriberWrapper(@Nullable OnSubscribeType onSubscribe,
                                                              @Nullable Scheduler observerThread,
                                                              @NonNull Scheduler defaultThread);

    @NonNull
    private Subscription startSubscription(@Nullable OnSubscribeType onSubscribe) {
        final SubscriberType subscriber = createSubscriberWrapper(onSubscribe, observerThread, defaultThread);

        subscriber.onStart();

        executeOnSubscriberThread(new Runnable() {
            @Override
            public void run() {
                try {
                    action.onSubscribe(subscriber);
                } catch (Exception exception) {
                    subscriber.onError(exception);
                }
            }
        });

        return subscriber;
    }

    private void executeOnSubscriberThread(@NonNull Runnable runnable) {
        if (subscriberThread != null) {
            subscriberThread.execute(runnable);
        } else {
            defaultThread.execute(runnable);
        }
    }

}
