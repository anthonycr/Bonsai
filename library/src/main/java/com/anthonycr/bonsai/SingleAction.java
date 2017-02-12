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
 * Created by anthonycr on 2/8/17.
 */
public interface SingleAction<T> {

    /**
     * Should be overridden to send the subscriber
     * events such as {@link SingleSubscriber#onItem(Object)}
     * or {@link SingleSubscriber#onComplete()}.
     *
     * @param subscriber the subscriber that is sent in
     *                   when the user of the Observable
     *                   subscribes.
     */
    void onSubscribe(@NonNull SingleSubscriber<T> subscriber);
}
