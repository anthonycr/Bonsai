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

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SingleUnitTest extends BaseUnitTest {

    @Test
    public void testMainLooperWorking() throws Exception {
        if (Looper.getMainLooper() == null) {
            Looper.prepareMainLooper();
        }
        assertNotNull(Looper.getMainLooper());
    }

    @Test
    public void testSingleEmissionOrder_singleThread() throws Exception {
        final int testCount = 1;

        final List<String> list = new ArrayList<>(testCount);
        Single.create(new SingleAction<String>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {
                for (int n = 0; n < testCount; n++) {
                    subscriber.onItem(String.valueOf(n));
                }
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.current())
            .subscribe(new SingleOnSubscribe<String>() {
                @Override
                public void onItem(@Nullable String item) {
                    list.add(item);
                }

            });
        assertTrue(list.size() == testCount);
        for (int n = 0; n < list.size(); n++) {
            assertTrue(String.valueOf(n).equals(list.get(n)));
        }
    }

    @Test
    public void testSingleMultipleEventEmission_throwsException() throws Exception {
        final int testCount = 2;

        final Assertion<Boolean> exceptionWasThrown = new Assertion<>(false);
        final Assertion<Boolean> exceptionWasCause = new Assertion<>(false);
        final Assertion<Throwable> throwableAssertion = new Assertion<>(null);

        final CountDownLatch onErrorCountdown = new CountDownLatch(1);
        final CountDownLatch onCompleteCountdown = new CountDownLatch(1);

        final List<String> list = new ArrayList<>(testCount);
        Single.create(new SingleAction<String>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {
                try {
                    for (int n = 0; n < testCount; n++) {
                        subscriber.onItem(String.valueOf(n));
                    }
                } catch (Exception ignored) {
                    throwableAssertion.set(ignored);
                    exceptionWasThrown.set(true);
                    subscriber.onError(ignored);
                }
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.io())
            .subscribe(new SingleOnSubscribe<String>() {
                @Override
                public void onError(@NonNull Throwable throwable) {
                    exceptionWasCause.set(throwable.equals(throwableAssertion.get()));
                    onErrorCountdown.countDown();
                }

                @Override
                public void onItem(@Nullable String item) {
                    list.add(item);
                    onCompleteCountdown.countDown();
                }

            });


        onErrorCountdown.await();
        onCompleteCountdown.await();

        assertTrue(exceptionWasCause.get());
        assertTrue(exceptionWasThrown.get());

        assertTrue(list.size() == 1);
        for (int n = 0; n < list.size(); n++) {
            assertTrue(String.valueOf(n).equals(list.get(n)));
        }
    }

    @Test
    public void testSingleEventEmission_withoutSubscriber_withException() throws Exception {
        Single.create(new SingleAction<String>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {
                subscriber.onItem(String.valueOf(1));
                throw new RuntimeException("Test failure");
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.io())
            .subscribe();

        // No assertions since we did not supply an OnSubscribe.
    }

    @Test
    public void testSingleEventEmission_withException() throws Exception {
        final int testCount = 1;

        final Assertion<Boolean> errorAssertion = new Assertion<>(false);
        final Assertion<Boolean> nextAssertion = new Assertion<>(false);
        final Assertion<Boolean> completeAssertion = new Assertion<>(false);
        final Assertion<Boolean> startAssertion = new Assertion<>(false);

        final List<String> list = new ArrayList<>(testCount);
        Single.create(new SingleAction<String>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {
                for (int n = 0; n < testCount; n++) {
                    subscriber.onItem(String.valueOf(n));
                }
                throw new RuntimeException("Test failure");
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.current())
            .subscribe(new SingleOnSubscribe<String>() {

                @Override
                public void onStart() {
                    startAssertion.set(true);
                }

                @Override
                public void onItem(@Nullable String item) {
                    nextAssertion.set(true);
                    list.add(item);
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

        // Even though error has been broadcast,
        // Single should still complete.
        assertTrue(list.size() == testCount);
        for (int n = 0; n < list.size(); n++) {
            assertTrue(String.valueOf(n).equals(list.get(n)));
        }

        // Assert that each of the events was
        // received by the subscriber
        assertTrue(errorAssertion.get());
        assertTrue(startAssertion.get());
        assertFalse(completeAssertion.get());
        assertTrue(nextAssertion.get());
    }

    @Test
    public void testSingleEventEmission_withError() throws Exception {
        final int testCount = 1;

        final Assertion<Boolean> errorAssertion = new Assertion<>(false);
        final Assertion<Boolean> nextAssertion = new Assertion<>(false);
        final Assertion<Boolean> completeAssertion = new Assertion<>(false);
        final Assertion<Boolean> startAssertion = new Assertion<>(false);

        final List<String> list = new ArrayList<>(testCount);
        Single.create(new SingleAction<String>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {
                for (int n = 0; n < testCount; n++) {
                    subscriber.onItem(String.valueOf(n));
                }
                try {
                    throw new Exception("Test failure");
                } catch (Exception e) {
                    subscriber.onError(e);
                }
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.current())
            .subscribe(new SingleOnSubscribe<String>() {

                @Override
                public void onStart() {
                    startAssertion.set(true);
                }

                @Override
                public void onItem(@Nullable String item) {
                    nextAssertion.set(true);
                    list.add(item);
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

        // Even though error has been broadcast,
        // Single should still complete.
        assertTrue(list.size() == testCount);
        for (int n = 0; n < list.size(); n++) {
            assertTrue(String.valueOf(n).equals(list.get(n)));
        }

        // Assert that each of the events was
        // received by the subscriber
        assertTrue(errorAssertion.get());
        assertTrue(startAssertion.get());
        assertFalse(completeAssertion.get());
        assertTrue(nextAssertion.get());
    }

    @Test
    public void testSingleEventEmission_withoutError() throws Exception {
        final int testCount = 1;

        final Assertion<Boolean> errorAssertion = new Assertion<>(false);
        final Assertion<Boolean> nextAssertion = new Assertion<>(false);
        final Assertion<Boolean> completeAssertion = new Assertion<>(false);
        final Assertion<Boolean> startAssertion = new Assertion<>(false);

        final List<String> list = new ArrayList<>(testCount);
        Single.create(new SingleAction<String>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {
                for (int n = 0; n < testCount; n++) {
                    subscriber.onItem(String.valueOf(n));
                }
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.current())
            .subscribe(new SingleOnSubscribe<String>() {

                @Override
                public void onStart() {
                    startAssertion.set(true);
                }

                @Override
                public void onItem(@Nullable String item) {
                    nextAssertion.set(true);
                    list.add(item);
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

        // Even though error has been broadcast,
        // Single should still complete.
        assertTrue(list.size() == testCount);
        for (int n = 0; n < list.size(); n++) {
            assertTrue(String.valueOf(n).equals(list.get(n)));
        }

        // Assert that each of the events was
        // received by the subscriber
        assertFalse(errorAssertion.get());
        assertTrue(startAssertion.get());
        assertTrue(completeAssertion.get());
        assertTrue(nextAssertion.get());
    }

    @Test
    public void testStreamThrowsException_onStartCalled() throws Exception {
        final Assertion<Boolean> errorThrown = new Assertion<>(false);
        Single.create(new SingleAction<Object>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<Object> subscriber) {
                try {
                    subscriber.onStart();
                } catch (Exception exception) {
                    errorThrown.set(true);
                }
            }
        }).subscribe(new SingleOnSubscribe<Object>() {});
        assertTrue("Exception should be thrown in subscribe code if onStart is called", errorThrown.get());
    }

    @Test
    public void testSingleUnsubscribe_unsubscribesSuccessfully() throws Exception {
        final CountDownLatch subscribeLatch = new CountDownLatch(1);
        final CountDownLatch latch = new CountDownLatch(1);
        final Assertion<Boolean> assertion = new Assertion<>(false);
        Subscription stringSubscription = Single.create(new SingleAction<String>() {

            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {
                try {
                    subscribeLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                subscriber.onItem("test");
                latch.countDown();
            }
        }).subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe(new SingleOnSubscribe<String>() {
                @Override
                public void onItem(@Nullable String item) {
                    assertion.set(true);
                }
            });

        stringSubscription.unsubscribe();
        subscribeLatch.countDown();
        latch.await();

        assertFalse(assertion.get());
    }

    @Test
    public void testSingleThread_onStart_isCorrect() throws Exception {
        final CountDownLatch observeLatch = new CountDownLatch(1);
        final CountDownLatch subscribeLatch = new CountDownLatch(1);

        final Assertion<String> subscribeThreadAssertion = new Assertion<>();
        final Assertion<String> observerThreadAssertion = new Assertion<>();

        final Assertion<Boolean> onStartAssertion = new Assertion<>(false);
        final Assertion<Boolean> onErrorAssertion = new Assertion<>(false);

        Single.create(new SingleAction<String>() {

            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {
                subscribeThreadAssertion.set(Thread.currentThread().toString());
                subscribeLatch.countDown();
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.worker())
            .observeOn(Schedulers.io())
            .subscribe(new SingleOnSubscribe<String>() {
                @Override
                public void onItem(@Nullable String item) {

                }

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
    public void testSingleThread_onItem_isCorrect() throws Exception {
        final CountDownLatch observeLatch = new CountDownLatch(1);
        final CountDownLatch subscribeLatch = new CountDownLatch(1);

        final Assertion<String> subscribeThreadAssertion = new Assertion<>();
        final Assertion<String> observerThreadAssertion = new Assertion<>();

        final Assertion<Boolean> onItemAssertion = new Assertion<>(false);
        final Assertion<Boolean> onErrorAssertion = new Assertion<>(false);

        Single.create(new SingleAction<String>() {

            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {
                subscribeThreadAssertion.set(Thread.currentThread().toString());
                subscribeLatch.countDown();
                subscriber.onItem(null);
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.worker())
            .observeOn(Schedulers.io())
            .subscribe(new SingleOnSubscribe<String>() {
                @Override
                public void onError(@NonNull Throwable throwable) {
                    onErrorAssertion.set(true);
                    observerThreadAssertion.set(Thread.currentThread().toString());
                    observeLatch.countDown();
                }

                @Override
                public void onItem(@Nullable String item) {
                    onItemAssertion.set(true);
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

        assertTrue(onItemAssertion.get());
        assertFalse(onErrorAssertion.get());
    }

    @Test
    public void testSingleThread_onComplete_isCorrect() throws Exception {
        final CountDownLatch observeLatch = new CountDownLatch(1);
        final CountDownLatch subscribeLatch = new CountDownLatch(1);

        final Assertion<String> subscribeThreadAssertion = new Assertion<>();
        final Assertion<String> observerThreadAssertion = new Assertion<>();

        final Assertion<Boolean> onCompleteAssertion = new Assertion<>(false);
        final Assertion<Boolean> onErrorAssertion = new Assertion<>(false);

        Single.create(new SingleAction<String>() {

            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {
                subscribeThreadAssertion.set(Thread.currentThread().toString());
                subscribeLatch.countDown();
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.worker())
            .observeOn(Schedulers.io())
            .subscribe(new SingleOnSubscribe<String>() {
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
    public void testSingleThread_onError_isCorrect() throws Exception {
        final CountDownLatch observeLatch = new CountDownLatch(1);
        final CountDownLatch subscribeLatch = new CountDownLatch(1);

        final Assertion<String> subscribeThreadAssertion = new Assertion<>();
        final Assertion<String> observerThreadAssertion = new Assertion<>();

        final Assertion<Boolean> onCompleteAssertion = new Assertion<>(false);
        final Assertion<Boolean> onErrorAssertion = new Assertion<>(false);

        Single.create(new SingleAction<String>() {

            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {
                subscribeThreadAssertion.set(Thread.currentThread().toString());
                subscribeLatch.countDown();
                subscriber.onError(new RuntimeException("There was a problem"));
            }
        }).subscribeOn(Schedulers.worker())
            .observeOn(Schedulers.io())
            .subscribe(new SingleOnSubscribe<String>() {
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
    public void testSingleThread_ThrownException_isCorrect() throws Exception {
        final CountDownLatch observeLatch = new CountDownLatch(1);
        final CountDownLatch subscribeLatch = new CountDownLatch(1);

        final Assertion<String> subscribeThreadAssertion = new Assertion<>();
        final Assertion<String> observerThreadAssertion = new Assertion<>();

        final Assertion<Boolean> onCompleteAssertion = new Assertion<>(false);
        final Assertion<Boolean> onErrorAssertion = new Assertion<>(false);

        Single.create(new SingleAction<String>() {

            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {
                subscribeThreadAssertion.set(Thread.currentThread().toString());
                subscribeLatch.countDown();
                throw new RuntimeException("There was a problem");
            }
        }).subscribeOn(Schedulers.worker())
            .observeOn(Schedulers.io())
            .subscribe(new SingleOnSubscribe<String>() {
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
    public void testSingleSubscribesWithoutSubscriber() throws Exception {
        final Assertion<Boolean> isCalledAssertion = new Assertion<>(false);
        Single.create(new SingleAction<Object>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<Object> subscriber) {
                subscriber.onItem(null);
                subscriber.onComplete();
                isCalledAssertion.set(true);
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.current())
            .subscribe();
        assertTrue("onSubscribe must be called when subscribe is called", isCalledAssertion.get());
    }

    @Test
    public void testSingleThrowsException_onCompleteCalledTwice() throws Exception {
        final Assertion<Boolean> errorThrown = new Assertion<>(false);
        Single.create(new SingleAction<Object>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<Object> subscriber) {
                try {
                    subscriber.onComplete();
                    subscriber.onComplete();
                } catch (RuntimeException e) {
                    errorThrown.set(true);
                }
            }
        }).subscribe(new SingleOnSubscribe<Object>() {
        });
        assertTrue("Exception should be thrown in subscribe code if onComplete called more than once",
            errorThrown.get());
    }

    @Test
    public void testSingleThrowsException_onItemCalledAfterOnComplete() throws Exception {
        final Assertion<Boolean> errorThrown = new Assertion<>(false);
        Single.create(new SingleAction<Object>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<Object> subscriber) {
                try {
                    subscriber.onComplete();
                    subscriber.onItem(null);
                } catch (RuntimeException e) {
                    errorThrown.set(true);
                }
            }
        }).subscribe(new SingleOnSubscribe<Object>() {
        });
        assertTrue("Exception should be thrown in subscribe code if onItem called after onComplete",
            errorThrown.get());
    }

    @Test
    public void testSingleCreatesLooperIfNotThere() throws Exception {
        final Assertion<Boolean> looperInitiallyNull = new Assertion<>(false);
        final Assertion<Boolean> looperFinallyNotNull = new Assertion<>(false);
        final CountDownLatch latch = new CountDownLatch(1);
        Schedulers.newSingleThreadedScheduler().execute(new Runnable() {
            @Override
            public void run() {
                // No looper associated with this thread yet
                looperInitiallyNull.set(Looper.myLooper() == null);
                Single.create(new SingleAction<Object>() {
                    @Override
                    public void onSubscribe(@NonNull SingleSubscriber<Object> subscriber) {
                        looperFinallyNotNull.set(Looper.myLooper() != null);
                    }
                }).subscribe();
                latch.countDown();
            }
        });
        latch.await();
        assertTrue("Looper should initially be null", looperInitiallyNull.get());
        assertTrue("Looper should be initialized by Single class", looperFinallyNotNull.get());
    }

    @Test
    public void testSingleSubscriberIsUnsubscribed() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch onFinalLatch = new CountDownLatch(1);
        final Assertion<Boolean> unsubscribed = new Assertion<>(false);
        final List<String> list = new ArrayList<>();
        Subscription subscription = Single.create(new SingleAction<String>() {

            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // should be unsubscribed after the latch countdown occurs
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onItem("test 1");
                }
                unsubscribed.set(subscriber.isUnsubscribed());
                onFinalLatch.countDown();
            }
        }).subscribeOn(Schedulers.newSingleThreadedScheduler())
            .observeOn(Schedulers.newSingleThreadedScheduler())
            .subscribe(new SingleOnSubscribe<String>() {
                @Override
                public void onItem(@Nullable String item) {
                    list.add(item);
                }
            });

        subscription.unsubscribe();
        latch.countDown();
        onFinalLatch.await();

        assertTrue("No items should have been emitted", list.size() == 0);
        assertTrue("isUnsubscribed() was not correct", unsubscribed.get());
    }

    @Test
    public void testSingleEmpty_emitsNothingImmediately() throws Exception {
        final Assertion<Boolean> onItemAssertion = new Assertion<>(false);
        final Assertion<Boolean> onCompleteAssertion = new Assertion<>(false);
        Single.empty().subscribe(new SingleOnSubscribe<Object>() {

            @Override
            public void onItem(@Nullable Object item) {
                onItemAssertion.set(true);
            }

            @Override
            public void onComplete() {
                onCompleteAssertion.set(true);
            }

        });

        assertFalse(onItemAssertion.get());
        assertTrue(onCompleteAssertion.get());
    }

}
