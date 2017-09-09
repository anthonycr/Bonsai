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
    private val stringStreamOnSubscribe: StreamOnSubscribe<String>? = null

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    @Throws(Exception::class)
    fun testStreamEmissionOrder_singleThread() {
        val testList = Arrays.asList("1", "2", "3", "4", "5", "6", "7")

        Stream.create(StreamAction<String> { subscriber ->
            for (item in testList) {
                subscriber.onNext(item)
            }
            subscriber.onComplete()
        }).subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe(stringStreamOnSubscribe!!)

        val inOrder = Mockito.inOrder(stringStreamOnSubscribe)

        inOrder.verify(stringStreamOnSubscribe).onStart()
        for (item in testList) {
            inOrder.verify(stringStreamOnSubscribe).onNext(item)
        }
        inOrder.verify(stringStreamOnSubscribe).onComplete()

        Mockito.verifyNoMoreInteractions(stringStreamOnSubscribe)
    }

    @Test
    @Throws(Exception::class)
    fun testStreamEventEmission_withException() {
        val testList = Arrays.asList("1", "2", "3", "4", "5", "6", "7")
        val runtimeException = RuntimeException("Test failure")

        Stream.create(StreamAction<String> { subscriber ->
            for (item in testList) {
                subscriber.onNext(item)
            }
            throw runtimeException
        }).subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe(stringStreamOnSubscribe!!)

        val inOrder = Mockito.inOrder(stringStreamOnSubscribe)

        inOrder.verify(stringStreamOnSubscribe).onStart()
        for (item in testList) {
            inOrder.verify(stringStreamOnSubscribe).onNext(item)
        }
        inOrder.verify(stringStreamOnSubscribe).onError(runtimeException)

        Mockito.verifyNoMoreInteractions(stringStreamOnSubscribe)
    }

    @Test
    @Throws(Exception::class)
    fun testStreamEventEmission_withoutSubscriber_withException() {
        val testList = Arrays.asList("1", "2", "3", "4", "5", "6", "7")

        Stream.create(StreamAction<String> { subscriber ->
            for (item in testList) {
                subscriber.onNext(item)
            }
            throw RuntimeException("Test failure")
        }).subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe()

        // No assertions since we did not supply an OnSubscribe.
    }

    @Test
    @Throws(Exception::class)
    fun testStreamEventEmission_withError() {
        val testList = Arrays.asList("1", "2", "3", "4", "5", "6", "7")
        val exception = Exception("Test failure")

        Stream.create(StreamAction<String> { subscriber ->
            for (item in testList) {
                subscriber.onNext(item)
            }
            try {
                throw exception
            } catch (e: Exception) {
                subscriber.onError(e)
            }

            subscriber.onComplete()
        }).subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe(stringStreamOnSubscribe!!)

        val inOrder = Mockito.inOrder(stringStreamOnSubscribe)

        inOrder.verify(stringStreamOnSubscribe).onStart()
        for (item in testList) {
            inOrder.verify(stringStreamOnSubscribe).onNext(item)
        }
        inOrder.verify(stringStreamOnSubscribe).onError(exception)

        Mockito.verifyNoMoreInteractions(stringStreamOnSubscribe)
    }

    @Test
    @Throws(Exception::class)
    fun testStreamUnsubscribe_unsubscribesSuccessfully() {
        val subscribeLatch = CountDownLatch(1)
        val latch = CountDownLatch(1)
        val assertion = AtomicReference(false)
        val stringSubscription = Stream.create(StreamAction<String> { subscriber ->
            Utils.safeWait(subscribeLatch)
            subscriber.onNext("test")
            latch.countDown()
        }).subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(object : StreamOnSubscribe<String>() {
                    override fun onNext(item: String?) {
                        assertion.set(true)
                    }
                })

        stringSubscription.unsubscribe()
        subscribeLatch.countDown()
        latch.await()

        assertFalse(assertion.get())
    }

    @Test
    @Throws(Exception::class)
    fun testStreamThread_onStart_isCorrect() {
        val observeLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)

        val subscribeThreadAssertion = AtomicReference<String>()
        val observerThreadAssertion = AtomicReference<String>()

        val onStartAssertion = AtomicReference(false)
        val onErrorAssertion = AtomicReference(false)

        Stream.create(StreamAction<String> { subscriber ->
            subscribeThreadAssertion.set(Thread.currentThread().toString())
            subscribeLatch.countDown()
            subscriber.onComplete()
        }).subscribeOn(Schedulers.worker())
                .observeOn(Schedulers.io())
                .subscribe(object : StreamOnSubscribe<String>() {
                    override fun onError(throwable: Throwable) {
                        onErrorAssertion.set(true)
                        observerThreadAssertion.set(Thread.currentThread().toString())
                        observeLatch.countDown()
                    }

                    override fun onStart() {
                        onStartAssertion.set(true)
                        observerThreadAssertion.set(Thread.currentThread().toString())
                        observeLatch.countDown()
                    }
                })

        subscribeLatch.await()
        observeLatch.await()

        val currentThread = Thread.currentThread().toString()

        assertNotNull(subscribeThreadAssertion.get())
        assertNotNull(observerThreadAssertion.get())

        assertNotEquals(subscribeThreadAssertion.get(), currentThread)
        assertNotEquals(observerThreadAssertion.get(), currentThread)
        assertNotEquals(subscribeThreadAssertion.get(), observerThreadAssertion.get())

        assertTrue(onStartAssertion.get())
        assertFalse(onErrorAssertion.get())
    }

    @Test
    @Throws(Exception::class)
    fun testStreamThread_onNext_isCorrect() {
        val observeLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)

        val subscribeThreadAssertion = AtomicReference<String>()
        val observerThreadAssertion = AtomicReference<String>()

        val onNextAssertion = AtomicReference(false)
        val onErrorAssertion = AtomicReference(false)

        Stream.create(StreamAction<String> { subscriber ->
            subscribeThreadAssertion.set(Thread.currentThread().toString())
            subscribeLatch.countDown()
            subscriber.onNext(null)
            subscriber.onComplete()
        }).subscribeOn(Schedulers.worker())
                .observeOn(Schedulers.io())
                .subscribe(object : StreamOnSubscribe<String>() {
                    override fun onError(throwable: Throwable) {
                        onErrorAssertion.set(true)
                        observerThreadAssertion.set(Thread.currentThread().toString())
                        observeLatch.countDown()
                    }

                    override fun onNext(item: String?) {
                        onNextAssertion.set(true)
                        observerThreadAssertion.set(Thread.currentThread().toString())
                        observeLatch.countDown()
                    }
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
    @Throws(Exception::class)
    fun testStreamThread_onComplete_isCorrect() {
        val observeLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)

        val subscribeThreadAssertion = AtomicReference<String>()
        val observerThreadAssertion = AtomicReference<String>()

        val onCompleteAssertion = AtomicReference(false)
        val onErrorAssertion = AtomicReference(false)

        Stream.create(StreamAction<String> { subscriber ->
            subscribeThreadAssertion.set(Thread.currentThread().toString())
            subscribeLatch.countDown()
            subscriber.onComplete()
        }).subscribeOn(Schedulers.worker())
                .observeOn(Schedulers.io())
                .subscribe(object : StreamOnSubscribe<String>() {
                    override fun onError(throwable: Throwable) {
                        onErrorAssertion.set(true)
                        observerThreadAssertion.set(Thread.currentThread().toString())
                        observeLatch.countDown()
                    }

                    override fun onComplete() {
                        onCompleteAssertion.set(true)
                        observerThreadAssertion.set(Thread.currentThread().toString())
                        observeLatch.countDown()
                    }
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
    @Throws(Exception::class)
    fun testStreamThread_onError_isCorrect() {
        val observeLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)

        val subscribeThreadAssertion = AtomicReference<String>()
        val observerThreadAssertion = AtomicReference<String>()

        val onCompleteAssertion = AtomicReference(false)
        val onErrorAssertion = AtomicReference(false)

        Stream.create(StreamAction<String> { subscriber ->
            subscribeThreadAssertion.set(Thread.currentThread().toString())
            subscribeLatch.countDown()
            subscriber.onError(RuntimeException("There was a problem"))
        }).subscribeOn(Schedulers.worker())
                .observeOn(Schedulers.io())
                .subscribe(object : StreamOnSubscribe<String>() {
                    override fun onError(throwable: Throwable) {
                        onErrorAssertion.set(true)
                        observerThreadAssertion.set(Thread.currentThread().toString())
                        observeLatch.countDown()
                    }

                    override fun onComplete() {
                        onCompleteAssertion.set(true)
                        observerThreadAssertion.set(Thread.currentThread().toString())
                        observeLatch.countDown()
                    }
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
    @Throws(Exception::class)
    fun testStreamThread_ThrownException_isCorrect() {
        val observeLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)

        val subscribeThreadAssertion = AtomicReference<String>()
        val observerThreadAssertion = AtomicReference<String>()

        val onCompleteAssertion = AtomicReference(false)
        val onErrorAssertion = AtomicReference(false)

        Stream.create(StreamAction<String> {
            subscribeThreadAssertion.set(Thread.currentThread().toString())
            subscribeLatch.countDown()
            throw RuntimeException("There was a problem")
        }).subscribeOn(Schedulers.worker())
                .observeOn(Schedulers.io())
                .subscribe(object : StreamOnSubscribe<String>() {
                    override fun onError(throwable: Throwable) {
                        onErrorAssertion.set(true)
                        observerThreadAssertion.set(Thread.currentThread().toString())
                        observeLatch.countDown()
                    }

                    override fun onComplete() {
                        onCompleteAssertion.set(true)
                        observerThreadAssertion.set(Thread.currentThread().toString())
                        observeLatch.countDown()
                    }
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
    @Throws(Exception::class)
    fun testStreamSubscribesWithoutSubscriber() {
        val isCalledAssertion = AtomicReference(false)
        Stream.create(StreamAction<Any> { subscriber ->
            subscriber.onNext(null)
            subscriber.onComplete()
            isCalledAssertion.set(true)
        }).subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe()
        assertTrue("onSubscribe must be called when subscribe is called", isCalledAssertion.get())
    }

    @Test
    @Throws(Exception::class)
    fun testStreamThrowsException_onCompleteCalledTwice() {
        val thrownException = AtomicReference<Throwable>(null)
        Stream.create(StreamAction<String> { subscriber ->
            try {
                subscriber.onComplete()
                subscriber.onComplete()
            } catch (e: RuntimeException) {
                thrownException.set(e)
                throw e
            }
        }).subscribe(stringStreamOnSubscribe!!)

        Assert.assertNotNull(thrownException.get())

        val inOrder = Mockito.inOrder(stringStreamOnSubscribe)

        inOrder.verify(stringStreamOnSubscribe).onStart()
        inOrder.verify(stringStreamOnSubscribe).onComplete()

        Mockito.verifyNoMoreInteractions(stringStreamOnSubscribe)
    }

    @Test
    @Throws(Exception::class)
    fun testStreamThrowsException_onCompleteCalledTwice_noOnSubscribe() {
        val errorThrown = AtomicReference(false)
        Stream.create(StreamAction<Any> { subscriber ->
            try {
                subscriber.onComplete()
                subscriber.onComplete()
            } catch (e: RuntimeException) {
                errorThrown.set(true)
            }
        }).subscribe()
        assertTrue("Exception should be thrown in subscribe code if onComplete called more than once",
                errorThrown.get())
    }

    @Test
    @Throws(Exception::class)
    fun testStreamThrowsException_onStartCalled() {
        val thrownException = AtomicReference<Throwable>(null)
        Stream.create(StreamAction<String> { subscriber ->
            try {
                subscriber.onStart()
            } catch (exception: Exception) {
                thrownException.set(exception)
                throw exception
            }
        }).subscribe(stringStreamOnSubscribe!!)

        val inOrder = Mockito.inOrder(stringStreamOnSubscribe)

        inOrder.verify(stringStreamOnSubscribe).onStart()
        inOrder.verify(stringStreamOnSubscribe).onError(thrownException.get())

        Mockito.verifyNoMoreInteractions(stringStreamOnSubscribe)
    }

    @Test
    @Throws(Exception::class)
    fun testStreamThrowsException_onStartCalled_noOnSubscribe() {
        val errorThrown = AtomicReference(false)
        Stream.create(StreamAction<Any> { subscriber ->
            try {
                subscriber.onStart()
            } catch (exception: Exception) {
                errorThrown.set(true)
            }
        }).subscribe()
        assertTrue("Exception should be thrown in subscribe code if onStart is called", errorThrown.get())
    }

    @Test
    @Throws(Exception::class)
    fun testStreamThrowsException_onNextCalledAfterOnComplete() {
        val errorThrown = AtomicReference(false)
        Stream.create(StreamAction<String> { subscriber ->
            try {
                subscriber.onComplete()
                subscriber.onNext(null)
            } catch (e: RuntimeException) {
                errorThrown.set(true)
            }
        }).subscribe(stringStreamOnSubscribe!!)

        assertTrue("Exception should be thrown in subscribe code if onNext called after onComplete", errorThrown.get())

        val inOrder = Mockito.inOrder(stringStreamOnSubscribe)

        inOrder.verify(stringStreamOnSubscribe).onStart()
        inOrder.verify(stringStreamOnSubscribe).onComplete()

        Mockito.verifyNoMoreInteractions(stringStreamOnSubscribe)
    }

    @Test
    @Throws(Exception::class)
    fun testStreamSubscriberIsUnsubscribed() {
        val latch = CountDownLatch(1)
        val onNextLatch = CountDownLatch(1)
        val onFinalLatch = CountDownLatch(1)
        val unsubscribed = AtomicReference(false)
        val list = ArrayList<String>()
        val subscription = Stream.create(StreamAction<String> { subscriber ->
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
        }).subscribeOn(Schedulers.newSingleThreadedScheduler())
                .observeOn(Schedulers.newSingleThreadedScheduler())
                .subscribe(object : StreamOnSubscribe<String>() {
                    override fun onNext(item: String?) {
                        list.add(item!!)
                        onNextLatch.countDown()
                    }
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
    @Throws(Exception::class)
    fun testStreamEmpty_emitsNothingImmediately() {
        val stream = Stream.empty<String>()
        stream.subscribe(stringStreamOnSubscribe!!)

        val inOrder = Mockito.inOrder(stringStreamOnSubscribe)

        inOrder.verify(stringStreamOnSubscribe).onStart()
        inOrder.verify(stringStreamOnSubscribe).onComplete()

        Mockito.verifyNoMoreInteractions(stringStreamOnSubscribe)
    }

}