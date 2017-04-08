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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CompletableUnitTest extends BaseUnitTest {

    @Mock
    private CompletableOnSubscribe completableOnSubscribe;

    @Test
    public void testMainLooperWorking() throws Exception {
        if (Looper.getMainLooper() == null) {
            Looper.prepareMainLooper();
        }
        assertNotNull(Looper.getMainLooper());
    }

    @Test
    public void testCompletableEventEmission_withException() throws Exception {
        final RuntimeException runtimeException = new RuntimeException("Test failure");

        Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                throw runtimeException;
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.current())
            .subscribe(completableOnSubscribe);

        InOrder inOrder = Mockito.inOrder(completableOnSubscribe);

        inOrder.verify(completableOnSubscribe).onStart();
        inOrder.verify(completableOnSubscribe).onError(runtimeException);

        Mockito.verify(completableOnSubscribe, Mockito.never()).onComplete();
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
        final Exception exception = new Exception("Test failure");

        Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                try {
                    throw exception;
                } catch (Exception e) {
                    subscriber.onError(e);
                }
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.current())
            .subscribe(completableOnSubscribe);

        InOrder inOrder = Mockito.inOrder(completableOnSubscribe);

        inOrder.verify(completableOnSubscribe).onStart();
        inOrder.verify(completableOnSubscribe).onError(exception);

        Mockito.verify(completableOnSubscribe, Mockito.never()).onComplete();
    }

    @Test
    public void testCompletableEventEmission_withoutError() throws Exception {
        final AtomicReference<Boolean> onSubscribeAssertion = new AtomicReference<>(false);

        Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                onSubscribeAssertion.set(true);
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.current())
            .subscribe(completableOnSubscribe);

        // Assert that each of the events was
        // received by the subscriber
        assertTrue(onSubscribeAssertion.get());

        InOrder inOrder = Mockito.inOrder(completableOnSubscribe);

        inOrder.verify(completableOnSubscribe).onStart();
        inOrder.verify(completableOnSubscribe).onComplete();

        Mockito.verifyNoMoreInteractions(completableOnSubscribe);
    }

    @Test
    public void testCompletableUnsubscribe_unsubscribesSuccessfully() throws Exception {
        final CountDownLatch subscribeLatch = new CountDownLatch(1);
        final CountDownLatch latch = new CountDownLatch(1);
        Subscription stringSubscription = Completable.create(new CompletableAction() {

            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                Utils.safeWait(subscribeLatch);
                subscriber.onComplete();
                latch.countDown();
            }
        }).subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe(completableOnSubscribe);

        stringSubscription.unsubscribe();
        subscribeLatch.countDown();
        latch.await();

        Mockito.verify(completableOnSubscribe).onStart();
        Mockito.verifyNoMoreInteractions(completableOnSubscribe);
    }

    @Test
    public void testCompletableThread_onStart_isCorrect() throws Exception {
        final CountDownLatch observeLatch = new CountDownLatch(1);
        final CountDownLatch subscribeLatch = new CountDownLatch(1);

        final AtomicReference<String> subscribeThreadAssertion = new AtomicReference<>();
        final AtomicReference<String> observerThreadAssertion = new AtomicReference<>();

        final AtomicReference<Boolean> onStartAssertion = new AtomicReference<>(false);
        final AtomicReference<Boolean> onErrorAssertion = new AtomicReference<>(false);

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

        final AtomicReference<String> subscribeThreadAssertion = new AtomicReference<>();
        final AtomicReference<String> observerThreadAssertion = new AtomicReference<>();

        final AtomicReference<Boolean> onCompleteAssertion = new AtomicReference<>(false);
        final AtomicReference<Boolean> onErrorAssertion = new AtomicReference<>(false);

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

        final AtomicReference<String> subscribeThreadAssertion = new AtomicReference<>();
        final AtomicReference<String> observerThreadAssertion = new AtomicReference<>();

        final AtomicReference<Boolean> onCompleteAssertion = new AtomicReference<>(false);
        final AtomicReference<Boolean> onErrorAssertion = new AtomicReference<>(false);

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

        final AtomicReference<String> subscribeThreadAssertion = new AtomicReference<>();
        final AtomicReference<String> observerThreadAssertion = new AtomicReference<>();

        final AtomicReference<Boolean> onCompleteAssertion = new AtomicReference<>(false);
        final AtomicReference<Boolean> onErrorAssertion = new AtomicReference<>(false);

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
        final AtomicReference<Boolean> errorThrown = new AtomicReference<>(false);
        Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                try {
                    subscriber.onStart();
                } catch (Exception exception) {
                    errorThrown.set(true);
                }
                subscriber.onComplete();
            }
        }).subscribe(completableOnSubscribe);

        assertTrue("Exception should be thrown in subscribe code if onStart is called", errorThrown.get());

        InOrder inOrder = Mockito.inOrder(completableOnSubscribe);

        inOrder.verify(completableOnSubscribe).onStart();
        inOrder.verify(completableOnSubscribe).onComplete();

        Mockito.verifyNoMoreInteractions(completableOnSubscribe);
    }

    @Test
    public void testCompletableThrowsException_onStartCalled_noOnSubscribe() throws Exception {
        final AtomicReference<Boolean> errorThrown = new AtomicReference<>(false);
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
        final AtomicReference<Boolean> isCalledAssertion = new AtomicReference<>(false);
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
        final AtomicReference<Boolean> errorThrown = new AtomicReference<>(false);
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
        }).subscribe(completableOnSubscribe);

        assertTrue("Exception should be thrown in subscribe code if onComplete called more than once",
            errorThrown.get());

        InOrder inOrder = Mockito.inOrder(completableOnSubscribe);

        inOrder.verify(completableOnSubscribe).onStart();
        inOrder.verify(completableOnSubscribe).onComplete();

        Mockito.verifyNoMoreInteractions(completableOnSubscribe);
    }

    @Test
    public void testCompletableThrowsException_onCompleteCalledTwice_noOnSubscribe() throws Exception {
        final AtomicReference<Boolean> errorThrown = new AtomicReference<>(false);
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
        final AtomicReference<Boolean> looperInitiallyNull = new AtomicReference<>(false);
        final AtomicReference<Boolean> looperFinallyNotNull = new AtomicReference<>(false);
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
        final CountDownLatch onStartLatch = new CountDownLatch(1);

        final AtomicReference<Boolean> onStart = new AtomicReference<>(false);
        final AtomicReference<Boolean> onComplete = new AtomicReference<>(false);
        final AtomicReference<Boolean> onError = new AtomicReference<>(false);

        final AtomicReference<Boolean> unsubscribed = new AtomicReference<>(false);
        final AtomicReference<Boolean> workAssertion = new AtomicReference<>(false);

        Subscription subscription = Completable.create(new CompletableAction() {

            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                Utils.safeWait(latch);
                // should be unsubscribed after the latch countdown occurs
                if (!subscriber.isUnsubscribed()) {
                    workAssertion.set(true);
                }
                unsubscribed.set(subscriber.isUnsubscribed());
                subscriber.onComplete();
                onFinalLatch.countDown();
            }
        }).subscribeOn(Schedulers.newSingleThreadedScheduler())
            .observeOn(Schedulers.newSingleThreadedScheduler())
            .subscribe(new CompletableOnSubscribe() {
                @Override
                public void onComplete() {
                    onComplete.set(true);
                }

                @Override
                public void onError(@NonNull Throwable throwable) {
                    onError.set(true);
                }

                @Override
                public void onStart() {
                    onStart.set(true);
                    onStartLatch.countDown();
                }
            });

        subscription.unsubscribe();
        latch.countDown();
        onFinalLatch.await();
        onStartLatch.await();

        assertFalse(workAssertion.get());
        assertTrue("isUnsubscribed() was not correct", unsubscribed.get());
        assertTrue(onStart.get());
        assertFalse(onComplete.get());
        assertFalse(onError.get());

    }

    @Test
    public void testDefaultSubscriber_createdOnSubscribeThread() throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final CountDownLatch threadInitializationLatch = new CountDownLatch(2);
        final AtomicReference<String> singleThreadRef1 = new AtomicReference<>(null);
        final AtomicReference<String> singleThreadRef2 = new AtomicReference<>(null);

        final Scheduler singleThread1 = Schedulers.newSingleThreadedScheduler();
        Scheduler singleThread2 = Schedulers.newSingleThreadedScheduler();
        singleThread1.execute(new Runnable() {
            @Override
            public void run() {
                singleThreadRef1.set(Thread.currentThread().toString());
                threadInitializationLatch.countDown();
            }
        });

        singleThread2.execute(new Runnable() {
            @Override
            public void run() {
                singleThreadRef2.set(Thread.currentThread().toString());
                threadInitializationLatch.countDown();
            }
        });
        // Wait until we know the thread names
        threadInitializationLatch.await();

        // Ensure that the inner completable is executed on the subscribe
        // thread, not the thread that the completable was created on.
        final Completable innerCompletable = Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                Assert.assertEquals(singleThreadRef1.get(), Thread.currentThread().toString());
                subscriber.onComplete();
            }
        });

        // Ensure that the outer completable observes the inner completable
        // on the same thread on which it subscribed, not the thread it was
        // created on.
        Completable outerCompletable = Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull final CompletableSubscriber subscriber) {
                final String currentThread = Thread.currentThread().toString();
                innerCompletable.subscribe(new CompletableOnSubscribe() {
                    @Override
                    public void onComplete() {
                        Assert.assertEquals(Thread.currentThread().toString(), currentThread);
                        subscriber.onComplete();
                    }
                });
            }
        });

        outerCompletable
            .subscribeOn(singleThread1)
            .observeOn(singleThread2)
            .subscribe(new CompletableOnSubscribe() {
                @Override
                public void onComplete() {
                    Assert.assertEquals(singleThreadRef2.get(), Thread.currentThread().toString());
                    countDownLatch.countDown();
                }
            });

        countDownLatch.await();
    }

    @Test
    public void testCompletableEmpty_emitsNothingImmediately() throws Exception {
        Completable.empty().subscribe(completableOnSubscribe);

        InOrder inOrder = Mockito.inOrder(completableOnSubscribe);

        inOrder.verify(completableOnSubscribe).onStart();
        inOrder.verify(completableOnSubscribe).onComplete();

        Mockito.verifyNoMoreInteractions(completableOnSubscribe);
    }

}
