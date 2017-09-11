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

/**
 * A subscription to an originating
 * observable that can be unsubscribed.
 */
interface Subscription {

    /**
     * Calling this method unsubscribes a subscription
     * from the originating observable. Once this method
     * is called, no more calls to the OnSubscribe
     * callbacks will be made.
     */
    fun unsubscribe()

    /**
     * This method tells the caller whether or not
     * the subscriber to this observable has unsubscribed
     * or not. Useful for long running or never ending
     * operations that would otherwise needlessly use
     * resources.
     *
     * @return true if the the Subscriber has unsubscribed,
     * false otherwise.
     */
    fun isUnsubscribed(): Boolean

}
