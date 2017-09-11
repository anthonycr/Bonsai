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

import android.os.Handler
import android.os.Looper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, sdk = intArrayOf(25))
class SchedulersUnitTest {

    @Test
    fun testMainScheduler_isCorrect() {
        val mainScheduler = AtomicReference<Boolean>()
        val latch = CountDownLatch(1)
        Schedulers.main().execute {
            mainScheduler.set(Looper.myLooper() == Looper.getMainLooper())
            latch.countDown()
        }
        latch.await()
        assertTrue(mainScheduler.get())
    }

    @Test
    fun testImmediateScheduler_isSynchronous() {
        val executedBoolean = AtomicReference(false)
        val currentScheduler = AtomicReference<Looper>()
        val latch = CountDownLatch(1)

        Utils.prepareLooper()
        val currentLooper = Looper.myLooper()

        Schedulers.immediate().execute({
            Utils.prepareLooper()
            currentScheduler.set(Looper.myLooper())
            executedBoolean.set(true)
            latch.countDown()
        })

        assertTrue(executedBoolean.get())

        latch.await()
        assertEquals(currentScheduler.get(), currentLooper)
    }

    @Test
    fun testIoScheduler_isOnDifferentThread() {
        val currentScheduler = AtomicReference<Boolean>()
        val latch = CountDownLatch(1)
        Utils.prepareLooper()
        val currentLooper = Looper.myLooper()
        Schedulers.io().execute({
            Utils.prepareLooper()
            currentScheduler.set(Looper.myLooper() == currentLooper)
            latch.countDown()
        })

        latch.await()
        assertFalse(currentScheduler.get())
    }

    @Test
    fun testWorkerScheduler_isOnDifferentThread() {
        val currentScheduler = AtomicReference<Boolean>()
        val latch = CountDownLatch(1)
        Utils.prepareLooper()
        val currentLooper = Looper.myLooper()
        Schedulers.worker().execute({
            Utils.prepareLooper()
            currentScheduler.set(Looper.myLooper() == currentLooper)
            latch.countDown()
        })

        latch.await()
        assertFalse(currentScheduler.get())
    }

    @Test
    fun testNewSingleThreadScheduler_isOnDifferentThread() {
        val currentScheduler = AtomicReference<Boolean>()
        val latch = CountDownLatch(1)
        Utils.prepareLooper()
        val currentLooper = Looper.myLooper()
        Schedulers.newSingleThreadedScheduler().execute({
            Utils.prepareLooper()
            currentScheduler.set(Looper.myLooper() == currentLooper)
            latch.countDown()
        })

        latch.await()
        assertFalse(currentScheduler.get())
    }

    @Test
    fun testFromHandler_isOnRightThread() {
        val executorThread = AtomicReference<String>(null)
        val schedulerThread = AtomicReference<String>(null)
        val latch = CountDownLatch(1)
        Executors.newSingleThreadExecutor()
                .execute {
                    Looper.prepare()
                    val handler = Handler(Looper.myLooper())
                    executorThread.set(Thread.currentThread().toString())
                    Schedulers.from(handler)
                            .execute({
                                schedulerThread.set(Thread.currentThread().toString())
                                latch.countDown()
                            })
                }

        latch.await()

        assertNotNull(schedulerThread.get())
        assertNotNull(executorThread.get())
        assertEquals(schedulerThread.get(), executorThread.get())
    }

    @Test
    fun testFromScheduler_isOnRightThread() {
        val executorAssertion = AtomicReference<Looper>()
        val schedulerAssertion = AtomicReference<Looper>()
        val latch = CountDownLatch(2)
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            Utils.prepareLooper()
            executorAssertion.set(Looper.myLooper())
            latch.countDown()
        }

        Schedulers.from(executor).execute({
            Utils.prepareLooper()
            schedulerAssertion.set(Looper.myLooper())
            latch.countDown()
        })

        latch.await()

        assertTrue(executorAssertion.get() == schedulerAssertion.get())
    }

}
