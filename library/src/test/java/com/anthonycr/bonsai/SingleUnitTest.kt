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

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class SingleUnitTest {

    @Mock
    private val stringSingleOnSubscribe: SingleOnSubscribe<String>? = null

    @Before
    fun before() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    @Throws(Exception::class)
    fun testSingleEmissionOrder_singleThread() {
        val testItem = "1"

        Single.create(SingleAction<String> { subscriber ->
            subscriber.onItem(testItem)
            subscriber.onComplete()
        }).subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe(stringSingleOnSubscribe!!)

        val inOrder = Mockito.inOrder(stringSingleOnSubscribe)

        inOrder.verify(stringSingleOnSubscribe).onStart()
        inOrder.verify(stringSingleOnSubscribe).onItem(testItem)
        inOrder.verify(stringSingleOnSubscribe).onComplete()

        Mockito.verifyNoMoreInteractions(stringSingleOnSubscribe)
    }

    @Test
    @Throws(Exception::class)
    fun testSingleMultipleEventEmission_throwsException() {
        val testItem = "2"

        val exceptionWasCause = AtomicReference(false)
        val throwableAssertion = AtomicReference<Throwable>(null)
        val stringItem = AtomicReference<String>(null)

        val onErrorCountdown = CountDownLatch(1)
        val onCompleteCountdown = CountDownLatch(1)

        Single.create(SingleAction<String> { subscriber ->
            try {
                subscriber.onItem(testItem)
                subscriber.onItem(testItem)
            } catch (e: Exception) {
                throwableAssertion.set(e)
                subscriber.onError(e)
            }

            subscriber.onComplete()
        }).subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.io())
                .subscribe(object : SingleOnSubscribe<String>() {
                    override fun onError(throwable: Throwable) {
                        exceptionWasCause.set(throwable == throwableAssertion.get())
                        onErrorCountdown.countDown()
                    }

                    override fun onItem(item: String?) {
                        stringItem.set(item)
                        onCompleteCountdown.countDown()
                    }

                })

        onErrorCountdown.await()
        onCompleteCountdown.await()

        assertTrue(exceptionWasCause.get())
        assertEquals(testItem, stringItem.get())
    }

    @Test
    @Throws(Exception::class)
    fun testSingleEventEmission_withoutSubscriber_withException() {
        Single.create(SingleAction<String> { subscriber ->
            subscriber.onItem(1.toString())
            throw RuntimeException("Test failure")
        }).subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.io())
                .subscribe()

        // No assertions since we did not supply an OnSubscribe.
    }

    @Test
    @Throws(Exception::class)
    fun testSingleEventEmission_withException() {
        val testItem = "1"
        val runtimeException = RuntimeException("Test failure")

        Single.create(SingleAction<String> { subscriber ->
            subscriber.onItem(testItem)
            throw runtimeException
        }).subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe(stringSingleOnSubscribe!!)

        val inOrder = Mockito.inOrder(stringSingleOnSubscribe)

        inOrder.verify(stringSingleOnSubscribe).onStart()
        inOrder.verify(stringSingleOnSubscribe).onItem(testItem)
        inOrder.verify(stringSingleOnSubscribe).onError(runtimeException)

        Mockito.verifyNoMoreInteractions(stringSingleOnSubscribe)
    }

    @Test
    @Throws(Exception::class)
    fun testSingleEventEmission_withError() {
        val testItem = "1"
        val exception = Exception("Test failure")

        Single.create(SingleAction<String> { subscriber ->
            subscriber.onItem(testItem)
            try {
                throw exception
            } catch (e: Exception) {
                subscriber.onError(e)
            }

            subscriber.onComplete()
        }).subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe(stringSingleOnSubscribe!!)

        val inOrder = Mockito.inOrder(stringSingleOnSubscribe)

        inOrder.verify(stringSingleOnSubscribe).onStart()
        inOrder.verify(stringSingleOnSubscribe).onItem(testItem)
        inOrder.verify(stringSingleOnSubscribe).onError(exception)

        Mockito.verifyNoMoreInteractions(stringSingleOnSubscribe)
    }

    @Test
    @Throws(Exception::class)
    fun testSingleEventEmission_withoutError() {
        val testItem = "1"

        Single.create(SingleAction<String> { subscriber ->
            subscriber.onItem(testItem)
            subscriber.onComplete()
        }).subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe(stringSingleOnSubscribe!!)

        val inOrder = Mockito.inOrder(stringSingleOnSubscribe)

        inOrder.verify(stringSingleOnSubscribe).onStart()
        inOrder.verify(stringSingleOnSubscribe).onItem(testItem)
        inOrder.verify(stringSingleOnSubscribe).onComplete()

        Mockito.verifyNoMoreInteractions(stringSingleOnSubscribe)
    }

    @Test
    @Throws(Exception::class)
    fun testStreamThrowsException_onStartCalled() {
        val throwableReference = AtomicReference<Throwable>(null)
        Single.create(SingleAction<String> { subscriber ->
            try {
                subscriber.onStart()
            } catch (e: Exception) {
                throwableReference.set(e)
                subscriber.onError(e)
            }
        }).subscribe(stringSingleOnSubscribe!!)

        val inOrder = Mockito.inOrder(stringSingleOnSubscribe)
        inOrder.verify(stringSingleOnSubscribe).onStart()
        inOrder.verify(stringSingleOnSubscribe).onError(throwableReference.get())

        Mockito.verifyNoMoreInteractions(stringSingleOnSubscribe)
    }

    @Test
    @Throws(Exception::class)
    fun testStreamThrowsException_onStartCalled_noOnSubscribe() {
        val errorThrown = AtomicReference(false)
        Single.create(SingleAction<Any> { subscriber ->
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
    fun testSingleUnsubscribe_unsubscribesSuccessfully() {
        val subscribeLatch = CountDownLatch(1)
        val latch = CountDownLatch(1)
        val assertion = AtomicReference(false)
        val stringSubscription = Single.create(SingleAction<String> { subscriber ->
            Utils.safeWait(subscribeLatch)
            subscriber.onItem("test")
            latch.countDown()
        }).subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(object : SingleOnSubscribe<String>() {
                    override fun onItem(item: String?) {
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
    fun testSingleThread_onStart_isCorrect() {
        val observeLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)

        val subscribeThreadAssertion = AtomicReference<String>()
        val observerThreadAssertion = AtomicReference<String>()

        val onStartAssertion = AtomicReference(false)
        val onErrorAssertion = AtomicReference(false)

        Single.create(SingleAction<String> { subscriber ->
            subscribeThreadAssertion.set(Thread.currentThread().toString())
            subscribeLatch.countDown()
            subscriber.onComplete()
        }).subscribeOn(Schedulers.worker())
                .observeOn(Schedulers.io())
                .subscribe(object : SingleOnSubscribe<String>() {
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
    fun testSingleThread_onItem_isCorrect() {
        val observeLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)

        val subscribeThreadAssertion = AtomicReference<String>()
        val observerThreadAssertion = AtomicReference<String>()

        val onItemAssertion = AtomicReference(false)
        val onErrorAssertion = AtomicReference(false)

        Single.create(SingleAction<String> { subscriber ->
            subscribeThreadAssertion.set(Thread.currentThread().toString())
            subscribeLatch.countDown()
            subscriber.onItem(null)
            subscriber.onComplete()
        }).subscribeOn(Schedulers.worker())
                .observeOn(Schedulers.io())
                .subscribe(object : SingleOnSubscribe<String>() {
                    override fun onError(throwable: Throwable) {
                        onErrorAssertion.set(true)
                        observerThreadAssertion.set(Thread.currentThread().toString())
                        observeLatch.countDown()
                    }

                    override fun onItem(item: String?) {
                        onItemAssertion.set(true)
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

        assertTrue(onItemAssertion.get())
        assertFalse(onErrorAssertion.get())
    }

    @Test
    @Throws(Exception::class)
    fun testSingleThread_onComplete_isCorrect() {
        val observeLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)

        val subscribeThreadAssertion = AtomicReference<String>()
        val observerThreadAssertion = AtomicReference<String>()

        val onCompleteAssertion = AtomicReference(false)
        val onErrorAssertion = AtomicReference(false)

        Single.create(SingleAction<String> { subscriber ->
            subscribeThreadAssertion.set(Thread.currentThread().toString())
            subscribeLatch.countDown()
            subscriber.onComplete()
        }).subscribeOn(Schedulers.worker())
                .observeOn(Schedulers.io())
                .subscribe(object : SingleOnSubscribe<String>() {
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
    fun testSingleThread_onError_isCorrect() {
        val observeLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)

        val subscribeThreadAssertion = AtomicReference<String>()
        val observerThreadAssertion = AtomicReference<String>()

        val onCompleteAssertion = AtomicReference(false)
        val onErrorAssertion = AtomicReference(false)

        Single.create(SingleAction<String> { subscriber ->
            subscribeThreadAssertion.set(Thread.currentThread().toString())
            subscribeLatch.countDown()
            subscriber.onError(RuntimeException("There was a problem"))
        }).subscribeOn(Schedulers.worker())
                .observeOn(Schedulers.io())
                .subscribe(object : SingleOnSubscribe<String>() {
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
    fun testSingleThread_ThrownException_isCorrect() {
        val observeLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)

        val subscribeThreadAssertion = AtomicReference<String>()
        val observerThreadAssertion = AtomicReference<String>()

        val onCompleteAssertion = AtomicReference(false)
        val onErrorAssertion = AtomicReference(false)

        Single.create(SingleAction<String> {
            subscribeThreadAssertion.set(Thread.currentThread().toString())
            subscribeLatch.countDown()
            throw RuntimeException("There was a problem")
        }).subscribeOn(Schedulers.worker())
                .observeOn(Schedulers.io())
                .subscribe(object : SingleOnSubscribe<String>() {
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
    fun testSingleSubscribesWithoutSubscriber() {
        val isCalledAssertion = AtomicReference(false)
        Single.create(SingleAction<Any> { subscriber ->
            subscriber.onItem(null)
            subscriber.onComplete()
            isCalledAssertion.set(true)
        }).subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe()
        assertTrue("onSubscribe must be called when subscribe is called", isCalledAssertion.get())
    }

    @Test
    @Throws(Exception::class)
    fun testSingleThrowsException_onCompleteCalledTwice() {
        val errorThrown = AtomicReference(false)
        Single.create(SingleAction<String> { subscriber ->
            try {
                subscriber.onComplete()
                subscriber.onComplete()
            } catch (e: RuntimeException) {
                errorThrown.set(true)
            }
        }).subscribe(stringSingleOnSubscribe!!)
        assertTrue("Exception should be thrown in subscribe code if onComplete called more than once",
                errorThrown.get())

        val inOrder = Mockito.inOrder(stringSingleOnSubscribe)

        inOrder.verify(stringSingleOnSubscribe).onStart()
        inOrder.verify(stringSingleOnSubscribe).onComplete()

        Mockito.verifyNoMoreInteractions(stringSingleOnSubscribe)
    }

    @Test
    @Throws(Exception::class)
    fun testCompletableThrowsException_onCompleteCalledTwice_noOnSubscribe() {
        val errorThrown = AtomicReference(false)
        Single.create(SingleAction<Any> { subscriber ->
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
    fun testSingleThrowsException_onItemCalledTwice() {
        val errorThrown = AtomicReference(false)
        Single.create(SingleAction<String> { subscriber ->
            try {
                subscriber.onItem(null)
                subscriber.onItem(null)
            } catch (e: RuntimeException) {
                errorThrown.set(true)
            }

            subscriber.onComplete()
        }).subscribe(stringSingleOnSubscribe!!)
        assertTrue("Exception should be thrown in subscribe code if onItem called after onComplete",
                errorThrown.get())

        val inOrder = Mockito.inOrder(stringSingleOnSubscribe)

        inOrder.verify(stringSingleOnSubscribe).onStart()
        inOrder.verify(stringSingleOnSubscribe).onItem(null)
        inOrder.verify(stringSingleOnSubscribe).onComplete()

        Mockito.verifyNoMoreInteractions(stringSingleOnSubscribe)
    }

    @Test
    @Throws(Exception::class)
    fun testSingleThrowsException_onItemCalledAfterOnComplete() {
        val errorThrown = AtomicReference(false)
        Single.create(SingleAction<String> { subscriber ->
            try {
                subscriber.onComplete()
                subscriber.onItem(null)
            } catch (e: RuntimeException) {
                errorThrown.set(true)
            }
        }).subscribe(stringSingleOnSubscribe!!)
        assertTrue("Exception should be thrown in subscribe code if onItem called after onComplete",
                errorThrown.get())

        val inOrder = Mockito.inOrder(stringSingleOnSubscribe)

        inOrder.verify(stringSingleOnSubscribe).onStart()
        inOrder.verify(stringSingleOnSubscribe).onComplete()

        Mockito.verifyNoMoreInteractions(stringSingleOnSubscribe)
    }

    @Test
    @Throws(Exception::class)
    fun testSingleSubscriberIsUnsubscribed() {
        val latch = CountDownLatch(1)
        val onFinalLatch = CountDownLatch(1)
        val unsubscribed = AtomicReference(false)
        val list = ArrayList<String>()
        val subscription = Single.create(SingleAction<String> { subscriber ->
            Utils.safeWait(latch)
            // should be unsubscribed after the latch countdown occurs
            if (!subscriber.isUnsubscribed) {
                subscriber.onItem("test 1")
            }
            unsubscribed.set(subscriber.isUnsubscribed)
            onFinalLatch.countDown()
        }).subscribeOn(Schedulers.newSingleThreadedScheduler())
                .observeOn(Schedulers.newSingleThreadedScheduler())
                .subscribe(object : SingleOnSubscribe<String>() {
                    override fun onItem(item: String?) {
                        list.add(item!!)
                    }
                })

        subscription.unsubscribe()
        latch.countDown()
        onFinalLatch.await()

        assertTrue("No items should have been emitted", list.size == 0)
        assertTrue("isUnsubscribed() was not correct", unsubscribed.get())
    }

    @Test
    @Throws(Exception::class)
    fun testSingleEmpty_emitsNothingImmediately() {
        val stringSingle = Single.empty<String>()
        stringSingle.subscribe(stringSingleOnSubscribe!!)

        val inOrder = Mockito.inOrder(stringSingleOnSubscribe)

        inOrder.verify(stringSingleOnSubscribe).onStart()
        inOrder.verify(stringSingleOnSubscribe).onComplete()

        Mockito.verifyNoMoreInteractions(stringSingleOnSubscribe)
    }

}
