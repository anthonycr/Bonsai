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

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SchedulersUnitTest extends BaseUnitTest {

    @Test
    public void testIsFinalClass() throws Exception {
        Utils.testNonInstantiableClass(Schedulers.class);
    }

    @Test
    public void testMainScheduler_isCorrect() throws Exception {
        final AtomicReference<Boolean> mainScheduler = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        Schedulers.main().execute(new Runnable() {
            @Override
            public void run() {
                mainScheduler.set(Looper.myLooper() == Looper.getMainLooper());
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(mainScheduler.get());
    }

    @Test
    public void testImmediateScheduler_isSynchronous() throws Exception {
        final AtomicReference<Boolean> executedBoolean = new AtomicReference<>(false);
        final AtomicReference<Looper> currentScheduler = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        Utils.prepareLooper();
        final Looper currentLooper = Looper.myLooper();

        Schedulers.immediate().execute(new Runnable() {
            @Override
            public void run() {
                Utils.prepareLooper();
                currentScheduler.set(Looper.myLooper());
                executedBoolean.set(true);
                latch.countDown();
            }
        });

        assertTrue(executedBoolean.get());

        latch.await();
        assertEquals(currentScheduler.get(), currentLooper);
    }

    @Test
    public void testIoScheduler_isOnDifferentThread() throws Exception {
        final AtomicReference<Boolean> currentScheduler = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        Utils.prepareLooper();
        final Looper currentLooper = Looper.myLooper();
        Schedulers.io().execute(new Runnable() {
            @Override
            public void run() {
                Utils.prepareLooper();
                currentScheduler.set(Looper.myLooper() == currentLooper);
                latch.countDown();
            }
        });

        latch.await();
        assertFalse(currentScheduler.get());
    }

    @Test
    public void testWorkerScheduler_isOnDifferentThread() throws Exception {
        final AtomicReference<Boolean> currentScheduler = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        Utils.prepareLooper();
        final Looper currentLooper = Looper.myLooper();
        Schedulers.worker().execute(new Runnable() {
            @Override
            public void run() {
                Utils.prepareLooper();
                currentScheduler.set(Looper.myLooper() == currentLooper);
                latch.countDown();
            }
        });

        latch.await();
        assertFalse(currentScheduler.get());
    }

    @Test
    public void testNewSingleThreadScheduler_isOnDifferentThread() throws Exception {
        final AtomicReference<Boolean> currentScheduler = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        Utils.prepareLooper();
        final Looper currentLooper = Looper.myLooper();
        Schedulers.newSingleThreadedScheduler().execute(new Runnable() {
            @Override
            public void run() {
                Utils.prepareLooper();
                currentScheduler.set(Looper.myLooper() == currentLooper);
                latch.countDown();
            }
        });

        latch.await();
        assertFalse(currentScheduler.get());
    }

    @Test
    public void testFromScheduler_isOnRightThread() throws Exception {
        final AtomicReference<Looper> executorAssertion = new AtomicReference<>();
        final AtomicReference<Looper> schedulerAssertion = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(2);
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Utils.prepareLooper();
                executorAssertion.set(Looper.myLooper());
                latch.countDown();
            }
        });

        Schedulers.from(executor).execute(new Runnable() {
            @Override
            public void run() {
                Utils.prepareLooper();
                schedulerAssertion.set(Looper.myLooper());
                latch.countDown();
            }
        });

        latch.await();

        assertTrue(executorAssertion.get() == schedulerAssertion.get());
    }

}
