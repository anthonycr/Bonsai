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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class StreamUnitTest extends BaseUnitTest {

    @Mock
    private StreamOnSubscribe<String> stringStreamOnSubscribe;

    @Test
    public void testMainLooperWorking() throws Exception {
        if (Looper.getMainLooper() == null) {
            Looper.prepareMainLooper();
        }
        assertNotNull(Looper.getMainLooper());
    }

    @Test
    public void testStreamEmissionOrder_singleThread() throws Exception {
        final List<String> testList = Arrays.asList("1", "2", "3", "4", "5", "6", "7");

        Stream.create(new StreamAction<String>() {
            @Override
            public void onSubscribe(@NonNull StreamSubscriber<String> subscriber) {
                for (String item : testList) {
                    subscriber.onNext(item);
                }
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.immediate())
            .observeOn(Schedulers.immediate())
            .subscribe(stringStreamOnSubscribe);

        InOrder inOrder = Mockito.inOrder(stringStreamOnSubscribe);

        inOrder.verify(stringStreamOnSubscribe).onStart();
        for (String item : testList) {
            inOrder.verify(stringStreamOnSubscribe).onNext(item);
        }
        inOrder.verify(stringStreamOnSubscribe).onComplete();

        Mockito.verifyNoMoreInteractions(stringStreamOnSubscribe);
    }

    @Test
    public void testStreamEventEmission_withException() throws Exception {
        final List<String> testList = Arrays.asList("1", "2", "3", "4", "5", "6", "7");
        final RuntimeException runtimeException = new RuntimeException("Test failure");

        Stream.create(new StreamAction<String>() {
            @Override
            public void onSubscribe(@NonNull StreamSubscriber<String> subscriber) {
                for (String item : testList) {
                    subscriber.onNext(item);
                }
                throw runtimeException;
            }
        }).subscribeOn(Schedulers.immediate())
            .observeOn(Schedulers.immediate())
            .subscribe(stringStreamOnSubscribe);

        InOrder inOrder = Mockito.inOrder(stringStreamOnSubscribe);

        inOrder.verify(stringStreamOnSubscribe).onStart();
        for (String item : testList) {
            inOrder.verify(stringStreamOnSubscribe).onNext(item);
        }
        inOrder.verify(stringStreamOnSubscribe).onError(runtimeException);

        Mockito.verifyNoMoreInteractions(stringStreamOnSubscribe);
    }

    @Test
    public void testStreamEventEmission_withoutSubscriber_withException() throws Exception {
        final List<String> testList = Arrays.asList("1", "2", "3", "4", "5", "6", "7");

        Stream.create(new StreamAction<String>() {
            @Override
            public void onSubscribe(@NonNull StreamSubscriber<String> subscriber) {
                for (String item : testList) {
                    subscriber.onNext(item);
                }
                throw new RuntimeException("Test failure");
            }
        }).subscribeOn(Schedulers.immediate())
            .observeOn(Schedulers.immediate())
            .subscribe();

        // No assertions since we did not supply an OnSubscribe.
    }

    @Test
    public void testStreamEventEmission_withError() throws Exception {
        final List<String> testList = Arrays.asList("1", "2", "3", "4", "5", "6", "7");
        final Exception exception = new Exception("Test failure");

        Stream.create(new StreamAction<String>() {
            @Override
            public void onSubscribe(@NonNull StreamSubscriber<String> subscriber) {
                for (String item : testList) {
                    subscriber.onNext(item);
                }
                try {
                    throw exception;
                } catch (Exception e) {
                    subscriber.onError(e);
                }
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.immediate())
            .observeOn(Schedulers.immediate())
            .subscribe(stringStreamOnSubscribe);

        InOrder inOrder = Mockito.inOrder(stringStreamOnSubscribe);

        inOrder.verify(stringStreamOnSubscribe).onStart();
        for (String item : testList) {
            inOrder.verify(stringStreamOnSubscribe).onNext(item);
        }
        inOrder.verify(stringStreamOnSubscribe).onError(exception);

        Mockito.verifyNoMoreInteractions(stringStreamOnSubscribe);
    }

    @Test
    public void testStreamUnsubscribe_unsubscribesSuccessfully() throws Exception {
        final CountDownLatch subscribeLatch = new CountDownLatch(1);
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Boolean> assertion = new AtomicReference<>(false);
        Subscription stringSubscription = Stream.create(new StreamAction<String>() {

            @Override
            public void onSubscribe(@NonNull StreamSubscriber<String> subscriber) {
                Utils.safeWait(subscribeLatch);
                subscriber.onNext("test");
                latch.countDown();
            }
        }).subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe(new StreamOnSubscribe<String>() {
                @Override
                public void onNext(@Nullable String item) {
                    assertion.set(true);
                }
            });

        stringSubscription.unsubscribe();
        subscribeLatch.countDown();
        latch.await();

        assertFalse(assertion.get());
    }

    @Test
    public void testStreamThread_onStart_isCorrect() throws Exception {
        final CountDownLatch observeLatch = new CountDownLatch(1);
        final CountDownLatch subscribeLatch = new CountDownLatch(1);

        final AtomicReference<String> subscribeThreadAssertion = new AtomicReference<>();
        final AtomicReference<String> observerThreadAssertion = new AtomicReference<>();

        final AtomicReference<Boolean> onStartAssertion = new AtomicReference<>(false);
        final AtomicReference<Boolean> onErrorAssertion = new AtomicReference<>(false);

        Stream.create(new StreamAction<String>() {

            @Override
            public void onSubscribe(@NonNull StreamSubscriber<String> subscriber) {
                subscribeThreadAssertion.set(Thread.currentThread().toString());
                subscribeLatch.countDown();
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.worker())
            .observeOn(Schedulers.io())
            .subscribe(new StreamOnSubscribe<String>() {
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
    public void testStreamThread_onNext_isCorrect() throws Exception {
        final CountDownLatch observeLatch = new CountDownLatch(1);
        final CountDownLatch subscribeLatch = new CountDownLatch(1);

        final AtomicReference<String> subscribeThreadAssertion = new AtomicReference<>();
        final AtomicReference<String> observerThreadAssertion = new AtomicReference<>();

        final AtomicReference<Boolean> onNextAssertion = new AtomicReference<>(false);
        final AtomicReference<Boolean> onErrorAssertion = new AtomicReference<>(false);

        Stream.create(new StreamAction<String>() {

            @Override
            public void onSubscribe(@NonNull StreamSubscriber<String> subscriber) {
                subscribeThreadAssertion.set(Thread.currentThread().toString());
                subscribeLatch.countDown();
                subscriber.onNext(null);
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.worker())
            .observeOn(Schedulers.io())
            .subscribe(new StreamOnSubscribe<String>() {
                @Override
                public void onError(@NonNull Throwable throwable) {
                    onErrorAssertion.set(true);
                    observerThreadAssertion.set(Thread.currentThread().toString());
                    observeLatch.countDown();
                }

                @Override
                public void onNext(@Nullable String item) {
                    onNextAssertion.set(true);
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

        assertTrue(onNextAssertion.get());
        assertFalse(onErrorAssertion.get());
    }

    @Test
    public void testStreamThread_onComplete_isCorrect() throws Exception {
        final CountDownLatch observeLatch = new CountDownLatch(1);
        final CountDownLatch subscribeLatch = new CountDownLatch(1);

        final AtomicReference<String> subscribeThreadAssertion = new AtomicReference<>();
        final AtomicReference<String> observerThreadAssertion = new AtomicReference<>();

        final AtomicReference<Boolean> onCompleteAssertion = new AtomicReference<>(false);
        final AtomicReference<Boolean> onErrorAssertion = new AtomicReference<>(false);

        Stream.create(new StreamAction<String>() {

            @Override
            public void onSubscribe(@NonNull StreamSubscriber<String> subscriber) {
                subscribeThreadAssertion.set(Thread.currentThread().toString());
                subscribeLatch.countDown();
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.worker())
            .observeOn(Schedulers.io())
            .subscribe(new StreamOnSubscribe<String>() {
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
    public void testStreamThread_onError_isCorrect() throws Exception {
        final CountDownLatch observeLatch = new CountDownLatch(1);
        final CountDownLatch subscribeLatch = new CountDownLatch(1);

        final AtomicReference<String> subscribeThreadAssertion = new AtomicReference<>();
        final AtomicReference<String> observerThreadAssertion = new AtomicReference<>();

        final AtomicReference<Boolean> onCompleteAssertion = new AtomicReference<>(false);
        final AtomicReference<Boolean> onErrorAssertion = new AtomicReference<>(false);

        Stream.create(new StreamAction<String>() {

            @Override
            public void onSubscribe(@NonNull StreamSubscriber<String> subscriber) {
                subscribeThreadAssertion.set(Thread.currentThread().toString());
                subscribeLatch.countDown();
                subscriber.onError(new RuntimeException("There was a problem"));
            }
        }).subscribeOn(Schedulers.worker())
            .observeOn(Schedulers.io())
            .subscribe(new StreamOnSubscribe<String>() {
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
    public void testStreamThread_ThrownException_isCorrect() throws Exception {
        final CountDownLatch observeLatch = new CountDownLatch(1);
        final CountDownLatch subscribeLatch = new CountDownLatch(1);

        final AtomicReference<String> subscribeThreadAssertion = new AtomicReference<>();
        final AtomicReference<String> observerThreadAssertion = new AtomicReference<>();

        final AtomicReference<Boolean> onCompleteAssertion = new AtomicReference<>(false);
        final AtomicReference<Boolean> onErrorAssertion = new AtomicReference<>(false);

        Stream.create(new StreamAction<String>() {

            @Override
            public void onSubscribe(@NonNull StreamSubscriber<String> subscriber) {
                subscribeThreadAssertion.set(Thread.currentThread().toString());
                subscribeLatch.countDown();
                throw new RuntimeException("There was a problem");
            }
        }).subscribeOn(Schedulers.worker())
            .observeOn(Schedulers.io())
            .subscribe(new StreamOnSubscribe<String>() {
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
    public void testStreamSubscribesWithoutSubscriber() throws Exception {
        final AtomicReference<Boolean> isCalledAssertion = new AtomicReference<>(false);
        Stream.create(new StreamAction<Object>() {
            @Override
            public void onSubscribe(@NonNull StreamSubscriber<Object> subscriber) {
                subscriber.onNext(null);
                subscriber.onComplete();
                isCalledAssertion.set(true);
            }
        }).subscribeOn(Schedulers.immediate())
            .observeOn(Schedulers.immediate())
            .subscribe();
        assertTrue("onSubscribe must be called when subscribe is called", isCalledAssertion.get());
    }

    @Test
    public void testStreamThrowsException_onCompleteCalledTwice() throws Exception {
        final AtomicReference<Throwable> thrownException = new AtomicReference<>(null);
        Stream.create(new StreamAction<String>() {
            @Override
            public void onSubscribe(@NonNull StreamSubscriber<String> subscriber) {
                try {
                    subscriber.onComplete();
                    subscriber.onComplete();
                } catch (RuntimeException e) {
                    thrownException.set(e);
                    throw e;
                }
            }
        }).subscribe(stringStreamOnSubscribe);

        Assert.assertNotNull(thrownException.get());

        InOrder inOrder = Mockito.inOrder(stringStreamOnSubscribe);

        inOrder.verify(stringStreamOnSubscribe).onStart();
        inOrder.verify(stringStreamOnSubscribe).onComplete();

        Mockito.verifyNoMoreInteractions(stringStreamOnSubscribe);
    }

    @Test
    public void testStreamThrowsException_onCompleteCalledTwice_noOnSubscribe() throws Exception {
        final AtomicReference<Boolean> errorThrown = new AtomicReference<>(false);
        Stream.create(new StreamAction<Object>() {
            @Override
            public void onSubscribe(@NonNull StreamSubscriber<Object> subscriber) {
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
    public void testStreamThrowsException_onStartCalled() throws Exception {
        final AtomicReference<Throwable> thrownException = new AtomicReference<>(null);
        Stream.create(new StreamAction<String>() {
            @Override
            public void onSubscribe(@NonNull StreamSubscriber<String> subscriber) {
                try {
                    subscriber.onStart();
                } catch (Exception exception) {
                    thrownException.set(exception);
                    throw exception;
                }
            }
        }).subscribe(stringStreamOnSubscribe);

        InOrder inOrder = Mockito.inOrder(stringStreamOnSubscribe);

        inOrder.verify(stringStreamOnSubscribe).onStart();
        inOrder.verify(stringStreamOnSubscribe).onError(thrownException.get());

        Mockito.verifyNoMoreInteractions(stringStreamOnSubscribe);
    }

    @Test
    public void testStreamThrowsException_onStartCalled_noOnSubscribe() throws Exception {
        final AtomicReference<Boolean> errorThrown = new AtomicReference<>(false);
        Stream.create(new StreamAction<Object>() {
            @Override
            public void onSubscribe(@NonNull StreamSubscriber<Object> subscriber) {
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
    public void testStreamThrowsException_onNextCalledAfterOnComplete() throws Exception {
        final AtomicReference<Boolean> errorThrown = new AtomicReference<>(false);
        Stream.create(new StreamAction<String>() {
            @Override
            public void onSubscribe(@NonNull StreamSubscriber<String> subscriber) {
                try {
                    subscriber.onComplete();
                    subscriber.onNext(null);
                } catch (RuntimeException e) {
                    errorThrown.set(true);
                }
            }
        }).subscribe(stringStreamOnSubscribe);

        assertTrue("Exception should be thrown in subscribe code if onNext called after onComplete", errorThrown.get());

        InOrder inOrder = Mockito.inOrder(stringStreamOnSubscribe);

        inOrder.verify(stringStreamOnSubscribe).onStart();
        inOrder.verify(stringStreamOnSubscribe).onComplete();

        Mockito.verifyNoMoreInteractions(stringStreamOnSubscribe);
    }

    @Test
    public void testStream_DoesNotCreateLooper_IfNotThere() throws Exception {
        final AtomicReference<Boolean> looperInitiallyNull = new AtomicReference<>(false);
        final AtomicReference<Boolean> looperFinallyNull = new AtomicReference<>(false);
        final CountDownLatch latch = new CountDownLatch(1);
        Schedulers.newSingleThreadedScheduler().execute(new Runnable() {
            @Override
            public void run() {
                // No looper associated with this thread yet
                looperInitiallyNull.set(Looper.myLooper() == null);
                Stream.create(new StreamAction<Object>() {
                    @Override
                    public void onSubscribe(@NonNull StreamSubscriber<Object> subscriber) {
                        looperFinallyNull.set(Looper.myLooper() == null);
                    }
                }).subscribe();
                latch.countDown();
            }
        });
        latch.await();
        assertTrue("Looper should initially be null", looperInitiallyNull.get());
        assertTrue("Looper should not be initialized by stream class", looperFinallyNull.get());
    }

    @Test
    public void testStreamSubscriberIsUnsubscribed() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch onNextLatch = new CountDownLatch(1);
        final CountDownLatch onFinalLatch = new CountDownLatch(1);
        final AtomicReference<Boolean> unsubscribed = new AtomicReference<>(false);
        final List<String> list = new ArrayList<>();
        Subscription subscription = Stream.create(new StreamAction<String>() {

            @Override
            public void onSubscribe(@NonNull StreamSubscriber<String> subscriber) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext("test 1");
                }
                Utils.safeWait(latch);
                // should be unsubscribed after the latch countdown occurs
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext("test 2");
                }
                unsubscribed.set(subscriber.isUnsubscribed());
                onFinalLatch.countDown();
            }
        }).subscribeOn(Schedulers.newSingleThreadedScheduler())
            .observeOn(Schedulers.newSingleThreadedScheduler())
            .subscribe(new StreamOnSubscribe<String>() {
                @Override
                public void onNext(@Nullable String item) {
                    list.add(item);
                    onNextLatch.countDown();
                }
            });

        onNextLatch.await();
        subscription.unsubscribe();
        latch.countDown();
        onFinalLatch.await();

        assertTrue("Only one item should have been emitted", list.size() == 1);
        assertTrue("Wrong item emitted", list.get(0).equals("test 1"));
        assertTrue("isUnsubscribed() was not correct", unsubscribed.get());
    }

    @Test
    public void testStreamEmpty_emitsNothingImmediately() throws Exception {
        Stream<String> stream = Stream.empty();
        stream.subscribe(stringStreamOnSubscribe);

        InOrder inOrder = Mockito.inOrder(stringStreamOnSubscribe);

        inOrder.verify(stringStreamOnSubscribe).onStart();
        inOrder.verify(stringStreamOnSubscribe).onComplete();

        Mockito.verifyNoMoreInteractions(stringStreamOnSubscribe);
    }

}