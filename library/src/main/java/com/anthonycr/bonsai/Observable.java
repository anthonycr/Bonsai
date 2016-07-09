/**
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

import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * A reactive Java implementation. This class allows work
 * to be done on a certain thread and then allows
 * items to be emitted on a different thread. It is
 * a replacement for {@link android.os.AsyncTask}.
 *
 * @param <T> the type that the Observable will emit.
 */
@SuppressWarnings("unused")
public class Observable<T> {

    private static final String TAG = Observable.class.getSimpleName();

    @NonNull private final Action<T> mAction;
    @Nullable private Scheduler mSubscriberThread;
    @Nullable private Scheduler mObserverThread;
    @NonNull private final Scheduler mDefault;

    Observable(@NonNull Action<T> action) {
        mAction = action;
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        Looper looper = Looper.myLooper();
        Preconditions.checkNonNull(looper);
        mDefault = new ThreadScheduler(looper);
    }

    /**
     * Static creator method that creates an Observable from the
     * {@link Action} that is passed in as the parameter. Action
     * must not be null.
     *
     * @param action the Action to perform
     * @param <T>    the type that will be emitted to the onSubscribe
     * @return a valid non-null Observable.
     */
    @NonNull
    public static <T> Observable<T> create(@NonNull Action<T> action) {
        Preconditions.checkNonNull(action);
        return new Observable<>(action);
    }

    /**
     * Tells the Observable what Scheduler that the onSubscribe
     * work should run on.
     *
     * @param subscribeScheduler the Scheduler to run the work on.
     * @return returns this so that calls can be conveniently chained.
     */
    public Observable<T> subscribeOn(@NonNull Scheduler subscribeScheduler) {
        mSubscriberThread = subscribeScheduler;
        return this;
    }

    /**
     * Tells the Observable what Scheduler the onSubscribe should observe
     * the work on.
     *
     * @param observerScheduler the Scheduler to run to callback on.
     * @return returns this so that calls can be conveniently chained.
     */
    public Observable<T> observeOn(@NonNull Scheduler observerScheduler) {
        mObserverThread = observerScheduler;
        return this;
    }

    /**
     * Subscribes immediately to the Observable and ignores
     * all onComplete and onNext calls.
     */
    public void subscribe() {
        executeOnSubscriberThread(new Runnable() {
            @Override
            public void run() {
                mAction.onSubscribe(new SubscriberImpl<>(null, Observable.this));
            }
        });
    }

    /**
     * Immediately subscribes to the Observable and starts
     * sending events from the Observable to the {@link OnSubscribe}.
     *
     * @param onSubscribe the class that wishes to receive onNext and
     *                    onComplete callbacks from the Observable.
     */
    public Subscription subscribe(@NonNull OnSubscribe<T> onSubscribe) {

        Preconditions.checkNonNull(onSubscribe);

        final Subscriber<T> subscriber = new SubscriberImpl<>(onSubscribe, this);

        subscriber.onStart();

        executeOnSubscriberThread(new Runnable() {
            @Override
            public void run() {
                mAction.onSubscribe(subscriber);
            }
        });

        return subscriber;
    }

    private void executeOnObserverThread(@NonNull Runnable runnable) {
        if (mObserverThread != null) {
            mObserverThread.execute(runnable);
        } else {
            mDefault.execute(runnable);
        }
    }

    private void executeOnSubscriberThread(@NonNull Runnable runnable) {
        if (mSubscriberThread != null) {
            mSubscriberThread.execute(runnable);
        } else {
            mDefault.execute(runnable);
        }
    }

    private static class SubscriberImpl<T> implements Subscriber<T> {

        @Nullable private volatile OnSubscribe<T> mOnSubscribe;
        @NonNull private final Observable<T> mObservable;
        private boolean mOnCompleteExecuted = false;
        private boolean mOnError = false;

        public SubscriberImpl(@Nullable OnSubscribe<T> onSubscribe, @NonNull Observable<T> observable) {
            mOnSubscribe = onSubscribe;
            mObservable = observable;
        }

        @Override
        public void unsubscribe() {
            mOnSubscribe = null;
        }

        @Override
        public void onComplete() {
            OnSubscribe<T> onSubscribe = mOnSubscribe;
            if (!mOnCompleteExecuted && onSubscribe != null && !mOnError) {
                mOnCompleteExecuted = true;
                mObservable.executeOnObserverThread(new OnCompleteRunnable<>(onSubscribe));
            } else if (!mOnError && mOnCompleteExecuted) {
                Log.e(TAG, "onComplete called more than once");
                throw new RuntimeException("onComplete called more than once");
            }
            unsubscribe();
        }

        @Override
        public void onStart() {
            OnSubscribe<T> onSubscribe = mOnSubscribe;
            if (onSubscribe != null) {
                mObservable.executeOnObserverThread(new OnStartRunnable<>(onSubscribe));
            }
        }

        @Override
        public void onError(@NonNull final Throwable throwable) {
            OnSubscribe<T> onSubscribe = mOnSubscribe;
            if (onSubscribe != null) {
                mOnError = true;
                mObservable.executeOnObserverThread(new OnErrorRunnable<>(onSubscribe, throwable));
            }
        }

        @Override
        public void onNext(final T item) {
            OnSubscribe<T> onSubscribe = mOnSubscribe;
            if (!mOnCompleteExecuted && onSubscribe != null && !mOnError) {
                mObservable.executeOnObserverThread(new OnNextRunnable<>(onSubscribe, item));
            } else if (mOnCompleteExecuted) {
                Log.e(TAG, "onComplete has been already called, onNext should not be called");
                throw new RuntimeException("onNext should not be called after onComplete has been called");
            } else {
                // Subscription has been unsubscribed, ignore it
            }
        }

        @Override
        public boolean isUnsubscribed() {
            return mOnSubscribe == null;
        }
    }

    private static class OnCompleteRunnable<T> implements Runnable {
        private final OnSubscribe<T> onSubscribe;

        public OnCompleteRunnable(@NonNull OnSubscribe<T> onSubscribe) {this.onSubscribe = onSubscribe;}

        @Override
        public void run() {
            onSubscribe.onComplete();
        }
    }

    private static class OnNextRunnable<T> implements Runnable {
        private final OnSubscribe<T> onSubscribe;
        private final T item;

        public OnNextRunnable(@NonNull OnSubscribe<T> onSubscribe, T item) {
            this.onSubscribe = onSubscribe;
            this.item = item;
        }

        @Override
        public void run() {
            onSubscribe.onNext(item);
        }
    }

    private static class OnErrorRunnable<T> implements Runnable {
        private final OnSubscribe<T> onSubscribe;
        private final Throwable throwable;

        public OnErrorRunnable(@NonNull OnSubscribe<T> onSubscribe, @NonNull Throwable throwable) {
            this.onSubscribe = onSubscribe;
            this.throwable = throwable;
        }

        @Override
        public void run() {
            onSubscribe.onError(throwable);
        }
    }

    private static class OnStartRunnable<T> implements Runnable {
        private final OnSubscribe<T> onSubscribe;

        public OnStartRunnable(@NonNull OnSubscribe<T> onSubscribe) {this.onSubscribe = onSubscribe;}

        @Override
        public void run() {
            onSubscribe.onStart();
        }
    }
}

