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
 * @param <ActionT>      The {@link ObservableAction} that will be provided
 *                       to the {@link OnSubscribeT} when the
 *                       consumer subscribes.
 * @param <OnSubscribeT> The {@link ObservableOnSubscribe} or type that
 *                       extends it that will be supplied when the consumer
 *                       subscribes.
 * @param <SubscriberT>  The {@link ObservableSubscriber} or type that
 *                       extends it that will be supplied to the {@link ObservableAction}
 *                       when the consumer subscribes.
 */
@SuppressWarnings("WeakerAccess")
public abstract class Observable<ActionT extends ObservableAction<SubscriberT>,
    OnSubscribeT extends ObservableOnSubscribe,
    SubscriberT extends ObservableSubscriber> {

    @NonNull private final ActionT action;
    @Nullable private Scheduler subscriberThread;
    @Nullable private Scheduler observerThread;
    @NonNull private final Scheduler defaultThread;

    protected Observable(@NonNull ActionT action) {
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
     */
    protected final void setActionScheduler(@NonNull Scheduler subscribeScheduler) {
        subscriberThread = subscribeScheduler;
    }

    /**
     * Tells the observable what {@link Scheduler} that
     * the onSubscribe should observe the work on.
     *
     * @param observerScheduler the {@link Scheduler} to run to callback on.
     */
    protected final void setObserverScheduler(@NonNull Scheduler observerScheduler) {
        observerThread = observerScheduler;
    }

    /**
     * Subscribes immediately to the {@link Observable} and
     * ignores all calls to the subscriber.
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
     * {@link OnSubscribeT}.
     *
     * @param onSubscribe the class that wishes to receive
     *                    callbacks from the observable.
     * @return a work subscription that can be cancelled.
     */
    @NonNull
    public final Subscription subscribe(@NonNull OnSubscribeT onSubscribe) {
        Preconditions.checkNonNull(onSubscribe);

        return startSubscription(onSubscribe);
    }

    /**
     * Creates a {@link SubscriberT} that
     * wraps the {@link OnSubscribeT} in order
     * to properly execute method calls on the
     * appropriate observer thread.
     *
     * @param onSubscribe    the {@link OnSubscribeT} supplied when
     *                       the consumer subscribed.
     * @param observerThread the thread that the {@link OnSubscribeT}
     *                       should be notified on, may be null.
     * @param defaultThread  the thread to notify the {@link OnSubscribeT}
     *                       on if the provided observer is null.
     * @return a valid {@link SubscriberT} that wraps the {@link OnSubscribeT}.
     */
    @NonNull
    protected abstract SubscriberT createSubscriberWrapper(@Nullable OnSubscribeT onSubscribe,
                                                           @Nullable Scheduler observerThread,
                                                           @NonNull Scheduler defaultThread);

    @NonNull
    private Subscription startSubscription(@Nullable OnSubscribeT onSubscribe) {
        final SubscriberT subscriber = createSubscriberWrapper(onSubscribe, observerThread, defaultThread);

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
