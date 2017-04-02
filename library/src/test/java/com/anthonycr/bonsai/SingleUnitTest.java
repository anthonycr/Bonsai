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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SingleUnitTest extends BaseUnitTest {

    @Mock
    private SingleOnSubscribe<String> stringSingleOnSubscribe;

    @Test
    public void testMainLooperWorking() throws Exception {
        if (Looper.getMainLooper() == null) {
            Looper.prepareMainLooper();
        }
        assertNotNull(Looper.getMainLooper());
    }

    @Test
    public void testSingleEmissionOrder_singleThread() throws Exception {
        final String testItem = "1";

        Single.create(new SingleAction<String>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {
                subscriber.onItem(testItem);
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.current())
            .subscribe(stringSingleOnSubscribe);

        InOrder inOrder = Mockito.inOrder(stringSingleOnSubscribe);

        inOrder.verify(stringSingleOnSubscribe).onStart();
        inOrder.verify(stringSingleOnSubscribe).onItem(testItem);
        inOrder.verify(stringSingleOnSubscribe).onComplete();

        Mockito.verifyNoMoreInteractions(stringSingleOnSubscribe);
    }

    @Test
    public void testSingleMultipleEventEmission_throwsException() throws Exception {
        final String testItem = "2";

        final AtomicReference<Boolean> exceptionWasCause = new AtomicReference<>(false);
        final AtomicReference<Throwable> throwableAssertion = new AtomicReference<>(null);
        final AtomicReference<String> stringItem = new AtomicReference<>(null);

        final CountDownLatch onErrorCountdown = new CountDownLatch(1);
        final CountDownLatch onCompleteCountdown = new CountDownLatch(1);

        Single.create(new SingleAction<String>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {
                try {
                    subscriber.onItem(testItem);
                    subscriber.onItem(testItem);
                } catch (Exception e) {
                    throwableAssertion.set(e);
                    subscriber.onError(e);
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
                    stringItem.set(item);
                    onCompleteCountdown.countDown();
                }

            });

        onErrorCountdown.await();
        onCompleteCountdown.await();

        assertTrue(exceptionWasCause.get());
        assertEquals(testItem, stringItem.get());
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
        final String testItem = "1";
        final RuntimeException runtimeException = new RuntimeException("Test failure");

        Single.create(new SingleAction<String>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {
                subscriber.onItem(testItem);
                throw runtimeException;
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.current())
            .subscribe(stringSingleOnSubscribe);

        InOrder inOrder = Mockito.inOrder(stringSingleOnSubscribe);

        inOrder.verify(stringSingleOnSubscribe).onStart();
        inOrder.verify(stringSingleOnSubscribe).onItem(testItem);
        inOrder.verify(stringSingleOnSubscribe).onError(runtimeException);

        Mockito.verifyNoMoreInteractions(stringSingleOnSubscribe);
    }

    @Test
    public void testSingleEventEmission_withError() throws Exception {
        final String testItem = "1";
        final Exception exception = new Exception("Test failure");

        Single.create(new SingleAction<String>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {
                subscriber.onItem(testItem);
                try {
                    throw exception;
                } catch (Exception e) {
                    subscriber.onError(e);
                }
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.current())
            .subscribe(stringSingleOnSubscribe);

        InOrder inOrder = Mockito.inOrder(stringSingleOnSubscribe);

        inOrder.verify(stringSingleOnSubscribe).onStart();
        inOrder.verify(stringSingleOnSubscribe).onItem(testItem);
        inOrder.verify(stringSingleOnSubscribe).onError(exception);

        Mockito.verifyNoMoreInteractions(stringSingleOnSubscribe);
    }

    @Test
    public void testSingleEventEmission_withoutError() throws Exception {
        final String testItem = "1";

        Single.create(new SingleAction<String>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {
                subscriber.onItem(testItem);
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.current())
            .subscribe(stringSingleOnSubscribe);

        InOrder inOrder = Mockito.inOrder(stringSingleOnSubscribe);

        inOrder.verify(stringSingleOnSubscribe).onStart();
        inOrder.verify(stringSingleOnSubscribe).onItem(testItem);
        inOrder.verify(stringSingleOnSubscribe).onComplete();

        Mockito.verifyNoMoreInteractions(stringSingleOnSubscribe);
    }

    @Test
    public void testStreamThrowsException_onStartCalled() throws Exception {
        final AtomicReference<Throwable> throwableReference = new AtomicReference<>(null);
        Single.create(new SingleAction<String>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {
                try {
                    subscriber.onStart();
                } catch (Exception e) {
                    throwableReference.set(e);
                    subscriber.onError(e);
                }
            }
        }).subscribe(stringSingleOnSubscribe);

        InOrder inOrder = Mockito.inOrder(stringSingleOnSubscribe);
        inOrder.verify(stringSingleOnSubscribe).onStart();
        inOrder.verify(stringSingleOnSubscribe).onError(throwableReference.get());

        Mockito.verifyNoMoreInteractions(stringSingleOnSubscribe);
    }

    @Test
    public void testStreamThrowsException_onStartCalled_noOnSubscribe() throws Exception {
        final AtomicReference<Boolean> errorThrown = new AtomicReference<>(false);
        Single.create(new SingleAction<Object>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<Object> subscriber) {
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
    public void testSingleUnsubscribe_unsubscribesSuccessfully() throws Exception {
        final CountDownLatch subscribeLatch = new CountDownLatch(1);
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Boolean> assertion = new AtomicReference<>(false);
        Subscription stringSubscription = Single.create(new SingleAction<String>() {

            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {
                Utils.safeWait(subscribeLatch);
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

        final AtomicReference<String> subscribeThreadAssertion = new AtomicReference<>();
        final AtomicReference<String> observerThreadAssertion = new AtomicReference<>();

        final AtomicReference<Boolean> onStartAssertion = new AtomicReference<>(false);
        final AtomicReference<Boolean> onErrorAssertion = new AtomicReference<>(false);

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

        final AtomicReference<String> subscribeThreadAssertion = new AtomicReference<>();
        final AtomicReference<String> observerThreadAssertion = new AtomicReference<>();

        final AtomicReference<Boolean> onItemAssertion = new AtomicReference<>(false);
        final AtomicReference<Boolean> onErrorAssertion = new AtomicReference<>(false);

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

        final AtomicReference<String> subscribeThreadAssertion = new AtomicReference<>();
        final AtomicReference<String> observerThreadAssertion = new AtomicReference<>();

        final AtomicReference<Boolean> onCompleteAssertion = new AtomicReference<>(false);
        final AtomicReference<Boolean> onErrorAssertion = new AtomicReference<>(false);

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

        final AtomicReference<String> subscribeThreadAssertion = new AtomicReference<>();
        final AtomicReference<String> observerThreadAssertion = new AtomicReference<>();

        final AtomicReference<Boolean> onCompleteAssertion = new AtomicReference<>(false);
        final AtomicReference<Boolean> onErrorAssertion = new AtomicReference<>(false);

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

        final AtomicReference<String> subscribeThreadAssertion = new AtomicReference<>();
        final AtomicReference<String> observerThreadAssertion = new AtomicReference<>();

        final AtomicReference<Boolean> onCompleteAssertion = new AtomicReference<>(false);
        final AtomicReference<Boolean> onErrorAssertion = new AtomicReference<>(false);

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
        final AtomicReference<Boolean> isCalledAssertion = new AtomicReference<>(false);
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
        final AtomicReference<Boolean> errorThrown = new AtomicReference<>(false);
        Single.create(new SingleAction<String>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {
                try {
                    subscriber.onComplete();
                    subscriber.onComplete();
                } catch (RuntimeException e) {
                    errorThrown.set(true);
                }
            }
        }).subscribe(stringSingleOnSubscribe);
        assertTrue("Exception should be thrown in subscribe code if onComplete called more than once",
            errorThrown.get());

        InOrder inOrder = Mockito.inOrder(stringSingleOnSubscribe);

        inOrder.verify(stringSingleOnSubscribe).onStart();
        inOrder.verify(stringSingleOnSubscribe).onComplete();

        Mockito.verifyNoMoreInteractions(stringSingleOnSubscribe);
    }

    @Test
    public void testCompletableThrowsException_onCompleteCalledTwice_noOnSubscribe() throws Exception {
        final AtomicReference<Boolean> errorThrown = new AtomicReference<>(false);
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
        }).subscribe();
        assertTrue("Exception should be thrown in subscribe code if onComplete called more than once",
            errorThrown.get());
    }

    @Test
    public void testSingleThrowsException_onItemCalledTwice() throws Exception {
        final AtomicReference<Boolean> errorThrown = new AtomicReference<>(false);
        Single.create(new SingleAction<String>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {
                try {
                    subscriber.onItem(null);
                    subscriber.onItem(null);
                } catch (RuntimeException e) {
                    errorThrown.set(true);
                }
                subscriber.onComplete();
            }
        }).subscribe(stringSingleOnSubscribe);
        assertTrue("Exception should be thrown in subscribe code if onItem called after onComplete",
            errorThrown.get());

        InOrder inOrder = Mockito.inOrder(stringSingleOnSubscribe);

        inOrder.verify(stringSingleOnSubscribe).onStart();
        inOrder.verify(stringSingleOnSubscribe).onItem(null);
        inOrder.verify(stringSingleOnSubscribe).onComplete();

        Mockito.verifyNoMoreInteractions(stringSingleOnSubscribe);
    }

    @Test
    public void testSingleThrowsException_onItemCalledAfterOnComplete() throws Exception {
        final AtomicReference<Boolean> errorThrown = new AtomicReference<>(false);
        Single.create(new SingleAction<String>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {
                try {
                    subscriber.onComplete();
                    subscriber.onItem(null);
                } catch (RuntimeException e) {
                    errorThrown.set(true);
                }
            }
        }).subscribe(stringSingleOnSubscribe);
        assertTrue("Exception should be thrown in subscribe code if onItem called after onComplete",
            errorThrown.get());

        InOrder inOrder = Mockito.inOrder(stringSingleOnSubscribe);

        inOrder.verify(stringSingleOnSubscribe).onStart();
        inOrder.verify(stringSingleOnSubscribe).onComplete();

        Mockito.verifyNoMoreInteractions(stringSingleOnSubscribe);
    }

    @Test
    public void testSingleCreatesLooperIfNotThere() throws Exception {
        final AtomicReference<Boolean> looperInitiallyNull = new AtomicReference<>(false);
        final AtomicReference<Boolean> looperFinallyNotNull = new AtomicReference<>(false);
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
        final AtomicReference<Boolean> unsubscribed = new AtomicReference<>(false);
        final List<String> list = new ArrayList<>();
        Subscription subscription = Single.create(new SingleAction<String>() {

            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {
                Utils.safeWait(latch);
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
        Single<String> stringSingle = Single.empty();
        stringSingle.subscribe(stringSingleOnSubscribe);

        InOrder inOrder = Mockito.inOrder(stringSingleOnSubscribe);

        inOrder.verify(stringSingleOnSubscribe).onStart();
        inOrder.verify(stringSingleOnSubscribe).onComplete();

        Mockito.verifyNoMoreInteractions(stringSingleOnSubscribe);
    }

}
