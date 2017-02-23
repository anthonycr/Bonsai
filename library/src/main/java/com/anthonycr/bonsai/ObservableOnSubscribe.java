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
package com.anthonycr.bonsai;

import android.support.annotation.NonNull;

/**
 * When a consumer subscribes to an {@link Observable}
 * it should supply an implementation of this class
 * with the desired methods overridden.
 * If {@link #onError(Throwable)} is not overridden,
 * it will throw an exception.
 */
@SuppressWarnings("WeakerAccess")
public abstract class ObservableOnSubscribe {

    /**
     * Called when the observable runs into an error
     * that will cause it to abort and not finish.
     * Receiving this callback means that the observable
     * is dead and the subscriber will be unsubscribed.
     * Default implementation throws an exception,
     * so you will get a crash if the observable throws
     * an exception and this method is not overridden.
     * <p>
     * Do not call super when you override this method.
     *
     * @param throwable an optional throwable that could
     *                  be sent.
     */
    public void onError(@NonNull Throwable throwable) {
        throw new RuntimeException("Exception thrown: override onError to handle it", throwable);
    }

    /**
     * Called before the observer begins
     * to process and emit items or complete.
     */
    public void onStart() {}

}
