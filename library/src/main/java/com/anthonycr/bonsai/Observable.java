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

import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * A reactive Java implementation. This class allows work
 * to be done on a certain thread and then allows
 * items to be emitted on a different thread. It is
 * a replacement for {@link android.os.AsyncTask}.
 * <p>
 * It allows the caller of this class to create an observable
 * task that can emit multiple items and then complete.
 * The consumer of the {@link Observable} will be notified
 * of the start and completion of the action, as well as the
 * items that can be emitted, or errors that occur.
 *
 * @param <T> the type that the Observable will emit.
 */
@SuppressWarnings("WeakerAccess")
public class Observable<T> {

    @NonNull private final ObservableAction<T> action;
    @NonNull private final Scheduler defaultThread;
    @Nullable private Scheduler subscriberThread;
    @Nullable private Scheduler observerThread;

    private Observable(@NonNull ObservableAction<T> action) {
        this.action = action;
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        Looper looper = Looper.myLooper();
        Preconditions.checkNonNull(looper);
        defaultThread = new ThreadScheduler(looper);
    }

    /**
     * Static creator method that creates an Observable from the
     * {@link CompletableAction} that is passed in as the parameter. Action
     * must not be null.
     *
     * @param action the Action to perform
     * @param <T>    the type that will be emitted to the onSubscribe
     * @return a valid non-null Observable.
     */
    @NonNull
    public static <T> Observable<T> create(@NonNull ObservableAction<T> action) {
        Preconditions.checkNonNull(action);
        return new Observable<>(action);
    }

    /**
     * Static creator that creates an Observable that is empty
     * and emits no items, but completes immediately.
     *
     * @param <T> the type that will be emitted to the onSubscribe
     * @return a valid non-null empty Observable.
     */
    @NonNull
    public static <T> Observable<T> empty() {
        return new Observable<>(new ObservableAction<T>() {
            @Override
            public void onSubscribe(@NonNull ObservableSubscriber<T> subscriber) {
                subscriber.onComplete();
            }
        });
    }

    /**
     * Tells the Observable what Scheduler that the onSubscribe
     * work should run on.
     *
     * @param subscribeScheduler the Scheduler to run the work on.
     * @return returns this so that calls can be conveniently chained.
     */
    @NonNull
    public Observable<T> subscribeOn(@NonNull Scheduler subscribeScheduler) {
        subscriberThread = subscribeScheduler;
        return this;
    }

    /**
     * Tells the Observable what Scheduler the onSubscribe should observe
     * the work on.
     *
     * @param observerScheduler the Scheduler to run to callback on.
     * @return returns this so that calls can be conveniently chained.
     */
    @NonNull
    public Observable<T> observeOn(@NonNull Scheduler observerScheduler) {
        observerThread = observerScheduler;
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
                try {
                    action.onSubscribe(new SubscriberImpl<>(null, Observable.this));
                } catch (Exception exception) {
                    // Do nothing because we don't have a subscriber
                }
            }
        });
    }

    /**
     * Immediately subscribes to the Observable and starts
     * sending events from the Observable to the {@link ObservableOnSubscribe}.
     *
     * @param onSubscribe the class that wishes to receive onNext and
     *                    onComplete callbacks from the Observable.
     */
    @NonNull
    public Subscription subscribe(@NonNull ObservableOnSubscribe<T> onSubscribe) {

        Preconditions.checkNonNull(onSubscribe);

        final ObservableSubscriber<T> subscriber = new SubscriberImpl<>(onSubscribe, this);

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

    private static class SubscriberImpl<T> implements ObservableSubscriber<T> {

        @Nullable private volatile ObservableOnSubscribe<T> onSubscribe;
        @NonNull private final Observable<T> observable;
        private boolean onCompleteExecuted = false;
        private boolean onError = false;

        SubscriberImpl(@Nullable ObservableOnSubscribe<T> onSubscribe, @NonNull Observable<T> observable) {
            this.onSubscribe = onSubscribe;
            this.observable = observable;
        }

        @Override
        public void unsubscribe() {
            onSubscribe = null;
        }

        @Override
        public void onComplete() {
            ObservableOnSubscribe<T> onSubscribe = this.onSubscribe;
            if (!onCompleteExecuted && onSubscribe != null && !onError) {
                onCompleteExecuted = true;
                observable.executeOnObserverThread(new OnCompleteRunnable(onSubscribe));
            } else if (!onError && onCompleteExecuted) {
                throw new RuntimeException("onComplete called more than once");
            }
            unsubscribe();
        }

        @Override
        public void onStart() {
            ObservableOnSubscribe<T> onSubscribe = this.onSubscribe;
            if (onSubscribe != null) {
                observable.executeOnObserverThread(new OnStartRunnable(onSubscribe));
            }
        }

        @Override
        public void onError(@NonNull final Throwable throwable) {
            ObservableOnSubscribe<T> onSubscribe = this.onSubscribe;
            if (onSubscribe != null) {
                onError = true;
                observable.executeOnObserverThread(new OnErrorRunnable(onSubscribe, throwable));
            }
            unsubscribe();
        }

        @Override
        public void onNext(final T item) {
            ObservableOnSubscribe<T> onSubscribe = this.onSubscribe;
            if (!onCompleteExecuted && onSubscribe != null && !onError) {
                observable.executeOnObserverThread(new OnNextRunnable<>(onSubscribe, item));
            } else if (onCompleteExecuted) {
                throw new RuntimeException("onNext should not be called after onComplete has been called");
            } else {
                // Subscription has been unsubscribed, ignore it
            }
        }

        @Override
        public boolean isUnsubscribed() {
            return onSubscribe == null;
        }
    }

}

