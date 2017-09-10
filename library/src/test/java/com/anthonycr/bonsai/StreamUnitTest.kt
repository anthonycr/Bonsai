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
package com.anthonycr.bonsai

import org.junit.Assert
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
class StreamUnitTest {

    @Mock
    lateinit var stringOnNext: (String) -> Unit

    @Mock
    lateinit var onComplete: () -> Unit

    @Mock
    lateinit var onError: (Throwable) -> Unit

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testStreamEmissionOrder_singleThread() {
        val testList = Arrays.asList("1", "2", "3", "4", "5", "6", "7")

        Stream.create<String> { subscriber ->
            for (item in testList) {
                subscriber.onNext(item)
            }
            subscriber.onComplete()
        }.subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe(stringOnNext, onComplete, onError)

        val inOrder = Mockito.inOrder(stringOnNext, onComplete)

        for (item in testList) {
            inOrder.verify(stringOnNext)(item)
        }
        inOrder.verify(onComplete)()

        stringOnNext.verifyNoMoreInteractions()
        onComplete.verifyNoMoreInteractions()
        onError.verifyZeroInteractions()
    }

    @Test
    fun testStreamEventEmission_withException() {
        val testList = Arrays.asList("1", "2", "3", "4", "5", "6", "7")
        val runtimeException = RuntimeException("Test failure")

        Stream.create<String> { subscriber ->
            for (item in testList) {
                subscriber.onNext(item)
            }
            throw runtimeException
        }.subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe(stringOnNext, onComplete, onError)

        val inOrder = Mockito.inOrder(stringOnNext, onError)

        for (item in testList) {
            inOrder.verify(stringOnNext)(item)
        }
        inOrder.verify(onError)(runtimeException)

        stringOnNext.verifyNoMoreInteractions()
        onError.verifyNoMoreInteractions()
        onComplete.verifyZeroInteractions()
    }

    @Test(expected = ReactiveEventException::class)
    fun testStreamEventEmission_withoutSubscriber_withException() {
        val testList = Arrays.asList("1", "2", "3", "4", "5", "6", "7")

        Stream.create<String> { subscriber ->
            for (item in testList) {
                subscriber.onNext(item)
            }
            throw RuntimeException("Test failure")
        }.subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe()
    }

    @Test
    fun testStreamEventEmission_withError() {
        val testList = Arrays.asList("1", "2", "3", "4", "5", "6", "7")
        val exception = Exception("Test failure")

        Stream.create<String> { subscriber ->
            for (item in testList) {
                subscriber.onNext(item)
            }

            subscriber.onError(exception)
        }.subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe(stringOnNext, onComplete, onError)

        val inOrder = Mockito.inOrder(stringOnNext, onError)

        for (item in testList) {
            inOrder.verify(stringOnNext)(item)
        }
        inOrder.verify(onError)(exception)

        stringOnNext.verifyNoMoreInteractions()
        onError.verifyNoMoreInteractions()
        onComplete.verifyZeroInteractions()
    }

    @Test
    fun testStreamUnsubscribe_unsubscribesSuccessfully() {
        val subscribeLatch = CountDownLatch(1)
        val latch = CountDownLatch(1)
        val assertion = AtomicReference(false)
        val stringSubscription = Stream.create<String> { subscriber ->
            Utils.safeWait(subscribeLatch)
            subscriber.onNext("test")
            latch.countDown()
        }.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(onNext = {
                    assertion.set(true)
                })

        stringSubscription.unsubscribe()
        subscribeLatch.countDown()
        latch.await()

        assertFalse(assertion.get())
    }

    @Test
    fun testStreamThread_onNext_isCorrect() {
        val observeLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)

        val subscribeThreadAssertion = AtomicReference<String>()
        val observerThreadAssertion = AtomicReference<String>()

        val onNextAssertion = AtomicReference(false)
        val onErrorAssertion = AtomicReference(false)

