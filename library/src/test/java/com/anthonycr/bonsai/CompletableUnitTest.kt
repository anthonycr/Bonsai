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
package com.anthonycr.bonsai

import com.anthonycr.bonsai.refactor.ReactiveEventException
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class CompletableUnitTest {

    @Mock
    lateinit var onComplete: () -> Unit

    @Mock
    lateinit var onError: (Throwable) -> Unit

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testCompletableEventEmission_withException() {
        val runtimeException = RuntimeException("Test failure")

        Completable.create { throw runtimeException }
                .subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe(onComplete, onError)

        onError.verifyOnlyOneInteraction()(runtimeException)
        onError.verifyNoMoreInteractions()

        onComplete.verifyZeroInteractions()
    }

    @Test(expected = ReactiveEventException::class)
    fun testCompletableEventEmission_withoutSubscriber_withException() {
        Completable.create { throw RuntimeException("Test failure") }
                .subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe()
    }

    @Test
    fun testCompletableEventEmission_withError() {
        val exception = Exception("Test failure")

        Completable.create { subscriber ->
            subscriber.onError(exception)
        }.subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe(onComplete, onError)

        onError.verifyOnlyOneInteraction()(exception)
        onError.verifyNoMoreInteractions()

        onComplete.verifyZeroInteractions()
    }

    @Test
    fun testCompletableEventEmission_withoutError() {
        val onSubscribeAssertion = AtomicReference(false)

        Completable.create { subscriber ->
            onSubscribeAssertion.set(true)
            subscriber.onComplete()
        }.subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe(onComplete, onError)

        // Assert that each of the events was
        // received by the subscriber
        assertTrue(onSubscribeAssertion.get())

        onComplete.verifyOnlyOneInteraction()()
        onComplete.verifyNoMoreInteractions()

        onError.verifyZeroInteractions()
    }

    @Test
    fun testCompletableUnsubscribe_unsubscribesSuccessfully() {
        val subscribeLatch = CountDownLatch(1)
        val latch = CountDownLatch(1)
        val stringSubscription = Completable.create { subscriber ->
            Utils.safeWait(subscribeLatch)
            subscriber.onComplete()
            latch.countDown()
        }.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(onComplete, onError)

        stringSubscription.unsubscribe()
        subscribeLatch.countDown()
        latch.await()

        onComplete.verifyZeroInteractions()
        onError.verifyZeroInteractions()
    }

    @Test
    fun testCompletableThread_onComplete_isCorrect() {
        val observeLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)

        val subscribeThreadAssertion = AtomicReference<String>()
        val observerThreadAssertion = AtomicReference<String>()

        val onCompleteAssertion = AtomicReference(false)
        val onErrorAssertion = AtomicReference(false)

        Completable.create { subscriber ->
            subscribeThreadAssertion.set(Thread.currentThread().toString())
            subscribeLatch.countDown()
            subscriber.onComplete()
        }.subscribeOn(Schedulers.worker())
                .observeOn(Schedulers.io())
                .subscribe({
                    onCompleteAssertion.set(true)
                    observerThreadAssertion.set(Thread.currentThread().toString())
                    observeLatch.countDown()
                }, {
                    onErrorAssertion.set(true)
                    observerThreadAssertion.set(Thread.currentThread().toString())
                    observeLatch.countDown()
                })

        subscribeLatch.await()
        observeLatch.await()

        val currentThread = Thread.currentThread().toString()

        assertNotNull(subscribeThreadAssertion.get())
        assertNotNull(observerThreadAssertion.get())

        assertNotEquals(subscribeThreadAssertion.get(), currentThread)
        assertNotEquals(observerThreadAssertion.get(), currentThread)
        assertNotEquals(subscribeThreadAssertion.get(), observerThreadAssertion.get())

        assertTrue(onCompleteAssertion.get())
        assertFalse(onErrorAssertion.get())
    }

    @Test
    fun testCompletableThread_onError_isCorrect() {
        val observeLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)

        val subscribeThreadAssertion = AtomicReference<String>()
        val observerThreadAssertion = AtomicReference<String>()

        val onCompleteAssertion = AtomicReference(false)
        val onErrorAssertion = AtomicReference(false)

        Completable.create { subscriber ->
            subscribeThreadAssertion.set(Thread.currentThread().toString())
            subscribeLatch.countDown()
            subscriber.onError(RuntimeException("There was a problem"))
        }.subscribeOn(Schedulers.worker())
                .observeOn(Schedulers.io())
                .subscribe({
                    onCompleteAssertion.set(true)
                    observerThreadAssertion.set(Thread.currentThread().toString())
                    observeLatch.countDown()
                }, {
                    onErrorAssertion.set(true)
                    observerThreadAssertion.set(Thread.currentThread().toString())
                    observeLatch.countDown()
                })

        subscribeLatch.await()
        observeLatch.await()

        val currentThread = Thread.currentThread().toString()

        assertNotNull(subscribeThreadAssertion.get())
        assertNotNull(observerThreadAssertion.get())

        assertNotEquals(subscribeThreadAssertion.get(), currentThread)
        assertNotEquals(observerThreadAssertion.get(), currentThread)
        assertNotEquals(subscribeThreadAssertion.get(), observerThreadAssertion.get())

        assertFalse(onCompleteAssertion.get())
        assertTrue(onErrorAssertion.get())
    }

    @Test
    fun testCompletableThread_ThrownException_isCorrect() {
        val observeLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)

        val subscribeThreadAssertion = AtomicReference<String>()
        val observerThreadAssertion = AtomicReference<String>()

        val onCompleteAssertion = AtomicReference(false)
        val onErrorAssertion = AtomicReference(false)

        Completable.create {
            subscribeThreadAssertion.set(Thread.currentThread().toString())
            subscribeLatch.countDown()
            throw RuntimeException("There was a problem")
        }.subscribeOn(Schedulers.worker())
                .observeOn(Schedulers.io())
                .subscribe({
                    onCompleteAssertion.set(true)
                    observerThreadAssertion.set(Thread.currentThread().toString())
                    observeLatch.countDown()
                }, {
                    onErrorAssertion.set(true)
                    observerThreadAssertion.set(Thread.currentThread().toString())
                    observeLatch.countDown()
                })

        subscribeLatch.await()
        observeLatch.await()

        val currentThread = Thread.currentThread().toString()

        assertNotNull(subscribeThreadAssertion.get())
        assertNotNull(observerThreadAssertion.get())

        assertNotEquals(subscribeThreadAssertion.get(), currentThread)
        assertNotEquals(observerThreadAssertion.get(), currentThread)
        assertNotEquals(subscribeThreadAssertion.get(), observerThreadAssertion.get())

        assertFalse(onCompleteAssertion.get())
        assertTrue(onErrorAssertion.get())
    }

    @Test
    fun testCompletableSubscribesWithoutSubscriber() {
        val isCalledAssertion = AtomicReference(false)
        Completable.create { subscriber ->
            subscriber.onComplete()
            isCalledAssertion.set(true)
        }.subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe()

        assertTrue("onSubscribe must be called when subscribe is called", isCalledAssertion.get())
    }

    @Test(expected = ReactiveEventException::class)
    fun testCompletableThrowsException_onCompleteCalledTwice() {
        Completable.create {
            it.onComplete()
            it.onComplete()
        }.subscribe(onComplete, onError)
    }

    @Test(expected = ReactiveEventException::class)
    fun testCompletableThrowsException_onCompleteCalledTwice_noOnSubscribe() {
        Completable.create { subscriber ->
            subscriber.onComplete()
            subscriber.onComplete()
        }.subscribe()
    }

    @Test
    fun testCompletableSubscriberIsUnsubscribed() {
        val latch = CountDownLatch(1)
        val onFinalLatch = CountDownLatch(1)

        val onComplete = AtomicReference(false)
        val onError = AtomicReference(false)

        val unsubscribed = AtomicReference(false)
        val workAssertion = AtomicReference(false)

        val subscription = Completable.create { subscriber ->
            Utils.safeWait(latch)
            // should be unsubscribed after the latch countdown occurs
            if (!subscriber.isUnsubscribed) {
                workAssertion.set(true)
            }
            unsubscribed.set(subscriber.isUnsubscribed)
            subscriber.onComplete()
            onFinalLatch.countDown()
        }.subscribeOn(Schedulers.newSingleThreadedScheduler())
                .observeOn(Schedulers.newSingleThreadedScheduler())
                .subscribe({
                    onComplete.set(true)
                }, {
                    onError.set(true)
                })

        subscription.unsubscribe()
        latch.countDown()
        onFinalLatch.await()

        assertFalse(workAssertion.get())
        assertTrue("isUnsubscribed() was not correct", unsubscribed.get())
        assertFalse(onComplete.get())
        assertFalse(onError.get())

    }

    @Test
    fun testDefaultSubscriber_createdOnSubscribeThread() {
        val countDownLatch = CountDownLatch(1)
        val threadInitializationLatch = CountDownLatch(2)
        val singleThreadRef1 = AtomicReference<String>(null)
        val singleThreadRef2 = AtomicReference<String>(null)

        val singleThread1 = Schedulers.newSingleThreadedScheduler()
        val singleThread2 = Schedulers.newSingleThreadedScheduler()
        singleThread1.execute {
            singleThreadRef1.set(Thread.currentThread().toString())
            threadInitializationLatch.countDown()
        }

        singleThread2.execute {
            singleThreadRef2.set(Thread.currentThread().toString())
            threadInitializationLatch.countDown()
        }
        // Wait until we know the thread names
        threadInitializationLatch.await()

        // Ensure that the inner completable is executed on the subscribe
        // thread, not the thread that the completable was created on.
        val innerCompletable = Completable.create { subscriber ->
            Assert.assertEquals(singleThreadRef1.get(), Thread.currentThread().toString())
            subscriber.onComplete()
        }

        // Ensure that the outer completable observes the inner completable
        // on the same thread on which it subscribed, not the thread it was
        // created on.
        val outerCompletable = Completable.create { subscriber ->
            val currentThread = Thread.currentThread().toString()
            innerCompletable.subscribe(onComplete = {
                Assert.assertEquals(Thread.currentThread().toString(), currentThread)
                subscriber.onComplete()
            })
        }

        outerCompletable
                .subscribeOn(singleThread1)
                .observeOn(singleThread2)
                .subscribe(onComplete = {
                    println(singleThreadRef1.get() + " " + singleThreadRef2.get())
                    Assert.assertEquals(singleThreadRef2.get(), Thread.currentThread().toString())
                    countDownLatch.countDown()
                })

        countDownLatch.await()
    }

    @Test
    @Throws(Exception::class)
    fun testCompletableEmpty_emitsNothingImmediately() {
        Completable.complete().subscribe(onComplete, onError)

        onComplete.verifyOnlyOneInteraction()()
        onComplete.verifyNoMoreInteractions()

        onError.verifyZeroInteractions()
    }

}
