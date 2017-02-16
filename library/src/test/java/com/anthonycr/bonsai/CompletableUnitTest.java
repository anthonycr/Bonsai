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

import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CompletableUnitTest extends BaseUnitTest {

    @Test
    public void testMainLooperWorking() throws Exception {
        if (Looper.getMainLooper() == null) {
            Looper.prepareMainLooper();
        }
        assertNotNull(Looper.getMainLooper());
    }

    @Test
    public void testCompletableEventEmission_withException() throws Exception {
        final Assertion<Boolean> errorAssertion = new Assertion<>(false);
        final Assertion<Boolean> completeAssertion = new Assertion<>(false);
        final Assertion<Boolean> startAssertion = new Assertion<>(false);

        Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                throw new RuntimeException("Test failure");
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.current())
            .subscribe(new CompletableOnSubscribe() {

                @Override
                public void onStart() {
                    startAssertion.set(true);
                }

                @Override
                public void onComplete() {
                    completeAssertion.set(true);
                }

                @Override
                public void onError(@NonNull Throwable throwable) {
                    errorAssertion.set(true);
                }
            });

        // Assert that each of the events was
        // received by the subscriber
        assertTrue(errorAssertion.get());
        assertTrue(startAssertion.get());
        assertFalse(completeAssertion.get());
    }

    @Test
    public void testCompletableEventEmission_withoutSubscriber_withException() throws Exception {
        Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                throw new RuntimeException("Test failure");
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.current())
            .subscribe();

        // No assertions since we did not supply an OnSubscribe.
    }

    @Test
    public void testCompletableEventEmission_withError() throws Exception {
        final Assertion<Boolean> errorAssertion = new Assertion<>(false);
        final Assertion<Boolean> completeAssertion = new Assertion<>(false);
        final Assertion<Boolean> startAssertion = new Assertion<>(false);

        Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                try {
                    throw new Exception("Test failure");
                } catch (Exception e) {
                    subscriber.onError(e);
                }
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.current())
            .subscribe(new CompletableOnSubscribe() {

                @Override
                public void onStart() {
                    startAssertion.set(true);
                }

                @Override
                public void onComplete() {
                    completeAssertion.set(true);
                }

                @Override
                public void onError(@NonNull Throwable throwable) {
                    errorAssertion.set(true);
                }
            });

        // Assert that each of the events was
        // received by the subscriber
        assertTrue(errorAssertion.get());
        assertTrue(startAssertion.get());
        assertFalse(completeAssertion.get());
    }

    @Test
    public void testCompletableEventEmission_withoutError() throws Exception {
        final Assertion<Boolean> onSubscribeAssertion = new Assertion<>(false);
        final Assertion<Boolean> errorAssertion = new Assertion<>(false);
        final Assertion<Boolean> completeAssertion = new Assertion<>(false);
        final Assertion<Boolean> startAssertion = new Assertion<>(false);

        Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                onSubscribeAssertion.set(true);
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.current())
            .subscribe(new CompletableOnSubscribe() {

                @Override
                public void onStart() {
                    startAssertion.set(true);
                }

                @Override
                public void onComplete() {
                    completeAssertion.set(true);
                }

                @Override
                public void onError(@NonNull Throwable throwable) {
                    errorAssertion.set(true);
                }
            });

        // Assert that each of the events was
        // received by the subscriber
        assertTrue(onSubscribeAssertion.get());
        assertFalse(errorAssertion.get());
        assertTrue(startAssertion.get());
        assertTrue(completeAssertion.get());
    }

    @Test
    public void testCompletableUnsubscribe_unsubscribesSuccessfully() throws Exception {
        final CountDownLatch subscribeLatch = new CountDownLatch(1);
        final CountDownLatch latch = new CountDownLatch(1);
        final Assertion<Boolean> assertion = new Assertion<>(false);
        Subscription stringSubscription = Completable.create(new CompletableAction() {

            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                try {
                    subscribeLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                subscriber.onComplete();
                latch.countDown();
            }
        }).subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe(new CompletableOnSubscribe() {
                @Override
                public void onComplete() {
                    assertion.set(true);
                }
            });

        stringSubscription.unsubscribe();
        subscribeLatch.countDown();
        latch.await();

        assertFalse(assertion.get());
    }

    @Test
    public void testCompletableThread_onStart_isCorrect() throws Exception {
        final CountDownLatch observeLatch = new CountDownLatch(1);
        final CountDownLatch subscribeLatch = new CountDownLatch(1);

        final Assertion<String> subscribeThreadAssertion = new Assertion<>();
        final Assertion<String> observerThreadAssertion = new Assertion<>();

        final Assertion<Boolean> onStartAssertion = new Assertion<>(false);
        final Assertion<Boolean> onErrorAssertion = new Assertion<>(false);

        Completable.create(new CompletableAction() {

            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                subscribeThreadAssertion.set(Thread.currentThread().toString());
                subscribeLatch.countDown();
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.worker())
            .observeOn(Schedulers.io())
            .subscribe(new CompletableOnSubscribe() {
                @Override
                public void onError(@NonNull Throwable throwable) {
                    onErrorAssertion.set(true);
                    observerThreadAssertion.set(Thread.currentThread().toString());
                    observeLatch.countDown();
                }

                @Override
                public void onStart() {
                    onStartAssertion.set(true);
                    observerThreadAssertion.set(Thread.currentThread().toString());
                    observeLatch.countDown();
                }
            });

        subscribeLatch.await();
        observeLatch.await();

        String currentThread = Thread.currentThread().toString();

        assertNotNull(subscribeThreadAssertion.get());
        assertNotNull(observerThreadAssertion.get());

        assertNotEquals(subscribeThreadAssertion.get(), currentThread);
        assertNotEquals(observerThreadAssertion.get(), currentThread);
        assertNotEquals(subscribeThreadAssertion.get(), observerThreadAssertion.get());

        assertTrue(onStartAssertion.get());
        assertFalse(onErrorAssertion.get());
    }

    @Test
    public void testCompletableThread_onComplete_isCorrect() throws Exception {
        final CountDownLatch observeLatch = new CountDownLatch(1);
        final CountDownLatch subscribeLatch = new CountDownLatch(1);

        final Assertion<String> subscribeThreadAssertion = new Assertion<>();
        final Assertion<String> observerThreadAssertion = new Assertion<>();

        final Assertion<Boolean> onCompleteAssertion = new Assertion<>(false);
        final Assertion<Boolean> onErrorAssertion = new Assertion<>(false);

        Completable.create(new CompletableAction() {

            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                subscribeThreadAssertion.set(Thread.currentThread().toString());
                subscribeLatch.countDown();
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.worker())
            .observeOn(Schedulers.io())
            .subscribe(new CompletableOnSubscribe() {
                @Override
                public void onError(@NonNull Throwable throwable) {
                    onErrorAssertion.set(true);
                    observerThreadAssertion.set(Thread.currentThread().toString());
                    observeLatch.countDown();
                }

                @Override
                public void onComplete() {
                    onCompleteAssertion.set(true);
                    observerThreadAssertion.set(Thread.currentThread().toString());
                    observeLatch.countDown();
                }
            });

        subscribeLatch.await();
        observeLatch.await();

        String currentThread = Thread.currentThread().toString();

        assertNotNull(subscribeThreadAssertion.get());
        assertNotNull(observerThreadAssertion.get());

        assertNotEquals(subscribeThreadAssertion.get(), currentThread);
        assertNotEquals(observerThreadAssertion.get(), currentThread);
        assertNotEquals(subscribeThreadAssertion.get(), observerThreadAssertion.get());

        assertTrue(onCompleteAssertion.get());
        assertFalse(onErrorAssertion.get());
    }

    @Test
    public void testCompletableThread_onError_isCorrect() throws Exception {
        final CountDownLatch observeLatch = new CountDownLatch(1);
        final CountDownLatch subscribeLatch = new CountDownLatch(1);

        final Assertion<String> subscribeThreadAssertion = new Assertion<>();
        final Assertion<String> observerThreadAssertion = new Assertion<>();

        final Assertion<Boolean> onCompleteAssertion = new Assertion<>(false);
        final Assertion<Boolean> onErrorAssertion = new Assertion<>(false);

        Completable.create(new CompletableAction() {

            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                subscribeThreadAssertion.set(Thread.currentThread().toString());
                subscribeLatch.countDown();
                subscriber.onError(new RuntimeException("There was a problem"));
            }
        }).subscribeOn(Schedulers.worker())
            .observeOn(Schedulers.io())
            .subscribe(new CompletableOnSubscribe() {
                @Override
                public void onError(@NonNull Throwable throwable) {
                    onErrorAssertion.set(true);
                    observerThreadAssertion.set(Thread.currentThread().toString());
                    observeLatch.countDown();
                }

                @Override
                public void onComplete() {
                    onCompleteAssertion.set(true);
                    observerThreadAssertion.set(Thread.currentThread().toString());
                    observeLatch.countDown();
                }
            });

        subscribeLatch.await();
        observeLatch.await();

        String currentThread = Thread.currentThread().toString();

        assertNotNull(subscribeThreadAssertion.get());
        assertNotNull(observerThreadAssertion.get());

        assertNotEquals(subscribeThreadAssertion.get(), currentThread);
        assertNotEquals(observerThreadAssertion.get(), currentThread);
        assertNotEquals(subscribeThreadAssertion.get(), observerThreadAssertion.get());

        assertFalse(onCompleteAssertion.get());
        assertTrue(onErrorAssertion.get());
    }

    @Test
    public void testCompletableThread_ThrownException_isCorrect() throws Exception {
        final CountDownLatch observeLatch = new CountDownLatch(1);
        final CountDownLatch subscribeLatch = new CountDownLatch(1);

        final Assertion<String> subscribeThreadAssertion = new Assertion<>();
        final Assertion<String> observerThreadAssertion = new Assertion<>();

        final Assertion<Boolean> onCompleteAssertion = new Assertion<>(false);
        final Assertion<Boolean> onErrorAssertion = new Assertion<>(false);

        Completable.create(new CompletableAction() {

            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                subscribeThreadAssertion.set(Thread.currentThread().toString());
                subscribeLatch.countDown();
                throw new RuntimeException("There was a problem");
            }
        }).subscribeOn(Schedulers.worker())
            .observeOn(Schedulers.io())
            .subscribe(new CompletableOnSubscribe() {
                @Override
                public void onError(@NonNull Throwable throwable) {
                    onErrorAssertion.set(true);
                    observerThreadAssertion.set(Thread.currentThread().toString());
                    observeLatch.countDown();
                }

                @Override
                public void onComplete() {
                    onCompleteAssertion.set(true);
                    observerThreadAssertion.set(Thread.currentThread().toString());
                    observeLatch.countDown();
                }
            });

        subscribeLatch.await();
        observeLatch.await();

        String currentThread = Thread.currentThread().toString();

        assertNotNull(subscribeThreadAssertion.get());
        assertNotNull(observerThreadAssertion.get());

        assertNotEquals(subscribeThreadAssertion.get(), currentThread);
        assertNotEquals(observerThreadAssertion.get(), currentThread);
        assertNotEquals(subscribeThreadAssertion.get(), observerThreadAssertion.get());

        assertFalse(onCompleteAssertion.get());
        assertTrue(onErrorAssertion.get());
    }

    @Test
    public void testCompletableThrowsException_onStartCalled() throws Exception {
        final Assertion<Boolean> errorThrown = new Assertion<>(false);
        Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                try {
                    subscriber.onStart();
                } catch (Exception exception) {
                    errorThrown.set(true);
                }
            }
        }).subscribe(new CompletableOnSubscribe() {});
        assertTrue("Exception should be thrown in subscribe code if onStart is called", errorThrown.get());
    }

    @Test
    public void testCompletableThrowsException_onStartCalled_noOnSubscribe() throws Exception {
        final Assertion<Boolean> errorThrown = new Assertion<>(false);
        Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                try {
                    subscriber.onStart();
                } catch (Exception exception) {
                    errorThrown.set(true);
                }
            }
        }).subscribe();
        assertTrue("Exception should be thrown in subscribe code if onStart is called", errorThrown.get());
    }

    @Test
    public void testCompletableSubscribesWithoutSubscriber() throws Exception {
        final Assertion<Boolean> isCalledAssertion = new Assertion<>(false);
        Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                subscriber.onComplete();
                isCalledAssertion.set(true);
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.current())
            .subscribe();
        assertTrue("onSubscribe must be called when subscribe is called", isCalledAssertion.get());
    }

    @Test
    public void testCompletableThrowsException_onCompleteCalledTwice() throws Exception {
        final Assertion<Boolean> errorThrown = new Assertion<>(false);
        Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                try {
                    subscriber.onComplete();
                    subscriber.onComplete();
                } catch (RuntimeException e) {
                    errorThrown.set(true);
                }
            }
        }).subscribe(new CompletableOnSubscribe() {
            @Override
            public void onComplete() {

            }
        });
        assertTrue("Exception should be thrown in subscribe code if onComplete called more than once",
            errorThrown.get());
    }

    @Test
    public void testCompletableThrowsException_onCompleteCalledTwice_noOnSubscribe() throws Exception {
        final Assertion<Boolean> errorThrown = new Assertion<>(false);
        Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                try {
                    subscriber.onComplete();
                    subscriber.onComplete();
                } catch (RuntimeException e) {
                    errorThrown.set(true);
                }
            }
        }).subscribe();
        assertTrue("Exception should be thrown in subscribe code if onComplete called more than once",
            errorThrown.get());
    }

    @Test
    public void testCompletableCreatesLooperIfNotThere() throws Exception {
        final Assertion<Boolean> looperInitiallyNull = new Assertion<>(false);
        final Assertion<Boolean> looperFinallyNotNull = new Assertion<>(false);
        final CountDownLatch latch = new CountDownLatch(1);
        Schedulers.newSingleThreadedScheduler().execute(new Runnable() {
            @Override
            public void run() {
                // No looper associated with this thread yet
                looperInitiallyNull.set(Looper.myLooper() == null);
                Completable.create(new CompletableAction() {
                    @Override
                    public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                        looperFinallyNotNull.set(Looper.myLooper() != null);
                    }
                }).subscribe();
                latch.countDown();
            }
        });
        latch.await();
        assertTrue("Looper should initially be null", looperInitiallyNull.get());
        assertTrue("Looper should be initialized by Completable class", looperFinallyNotNull.get());
    }

    @Test
    public void testCompletableSubscriberIsUnsubscribed() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch onFinalLatch = new CountDownLatch(1);
        final Assertion<Boolean> unsubscribed = new Assertion<>(false);
        final Assertion<Boolean> workAssertion = new Assertion<>(false);
        Subscription subscription = Completable.create(new CompletableAction() {

            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // should be unsubscribed after the latch countdown occurs
                if (!subscriber.isUnsubscribed()) {
                    workAssertion.set(true);
                }
                unsubscribed.set(subscriber.isUnsubscribed());
                onFinalLatch.countDown();
            }
        }).subscribeOn(Schedulers.newSingleThreadedScheduler())
            .observeOn(Schedulers.newSingleThreadedScheduler())
            .subscribe(new CompletableOnSubscribe() {
                @Override
                public void onComplete() {

                }
            });

        subscription.unsubscribe();
        latch.countDown();
        onFinalLatch.await();

        assertFalse(workAssertion.get());
        assertTrue("isUnsubscribed() was not correct", unsubscribed.get());
    }

    @Test
    public void testCompletableEmpty_emitsNothingImmediately() throws Exception {
        final Assertion<Boolean> onCompleteAssertion = new Assertion<>(false);
        Completable.empty().subscribe(new CompletableOnSubscribe() {
            @Override
            public void onComplete() {
                onCompleteAssertion.set(true);
            }

        });

        assertTrue(onCompleteAssertion.get());
    }

}
