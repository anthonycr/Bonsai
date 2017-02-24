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

import android.support.annotation.Nullable;

/**
 * When a consumer subscribes to a {@link SingleOnSubscribe}
 * it should supply an implementation of this class
 * with the desired methods overridden.
 * If {@link #onError(Throwable)} is not overridden,
 * it will throw an exception.
 *
 * @param <T> the type that will be emitted by the action.
 */
@SuppressWarnings("WeakerAccess")
public abstract class SingleOnSubscribe<T> extends CompletableOnSubscribe {

    /**
     * Called when the Observer emits an
     * item. It can be called one time.
     * It cannot be called after onComplete
     * has been called.
     *
     * @param item the item that has been emitted,
     *             can be null.
     */
    public void onItem(@Nullable T item) {}

}
