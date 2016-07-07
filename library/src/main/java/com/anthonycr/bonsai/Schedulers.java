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

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@SuppressWarnings("unused")
public final class Schedulers {

    private Schedulers() {
        throw new UnsupportedOperationException("This class is not instantiable");
    }

    private static final Scheduler sMainScheduler = new ThreadScheduler(Looper.getMainLooper());
    private static final Scheduler sWorkerScheduler = new WorkerScheduler();
    private static final Scheduler sIoScheduler = new SingleThreadedScheduler();

    private static class WorkerScheduler implements Scheduler {

        private static final Executor sWorker = Executors.newFixedThreadPool(4);

        @Override
        public void execute(@NonNull Runnable command) {
            sWorker.execute(command);
        }
    }

    private static class SingleThreadedScheduler implements Scheduler {

        private final Executor mSingleThreadExecutor = Executors.newSingleThreadExecutor();

        @Override
        public void execute(@NonNull Runnable command) {
            mSingleThreadExecutor.execute(command);
        }
    }

    private static class ExecutorScheduler implements Scheduler {

        private final Executor mBackingExecutor;

        public ExecutorScheduler(Executor executor) {
            mBackingExecutor = executor;
        }

        @Override
        public void execute(@NonNull Runnable command) {
            mBackingExecutor.execute(command);
        }
    }

    @NonNull
    public static Scheduler from(@NonNull Executor executor) {
        return new ExecutorScheduler(executor);
    }

    public static Scheduler current() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        //noinspection ConstantConditions
        return new ThreadScheduler(Looper.myLooper());
    }

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
        return sWorkerScheduler;
    }

    /**
     * The main thread.
     *
     * @return a non-null Scheduler that does work on the main thread.
     */
    @NonNull
    public static Scheduler main() {
        return sMainScheduler;
    }

    /**
     * The io thread.
     *
     * @return a non-null Scheduler that does
     * work on a single thread off the main thread.
     */
    @NonNull
    public static Scheduler io() {
        return sIoScheduler;
    }
}