        Stream.create<String> { subscriber ->
            subscribeThreadAssertion.set(Thread.currentThread().toString())
            subscribeLatch.countDown()
            subscriber.onNext("test")
            subscriber.onComplete()
        }.subscribeOn(Schedulers.worker())
                .observeOn(Schedulers.io())
                .subscribe(onNext = {
                    onNextAssertion.set(true)
                    observerThreadAssertion.set(Thread.currentThread().toString())
                    observeLatch.countDown()
                }, onError = {
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

        assertTrue(onNextAssertion.get())
        assertFalse(onErrorAssertion.get())
    }

    @Test
    fun testStreamThread_onComplete_isCorrect() {
        val observeLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)

        val subscribeThreadAssertion = AtomicReference<String>()
        val observerThreadAssertion = AtomicReference<String>()

        val onCompleteAssertion = AtomicReference(false)
        val onErrorAssertion = AtomicReference(false)

        Stream.create<String> { subscriber ->
            subscribeThreadAssertion.set(Thread.currentThread().toString())
            subscribeLatch.countDown()
            subscriber.onComplete()
        }.subscribeOn(Schedulers.worker())
                .observeOn(Schedulers.io())
                .subscribe(onError = {
                    onErrorAssertion.set(true)
                    observerThreadAssertion.set(Thread.currentThread().toString())
                    observeLatch.countDown()
                }, onComplete = {
                    onCompleteAssertion.set(true)
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
    fun testStreamThread_onError_isCorrect() {
        val observeLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)

        val subscribeThreadAssertion = AtomicReference<String>()
        val observerThreadAssertion = AtomicReference<String>()

        val onCompleteAssertion = AtomicReference(false)
        val onErrorAssertion = AtomicReference(false)

        Stream.create<String> { subscriber ->
            subscribeThreadAssertion.set(Thread.currentThread().toString())
            subscribeLatch.countDown()
            subscriber.onError(RuntimeException("There was a problem"))
        }.subscribeOn(Schedulers.worker())
                .observeOn(Schedulers.io())
                .subscribe(onError = {
                    onErrorAssertion.set(true)
                    observerThreadAssertion.set(Thread.currentThread().toString())
                    observeLatch.countDown()
                }, onComplete = {
                    onCompleteAssertion.set(true)
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
    fun testStreamThread_ThrownException_isCorrect() {
        val observeLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)

        val subscribeThreadAssertion = AtomicReference<String>()
        val observerThreadAssertion = AtomicReference<String>()

        val onCompleteAssertion = AtomicReference(false)
        val onErrorAssertion = AtomicReference(false)

        Stream.create<String> {
            subscribeThreadAssertion.set(Thread.currentThread().toString())
            subscribeLatch.countDown()
            throw RuntimeException("There was a problem")
        }.subscribeOn(Schedulers.worker())
                .observeOn(Schedulers.io())
                .subscribe(onError = {
                    onErrorAssertion.set(true)
                    observerThreadAssertion.set(Thread.currentThread().toString())
                    observeLatch.countDown()
                }, onComplete = {
                    onCompleteAssertion.set(true)
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
    fun testStreamSubscribesWithoutSubscriber() {
        val isCalledAssertion = AtomicReference(false)
        Stream.create<Any> { subscriber ->
            subscriber.onNext(Any())
            subscriber.onComplete()
            isCalledAssertion.set(true)
        }.subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe()
        assertTrue("onSubscribe must be called when subscribe is called", isCalledAssertion.get())
    }

    @Test
    fun testStreamThrowsException_onCompleteCalledTwice() {
        val thrownException = AtomicReference<Throwable>(null)
        Stream.create<String> { subscriber ->
            try {
                subscriber.onComplete()
                subscriber.onComplete()
            } catch (e: ReactiveEventException) {
                thrownException.set(e)
            }
        }.subscribe(stringOnNext, onComplete, onError)

        Assert.assertNotNull(thrownException.get())

        val inOrder = Mockito.inOrder(onComplete)

        inOrder.verify(onComplete)()

        onComplete.verifyNoMoreInteractions()
        onError.verifyZeroInteractions()
        stringOnNext.verifyZeroInteractions()
    }

    @Test
    fun testStreamThrowsException_onCompleteCalledTwice_noOnSubscribe() {
        val errorThrown = AtomicReference(false)
        Stream.create<Any> { subscriber ->
            try {
                subscriber.onComplete()
                subscriber.onComplete()
            } catch (e: ReactiveEventException) {
                errorThrown.set(true)
            }
        }.subscribe()
        assertTrue("Exception should be thrown in subscribe code if onComplete called more than once",
                errorThrown.get())
    }

    @Test
    fun testStreamThrowsException_onNextCalledAfterOnComplete() {
        val errorThrown = AtomicReference(false)
        Stream.create<String> { subscriber ->
            try {
                subscriber.onComplete()
                subscriber.onNext("test")
            } catch (e: ReactiveEventException) {
                errorThrown.set(true)
            }
        }.subscribe(stringOnNext, onComplete, onError)

        assertTrue("Exception should be thrown in subscribe code if onNext called after onComplete", errorThrown.get())

        val inOrder = Mockito.inOrder(onComplete)

        inOrder.verify(onComplete)()

        onComplete.verifyNoMoreInteractions()
        stringOnNext.verifyZeroInteractions()
        onError.verifyZeroInteractions()
    }

    @Test
    fun testStreamSubscriberIsUnsubscribed() {
        val latch = CountDownLatch(1)
        val onNextLatch = CountDownLatch(1)
        val onFinalLatch = CountDownLatch(1)
        val unsubscribed = AtomicReference(false)
        val list = ArrayList<String>()
        val subscription = Stream.create<String> { subscriber ->
            if (!subscriber.isUnsubscribed) {
                subscriber.onNext("test 1")
            }
            Utils.safeWait(latch)
            // should be unsubscribed after the latch countdown occurs
            if (!subscriber.isUnsubscribed) {
                subscriber.onNext("test 2")
            }
            unsubscribed.set(subscriber.isUnsubscribed)
            onFinalLatch.countDown()
        }.subscribeOn(Schedulers.newSingleThreadedScheduler())
                .observeOn(Schedulers.newSingleThreadedScheduler())
                .subscribe(onNext = {
                    list.add(it)
                    onNextLatch.countDown()
                })

        onNextLatch.await()
        subscription.unsubscribe()
        latch.countDown()
        onFinalLatch.await()

        assertTrue("Only one item should have been emitted", list.size == 1)
        assertTrue("Wrong item emitted", list[0] == "test 1")
        assertTrue("isUnsubscribed() was not correct", unsubscribed.get())
    }

    @Test
    fun testStreamEmpty_emitsNothingImmediately() {
        val stream = Stream.empty<String>()
        stream.subscribe(stringOnNext, onComplete, onError)

        val inOrder = Mockito.inOrder(onComplete)

        inOrder.verify(onComplete)()

        onComplete.verifyNoMoreInteractions()
        stringOnNext.verifyZeroInteractions()
        onError.verifyZeroInteractions()
    }

}