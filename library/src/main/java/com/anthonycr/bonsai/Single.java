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
 * It allows the caller of this class to create a single
 * task that optionally emits a single item and then completes.
 * The consumer of the {@link Single} will be notified
 * of the start and completion of the action, as well as the
 * item that could be emitted, or errors that occur.
 *
 * @param <T> the type that the Single will emit.
 */
@SuppressWarnings("WeakerAccess")
public class Single<T> {

    @NonNull private final SingleAction<T> mAction;
    @NonNull private final Scheduler mDefaultThread;
    @Nullable private Scheduler mSubscriberThread;
    @Nullable private Scheduler mObserverThread;

    private Single(@NonNull SingleAction<T> action) {
        mAction = action;
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        Looper looper = Looper.myLooper();
        Preconditions.checkNonNull(looper);
        mDefaultThread = new ThreadScheduler(looper);
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

    /**
     * Tells the Single what Scheduler that the onSubscribe
     * work should run on.
     *
     * @param subscribeScheduler the Scheduler to run the work on.
     * @return returns this so that calls can be conveniently chained.
     */
    @NonNull
    public Single<T> subscribeOn(@NonNull Scheduler subscribeScheduler) {
        mSubscriberThread = subscribeScheduler;
        return this;
    }

    /**
     * Tells the Single what Scheduler the onSubscribe should observe
     * the work on.
     *
     * @param observerScheduler the Scheduler to run to callback on.
     * @return returns this so that calls can be conveniently chained.
     */
    @NonNull
    public Single<T> observeOn(@NonNull Scheduler observerScheduler) {
        mObserverThread = observerScheduler;
        return this;
    }

    /**
     * Subscribes immediately to the Single and ignores
     * all onComplete and onItem calls.
     */
    public void subscribe() {
        executeOnSubscriberThread(new Runnable() {
            @Override
            public void run() {
                try {
                    mAction.onSubscribe(new Single.SubscriberImpl<>(null, Single.this));
                } catch (Exception exception) {
                    // Do nothing because we don't have a subscriber
                }
            }
        });
    }

    /**
     * Immediately subscribes to the Single and starts
     * sending events from the Single to the {@link ObservableOnSubscribe}.
     *
     * @param onSubscribe the class that wishes to receive onItem and
     *                    onComplete callbacks from the Single.
     */
    @NonNull
    public Subscription subscribe(@NonNull SingleOnSubscribe<T> onSubscribe) {

        Preconditions.checkNonNull(onSubscribe);

        final SingleSubscriber<T> subscriber = new Single.SubscriberImpl<>(onSubscribe, this);

        subscriber.onStart();

        executeOnSubscriberThread(new Runnable() {
            @Override
            public void run() {
                try {
                    mAction.onSubscribe(subscriber);
                } catch (Exception exception) {
                    subscriber.onError(exception);
                }
            }
        });

        return subscriber;
    }

    private void executeOnObserverThread(@NonNull Runnable runnable) {
        if (mObserverThread != null) {
            mObserverThread.execute(runnable);
        } else {
            mDefaultThread.execute(runnable);
        }
    }

    private void executeOnSubscriberThread(@NonNull Runnable runnable) {
        if (mSubscriberThread != null) {
            mSubscriberThread.execute(runnable);
        } else {
            mDefaultThread.execute(runnable);
        }
    }

    private static class SubscriberImpl<T> implements SingleSubscriber<T> {

        @Nullable private volatile SingleOnSubscribe<T> mOnSubscribe;
        @NonNull private final Single<T> mSingle;
        private boolean mOnCompleteExecuted = false;
        private boolean mOnOnlyExecuted = false;
        private boolean mOnError = false;

        SubscriberImpl(@Nullable SingleOnSubscribe<T> onSubscribe, @NonNull Single<T> Single) {
            mOnSubscribe = onSubscribe;
            mSingle = Single;
        }

        @Override
        public void unsubscribe() {
            mOnSubscribe = null;
        }

        @Override
        public void onComplete() {
            SingleOnSubscribe<T> onSubscribe = mOnSubscribe;
            if (!mOnCompleteExecuted && onSubscribe != null && !mOnError) {
                mOnCompleteExecuted = true;
                mSingle.executeOnObserverThread(new OnCompleteRunnable(onSubscribe));
            } else if (!mOnError && mOnCompleteExecuted) {
                throw new RuntimeException("onComplete called more than once");
            }
            unsubscribe();
        }

        @Override
        public void onStart() {
            SingleOnSubscribe<T> onSubscribe = mOnSubscribe;
            if (onSubscribe != null) {
                mSingle.executeOnObserverThread(new OnStartRunnable(onSubscribe));
            }
        }

        @Override
        public void onError(@NonNull final Throwable throwable) {
            SingleOnSubscribe<T> onSubscribe = mOnSubscribe;
            if (onSubscribe != null) {
                mOnError = true;
                mSingle.executeOnObserverThread(new OnErrorRunnable(onSubscribe, throwable));
            }
            unsubscribe();
        }

        @Override
        public void onItem(@Nullable T item) {
            SingleOnSubscribe<T> onSubscribe = mOnSubscribe;
            if (!mOnCompleteExecuted && !mOnOnlyExecuted && onSubscribe != null && !mOnError) {
                mOnOnlyExecuted = true;
                mSingle.executeOnObserverThread(new OnItemRunnable<>(onSubscribe, item));
            } else if (mOnCompleteExecuted) {
                throw new RuntimeException("onItem should not be called after onComplete has been called");
            } else if (mOnOnlyExecuted) {
                throw new RuntimeException("onItem should not be called multiple times");
            } else {
                // Subscription has been unsubscribed, ignore it
            }
        }

        @Override
        public boolean isUnsubscribed() {
            return mOnSubscribe == null;
        }
    }
}
