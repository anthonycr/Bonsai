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
public class Completable {

    @NonNull private final CompletableAction action;
    @NonNull private final Scheduler defaultThread;
    @Nullable private Scheduler subscriberThread;
    @Nullable private Scheduler observerThread;

    private Completable(@NonNull CompletableAction action) {
        this.action = action;
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        Looper looper = Looper.myLooper();
        Preconditions.checkNonNull(looper);
        this.defaultThread = new ThreadScheduler(looper);
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
     * Tells the Completable what Scheduler that the onSubscribe
     * work should run on.
     *
     * @param subscribeScheduler the Scheduler to run the work on.
     * @return returns this so that calls can be conveniently chained.
     */
    @NonNull
    public Completable subscribeOn(@NonNull Scheduler subscribeScheduler) {
        subscriberThread = subscribeScheduler;
        return this;
    }

    /**
     * Tells the Completable what Scheduler the onSubscribe
     * should observe the work on.
     *
     * @param observerScheduler the Scheduler to run to callback on.
     * @return returns this so that calls can be conveniently chained.
     */
    @NonNull
    public Completable observeOn(@NonNull Scheduler observerScheduler) {
        observerThread = observerScheduler;
        return this;
    }

    /**
     * Subscribes immediately to the Completable and
     * ignores all onComplete calls.
     */
    public void subscribe() {
        startSubscription(null);
    }

    /**
     * Immediately subscribes to the Completable and starts
     * sending events from the Completable to the
     * {@link StreamOnSubscribe}.
     *
     * @param onSubscribe the class that wishes to receive onComplete
     *                    callbacks from the Completable.
     */
    @NonNull
    public Subscription subscribe(@NonNull CompletableOnSubscribe onSubscribe) {
        Preconditions.checkNonNull(onSubscribe);

        return startSubscription(onSubscribe);
    }

    @NonNull
    private Subscription startSubscription(@Nullable CompletableOnSubscribe onSubscribe) {
        final CompletableSubscriber subscriber = new Completable.SubscriberImpl(onSubscribe, this);

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

    private void executeOnObserverThread(@NonNull Runnable runnable) {
        if (observerThread != null) {
            observerThread.execute(runnable);
        } else {
            defaultThread.execute(runnable);
        }
    }

    private void executeOnSubscriberThread(@NonNull Runnable runnable) {
        if (subscriberThread != null) {
            subscriberThread.execute(runnable);
        } else {
            defaultThread.execute(runnable);
        }
    }

    private static class SubscriberImpl implements CompletableSubscriber {

        @Nullable private volatile CompletableOnSubscribe onSubscribe;
        @NonNull private final Completable completable;
        private volatile boolean onStartExecuted = false;
        private volatile boolean onCompleteExecuted = false;
        private volatile boolean onErrorExecuted = false;

        SubscriberImpl(@Nullable CompletableOnSubscribe onSubscribe, @NonNull Completable completable) {
            this.onSubscribe = onSubscribe;
            this.completable = completable;
        }

        @Override
        public void unsubscribe() {
            onSubscribe = null;
        }

        @Override
        public void onComplete() {
            CompletableOnSubscribe onSubscribe = this.onSubscribe;

            if (onCompleteExecuted) {
                throw new RuntimeException("onComplete called more than once");
            } else if (onSubscribe != null && !onErrorExecuted) {
                completable.executeOnObserverThread(new OnCompleteRunnable(onSubscribe));
            }

            onCompleteExecuted = true;

            unsubscribe();
        }

        @Override
        public void onStart() {
            CompletableOnSubscribe onSubscribe = this.onSubscribe;

            if (onStartExecuted) {
                throw new RuntimeException("onStart is called internally, do not call it yourself");
            } else if (onSubscribe != null) {
                completable.executeOnObserverThread(new OnStartRunnable(onSubscribe));
            }

            onStartExecuted = true;
        }

        @Override
        public void onError(@NonNull final Throwable throwable) {
            CompletableOnSubscribe onSubscribe = this.onSubscribe;
            if (onSubscribe != null) {
                completable.executeOnObserverThread(new OnErrorRunnable(onSubscribe, throwable));
            }

            onErrorExecuted = true;

            unsubscribe();
        }

        @Override
        public boolean isUnsubscribed() {
            return onSubscribe == null;
        }
    }
}
