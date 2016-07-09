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

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@SuppressWarnings("unused")
public final class Schedulers {

    private Schedulers() {
        throw new UnsupportedOperationException("This class is not instantiable");
    }

    @Nullable private static Scheduler sMainScheduler;
    @Nullable private static Scheduler sWorkerScheduler;
    @Nullable private static Scheduler sIoScheduler;

    /**
     * A worker scheduler. Backed by a fixed
     * thread pool containing 4 thread.
     */
    private static class WorkerScheduler implements Scheduler {

        private final Executor mWorker = Executors.newFixedThreadPool(4);

        @Override
        public void execute(@NonNull Runnable command) {
            mWorker.execute(command);
        }
    }

    /**
     * A single threaded scheduler. Backed by a
     * single thread in an executor.
     */
    private static class SingleThreadedScheduler implements Scheduler {

        private final Executor mSingleThreadExecutor = Executors.newSingleThreadExecutor();

        @Override
        public void execute(@NonNull Runnable command) {
            mSingleThreadExecutor.execute(command);
        }
    }

    /**
     * A scheduler backed by an executor.
     */
    private static class ExecutorScheduler implements Scheduler {

        @NonNull
        private final Executor mBackingExecutor;

        public ExecutorScheduler(@NonNull Executor executor) {
            mBackingExecutor = executor;
        }

        @Override
        public void execute(@NonNull Runnable command) {
            mBackingExecutor.execute(command);
        }
    }

    /**
     * Creates a scheduler from an executor instance.
     *
     * @param executor the executor to use to create
     *                 the Scheduler.
     * @return a valid Scheduler backed by an executor.
     */
    @NonNull
    public static Scheduler from(@NonNull Executor executor) {
        return new ExecutorScheduler(executor);
    }

    /**
     * A scheduler that points to the
     * current thread. Useful when you
     * are not on the main thread and
     * need to observe on that thread.
     *
     * @return a scheduler associated with
     * the current thread.
     */
    @NonNull
    public static Scheduler current() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        //noinspection ConstantConditions
        return new ThreadScheduler(Looper.myLooper());
    }

    /**
     * Creates a new Scheduler that
     * creates a new thread and does
     * all work on it.
     *
     * @return a scheduler associated
     * with a new single thread.
     */
    @NonNull
    public static Scheduler newSingleThreadedScheduler() {
        return new SingleThreadedScheduler();
    }

    /**
     * The worker thread Scheduler, will
     * execute work on any one of multiple
     * threads.
     *
     * @return a non-null Scheduler.
     */
    @NonNull
    public static Scheduler worker() {
        if (sWorkerScheduler == null) {
            sWorkerScheduler = new WorkerScheduler();
        }
        return sWorkerScheduler;
    }

    /**
     * The main thread. All work will
     * be done on the single main thread.
     *
     * @return a non-null Scheduler that does work on the main thread.
     */
    @NonNull
    public static Scheduler main() {
        if (sMainScheduler == null) {
            sMainScheduler = new ThreadScheduler(Looper.getMainLooper());
        }
        return sMainScheduler;
    }

    /**
     * The io scheduler. All work will be
     * done on a single thread.
     *
     * @return a non-null Scheduler that does
     * work on a single thread off the main thread.
     */
    @NonNull
    public static Scheduler io() {
        if (sIoScheduler == null) {
            sIoScheduler = new SingleThreadedScheduler();
        }
        return sIoScheduler;
    }
}
