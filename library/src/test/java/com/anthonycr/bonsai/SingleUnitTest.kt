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
import com.nhaarman.mockito_kotlin.isA
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
    lateinit var stringOnSuccess: (String) -> Unit

    @Mock
    lateinit var stringOnError: (Throwable) -> Unit

    @Before
    fun before() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testSingleEmissionOrder_singleThread() {
        val testItem = "1"

        Single.create<String> {
            it.onSuccess(testItem)
        }.subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe(stringOnSuccess, stringOnError)

        Mockito.verify(stringOnSuccess, Mockito.times(1))(testItem)

        Mockito.verifyNoMoreInteractions(stringOnSuccess)
        Mockito.verifyZeroInteractions(stringOnError)
    }

    @Test(expected = ReactiveEventException::class)
    fun testSingleMultipleEventEmission_throwsException() {
        val testItem = "2"

        Single.create<String> {
            it.onSuccess(testItem)
            it.onSuccess(testItem)
        }.subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe()
    }

    @Test(expected = ReactiveEventException::class)
    fun testSingleEventEmission_withoutSubscriberWithException_throwsException() {
        Single.create<String> { subscriber ->
            subscriber.onSuccess(1.toString())
            throw RuntimeException("Test failure")
        }.subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe()
    }

    @Test(expected = Exception::class)
    fun testSingleEventEmission_withException_throwsException() {
        val testItem = "1"
        val runtimeException = RuntimeException("Test failure")

        Single.create<String> { subscriber ->
            subscriber.onSuccess(testItem)
            throw runtimeException
        }.subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe(stringOnSuccess, stringOnError)

    }

    @Test
    fun testSingleEventEmission_withError() {
        val exception = Exception("Test failure")

        Single.create<String> {
            throw exception
        }.subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe(stringOnSuccess, stringOnError)

        Mockito.verify(stringOnError, Mockito.times(1))(exception)
        Mockito.verifyNoMoreInteractions(stringOnError)
        Mockito.verifyZeroInteractions(stringOnSuccess)
    }

    @Test
    fun testSingleEventEmission_withoutError() {
        val testItem = "1"

        Single.create<String> {
            it.onSuccess(testItem)
        }.subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe(stringOnSuccess, stringOnError)

        Mockito.verify(stringOnSuccess, Mockito.times(1))(testItem)
        Mockito.verifyNoMoreInteractions(stringOnSuccess)
        Mockito.verifyZeroInteractions(stringOnError)
    }

    @Test
    fun testSingleUnsubscribe_unsubscribesSuccessfully() {
        val subscribeLatch = CountDownLatch(1)
        val emissionLatch = CountDownLatch(1)
        val assertion = AtomicReference<String>(null)
        val stringSubscription = Single.create<String> { subscriber ->
            Utils.safeWait(subscribeLatch)
            subscriber.onSuccess("test")
            emissionLatch.countDown()
        }.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(onSuccess = { assertion.set(it) })

        stringSubscription.unsubscribe()
        subscribeLatch.countDown()
        emissionLatch.await()

        assertNull(assertion.get())
    }

    @Test
    fun testSingleThread_onItem_isCorrect() {
        val observeLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)

        val subscribeThreadAssertion = AtomicReference<String>()
        val observerThreadAssertion = AtomicReference<String>()

        val onItemAssertion = AtomicReference(false)
        val onErrorAssertion = AtomicReference(false)

        Single.create<String> { subscriber ->
            subscribeThreadAssertion.set(Thread.currentThread().toString())
            subscribeLatch.countDown()
            subscriber.onSuccess("test")
        }.subscribeOn(Schedulers.worker())
                .observeOn(Schedulers.io())
                .subscribe({
                    onItemAssertion.set(true)
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

        assertTrue(onItemAssertion.get())
        assertFalse(onErrorAssertion.get())
    }

    @Test
    fun testSingleThread_onError_isCorrect() {
        val observeLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)

        val subscribeThreadAssertion = AtomicReference<String>()
        val observerThreadAssertion = AtomicReference<String>()

        val onSuccessAssertion = AtomicReference(false)
        val onErrorAssertion = AtomicReference(false)

        Single.create<String> { subscriber ->
            subscribeThreadAssertion.set(Thread.currentThread().toString())
            subscribeLatch.countDown()
            subscriber.onError(RuntimeException("There was a problem"))
        }.subscribeOn(Schedulers.worker())
                .observeOn(Schedulers.io())
                .subscribe({
                    onSuccessAssertion.set(true)
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

        assertFalse(onSuccessAssertion.get())
        assertTrue(onErrorAssertion.get())
    }

    @Test
    fun testSingleThread_ThrownException_isCorrect() {
        val observeLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)

        val subscribeThreadAssertion = AtomicReference<String>()
        val observerThreadAssertion = AtomicReference<String>()

        val onCompleteAssertion = AtomicReference(false)
        val onErrorAssertion = AtomicReference(false)

        Single.create<String> {
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
    fun testSingleSubscribesWithoutSubscriber() {
        val isCalledAssertion = AtomicReference(false)
        Single.create<Any> { subscriber ->
            subscriber.onSuccess(Any())
            isCalledAssertion.set(true)
        }.subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .subscribe()
        assertTrue("onSubscribe must be called when subscribe is called", isCalledAssertion.get())
    }

    @Test
    @Throws(Exception::class)
    fun testSingleThrowsException_onItemCalledTwice() {
        val errorThrown = AtomicReference(false)
        val emission1 = "test1"
        val emission2 = "test2"
        Single.create<String> { subscriber ->
            try {
                subscriber.onSuccess(emission1)
                subscriber.onSuccess(emission2)
            } catch (e: ReactiveEventException) {
                errorThrown.set(true)
            }
        }.subscribe(stringOnSuccess, stringOnError)
        assertTrue("Exception should be thrown in subscribe code if onItem called after onComplete",
                errorThrown.get())

        Mockito.verify(stringOnSuccess, Mockito.times(1))(emission1)
        Mockito.verifyNoMoreInteractions(stringOnSuccess)
        Mockito.verifyZeroInteractions(stringOnError)

        assertTrue(errorThrown.get())
    }

    @Test
    fun testSingleSubscriberIsUnsubscribed() {
        val latch = CountDownLatch(1)
        val onFinalLatch = CountDownLatch(1)
        val unsubscribed = AtomicReference(false)
        val list = ArrayList<String>()
        val subscription = Single.create<String> { subscriber ->
            Utils.safeWait(latch)
            // should be unsubscribed after the latch countdown occurs
            if (!subscriber.isUnsubscribed) {
                subscriber.onSuccess("test 1")
            }
            unsubscribed.set(subscriber.isUnsubscribed)
            onFinalLatch.countDown()
        }.subscribeOn(Schedulers.newSingleThreadedScheduler())
                .observeOn(Schedulers.newSingleThreadedScheduler())
                .subscribe(onSuccess = {
                    list.add(it)
                })

        subscription.unsubscribe()
        latch.countDown()
        onFinalLatch.await()

        assertTrue("No items should have been emitted", list.size == 0)
        assertTrue("isUnsubscribed() was not correct", unsubscribed.get())
    }

    @Test
    fun testSingleEmpty_emitsNothingImmediately() {
        val stringSingle = Single.error<String>()
        stringSingle.subscribe(stringOnSuccess, stringOnError)

        Mockito.verify(stringOnError, Mockito.times(1))(isA<RuntimeException>())
        Mockito.verifyNoMoreInteractions(stringOnError)
        Mockito.verifyZeroInteractions(stringOnSuccess)
    }

}
