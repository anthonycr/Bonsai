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

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class ObservableUnitTest {

    @Test
    public void testMainLooperWorking() throws Exception {
        if (Looper.getMainLooper() == null) {
            Looper.prepareMainLooper();
        }
        assertNotNull(Looper.getMainLooper());
    }

    @Test
    public void testObservableEmissionOrder_singleThread() throws Exception {
        final int testCount = 7;

        final List<String> list = new ArrayList<>(testCount);
        Observable.create(new Action<String>() {
            @Override
            public void onSubscribe(@NonNull Subscriber<String> subscriber) {
                for (int n = 0; n < 7; n++) {
                    subscriber.onNext(String.valueOf(n));
                }
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.current())
            .subscribe(new OnSubscribe<String>() {
                @Override
                public void onNext(@Nullable String item) {
                    list.add(item);
                }

                @Override
                public void onComplete() {
                }
            });

        assertTrue(list.size() == testCount);
        for (int n = 0; n < list.size(); n++) {
            assertTrue(String.valueOf(n).equals(list.get(n)));
        }
    }

    @Test
    public void testObservableEventEmission_withError() throws Exception {
        final int testCount = 7;

        final Assertion<Boolean> errorAssertion = new Assertion<>(false);
        final Assertion<Boolean> nextAssertion = new Assertion<>(false);
        final Assertion<Boolean> completeAssertion = new Assertion<>(false);
        final Assertion<Boolean> startAssertion = new Assertion<>(false);

        final List<String> list = new ArrayList<>(testCount);
        Observable.create(new Action<String>() {
            @Override
            public void onSubscribe(@NonNull Subscriber<String> subscriber) {
                for (int n = 0; n < 7; n++) {
                    subscriber.onNext(String.valueOf(n));
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
            .subscribe(new OnSubscribe<String>() {

                @Override
                public void onStart() {
                    startAssertion.set(true);
                }

                @Override
                public void onNext(@Nullable String item) {
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

        // Even though error has been broadcase,
        // observable should still complete.
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
    public void testObservableEventEmission_withoutError() throws Exception {
        final int testCount = 7;

        final Assertion<Boolean> errorAssertion = new Assertion<>(false);
        final Assertion<Boolean> nextAssertion = new Assertion<>(false);
        final Assertion<Boolean> completeAssertion = new Assertion<>(false);
        final Assertion<Boolean> startAssertion = new Assertion<>(false);

        final List<String> list = new ArrayList<>(testCount);
        Observable.create(new Action<String>() {
            @Override
            public void onSubscribe(@NonNull Subscriber<String> subscriber) {
                for (int n = 0; n < 7; n++) {
                    subscriber.onNext(String.valueOf(n));
                }
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.current())
            .subscribe(new OnSubscribe<String>() {

                @Override
                public void onStart() {
                    startAssertion.set(true);
                }

                @Override
                public void onNext(@Nullable String item) {
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

        // Even though error has been broadcase,
        // observable should still complete.
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
    public void testObservableUnsubscribe_unsubscribesSuccessfully() throws Exception {
        final CountDownLatch subscribeLatch = new CountDownLatch(1);
        final CountDownLatch latch = new CountDownLatch(1);
        final Assertion<Boolean> assertion = new Assertion<>(false);
        Subscription stringSubscription = Observable.create(new Action<String>() {

            @Override
            public void onSubscribe(@NonNull Subscriber<String> subscriber) {
                try {
                    subscribeLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                subscriber.onNext("test");
                latch.countDown();
            }
        }).subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe(new OnSubscribe<String>() {
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
    public void testObservableThread_isCorrect() throws Exception {
        final CountDownLatch observeLatch = new CountDownLatch(1);
        final CountDownLatch subscribeLatch = new CountDownLatch(1);
        final Assertion<String> subscribeThreadAssertion = new Assertion<>();
        final Assertion<String> observerThreadAssertion = new Assertion<>();
        Observable.create(new Action<String>() {

            @Override
            public void onSubscribe(@NonNull Subscriber<String> subscriber) {
                subscribeThreadAssertion.set(Thread.currentThread().toString());
                subscribeLatch.countDown();
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.worker())
            .observeOn(Schedulers.io())
            .subscribe(new OnSubscribe<String>() {
                @Override
                public void onComplete() {
                    observerThreadAssertion.set(Thread.currentThread().toString());
                    observeLatch.countDown();
                }
            });

        subscribeLatch.await();
        observeLatch.await();

        String currentThread = Thread.currentThread().toString();
        assertNotNull(subscribeThreadAssertion.get());
        assertNotNull(observerThreadAssertion.get());
        assertFalse(subscribeThreadAssertion.get().equals(currentThread));
        assertFalse(observerThreadAssertion.get().equals(currentThread));
        assertFalse(subscribeThreadAssertion.get().equals(observerThreadAssertion.get()));
    }

    @Test
    public void testObservableSubscribesWithoutSubscriber() throws Exception {
        final Assertion<Boolean> isCalledAssertion = new Assertion<>(false);
        Observable.create(new Action<Object>() {
            @Override
            public void onSubscribe(@NonNull Subscriber<Object> subscriber) {
                subscriber.onNext(null);
                subscriber.onComplete();
                isCalledAssertion.set(true);
            }
        }).subscribeOn(Schedulers.current())
            .observeOn(Schedulers.current())
            .subscribe();
        Assert.assertTrue("onSubscribe must be called when subscribe is called", isCalledAssertion.get());
    }

    @Test
    public void testObservableThrowsException_onCompleteCalledTwice() throws Exception {
        final Assertion<Boolean> errorThrown = new Assertion<>(false);
        Observable.create(new Action<Object>() {
            @Override
            public void onSubscribe(@NonNull Subscriber<Object> subscriber) {
                try {
                    subscriber.onComplete();
                    subscriber.onComplete();
                } catch (RuntimeException e) {
                    errorThrown.set(true);
                }
            }
        }).subscribe(new OnSubscribe<Object>() {
            @Override
            public void onComplete() {
            }
        });
        Assert.assertTrue("Exception should be thrown in subscribe code if onComplete called more than once",
            errorThrown.get());
    }

    @Test
    public void testObservableThrowsException_onNextCalledAfterOnComplete() throws Exception {
        final Assertion<Boolean> errorThrown = new Assertion<>(false);
        Observable.create(new Action<Object>() {
            @Override
            public void onSubscribe(@NonNull Subscriber<Object> subscriber) {
                try {
                    subscriber.onComplete();
                    subscriber.onNext(null);
                } catch (RuntimeException e) {
                    errorThrown.set(true);
                }
            }
        }).subscribe(new OnSubscribe<Object>() {
            @Override
            public void onComplete() {
            }
        });
        Assert.assertTrue("Exception should be thrown in subscribe code if onNext called after onComplete",
            errorThrown.get());
    }

    @Test
    public void testObservableCreatesLooperIfNotThere() throws Exception {
        final Assertion<Boolean> looperInitiallyNull = new Assertion<>(false);
        final Assertion<Boolean> looperFinallyNotNull = new Assertion<>(false);
        final CountDownLatch latch = new CountDownLatch(1);
        Schedulers.newSingleThreadedScheduler().execute(new Runnable() {
            @Override
            public void run() {
                // No looper associated with this thread yet
                looperInitiallyNull.set(Looper.myLooper() == null);
                Observable.create(new Action<Object>() {
                    @Override
                    public void onSubscribe(@NonNull Subscriber<Object> subscriber) {
                        looperFinallyNotNull.set(Looper.myLooper() != null);
                    }
                }).subscribe();
                latch.countDown();
            }
        });
        latch.await();
        Assert.assertTrue("Looper should initially be null", looperInitiallyNull.get());
        Assert.assertTrue("Looper should be initialized by observable class", looperFinallyNotNull.get());
    }

    @Test
    public void testObservableSubscriberIsUnsubscribed() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch onNextLatch = new CountDownLatch(1);
        final Assertion<Boolean> unsubscribed = new Assertion<>(false);
        final List<String> list = new ArrayList<>();
        Subscription subscription = Observable.create(new Action<String>() {

            @Override
            public void onSubscribe(@NonNull Subscriber<String> subscriber) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext("test 1");
                }
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // should be unsubscribed after the latch countdown occurrs
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext("test 2");
                }
                unsubscribed.set(subscriber.isUnsubscribed());
            }
        }).subscribeOn(Schedulers.newSingleThreadedScheduler())
            .observeOn(Schedulers.newSingleThreadedScheduler())
            .subscribe(new OnSubscribe<String>() {
                @Override
                public void onNext(@Nullable String item) {
                    list.add(item);
                    onNextLatch.countDown();
                }
            });

        onNextLatch.await();
        subscription.unsubscribe();
        latch.countDown();

        Assert.assertTrue("Only one item should have been emitted", list.size() == 1);
        Assert.assertTrue("Wrong item emitted", list.get(0).equals("test 1"));
        Assert.assertTrue("isUnsubscribed() was not correct", unsubscribed.get());
    }

}