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

import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * A class of default [Scheduler] provided
 * for use with [Stream], [Single],
 * and [Completable].
 *
 *
 * If the options available here are not sufficient,
 * implement [Scheduler] and create your own.
 */
object Schedulers {

    /**
     * A worker scheduler. Backed by a fixed
     * thread pool containing 4 thread.
     */
    private class WorkerScheduler : Scheduler {

        private val worker = Executors.newFixedThreadPool(4)

        override fun execute(runnable: () -> Unit) {
            worker.execute(runnable)
        }

    }

    /**
     * A single threaded scheduler. Backed by a
     * single thread in an executor.
     */
    private class SingleThreadedScheduler : Scheduler {

        private val singleThreadExecutor = Executors.newSingleThreadExecutor()

        override fun execute(runnable: () -> Unit) {
            singleThreadExecutor.execute(runnable)
        }

    }

    /**
     * A scheduler backed by an executor.
     */
    private class ExecutorScheduler(private val backingExecutor: Executor) : Scheduler {

        override fun execute(runnable: () -> Unit) {
            backingExecutor.execute(runnable)
        }

    }

    /**
     * A scheduler that executes tasks immediately.
     */
    private class ImmediateScheduler : Scheduler {

        override fun execute(runnable: () -> Unit) {
            runnable()
        }

    }

    private val mainScheduler: Scheduler by lazy { ThreadScheduler(Looper.getMainLooper()) }
    private val workerScheduler: Scheduler by lazy { WorkerScheduler() }
    private val ioScheduler: Scheduler by lazy { SingleThreadedScheduler() }
    private val immediateScheduler: Scheduler by lazy { ImmediateScheduler() }

    /**
     * Creates a scheduler from an executor instance.
     *
     * @param executor the executor to use to create
     * the Scheduler.
     * @return a valid Scheduler backed by an executor.
     */
    @JvmStatic
    fun from(executor: Executor): Scheduler = ExecutorScheduler(executor)

    /**
     * Creates a scheduler from a handler. The scheduler
     * will post tasks to the looper associated with the
     * handler.
     *
     * @param handler the handler used to create the Scheduler.
     * @return a valid Scheduler backed by a handler.
     */
    @JvmStatic
    fun from(handler: Handler): Scheduler = ThreadScheduler(handler.looper)

    /**
     * A scheduler that executes tasks synchronously
     * on the calling thread. If you use this scheduler
     * as the subscribe-on scheduler, then the work will
     * execute synchronously on the current thread. If
     * you use this scheduler as the observe-on thread,
     * then you will receive events on whatever thread
     * was used as that subscribe-on thread.
     *
     * @return a synchronous scheduler.
     */
    @JvmStatic
    fun immediate(): Scheduler = immediateScheduler

    /**
     * Creates a new Scheduler that
     * creates a new thread and does
     * all work on it.
     *
     * @return a scheduler associated
     * with a new single thread.
     */
    @JvmStatic
    fun newSingleThreadedScheduler(): Scheduler = SingleThreadedScheduler()

    /**
     * The worker thread Scheduler, will
     * execute work on any one of multiple
     * threads.
     *
     * @return a non-null Scheduler.
     */
    @JvmStatic
    fun worker(): Scheduler = workerScheduler

    /**
     * The main thread. All work will
     * be done on the single main thread.
     *
     * @return a non-null Scheduler that does work on the main thread.
     */
    @JvmStatic
    fun main(): Scheduler = mainScheduler

    /**
     * The io scheduler. All work will be
     * done on a single thread.
     *
     * @return a non-null Scheduler that does
     * work on a single thread off the main thread.
     */
    @JvmStatic
    fun io(): Scheduler = ioScheduler
}
