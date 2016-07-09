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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Modifier;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class OnSubscribeUnitTest {

    @Test
    public void testClassType() throws Exception {
        // should be an abstract class
        Assert.assertTrue(Modifier.isAbstract(OnSubscribe.class.getModifiers()));
    }

    @Test
    public void allMethodsExecute() throws Exception {
        TestSubscriberImpl onSubscribe = new TestSubscriberImpl();

        onSubscribe.onStart();
        onSubscribe.onNext("Test");
        onSubscribe.onComplete();
        onSubscribe.onError(new NullPointerException("test exception"));

        Assert.assertTrue(onSubscribe.onStart);
        Assert.assertTrue(onSubscribe.onNext);
        Assert.assertTrue(onSubscribe.onComplete);
        Assert.assertTrue(onSubscribe.onError);
    }

    private static class TestSubscriberImpl extends OnSubscribe<String> {

        public boolean onStart = false;
        public boolean onNext = false;
        public boolean onError = false;
        public boolean onComplete = false;


        @Override
        public void onStart() {
            super.onStart();
            onStart = true;
        }

        @Override
        public void onError(@NonNull Throwable throwable) {
            super.onError(throwable);
            onError = true;
        }

        @Override
        public void onNext(@Nullable String item) {
            super.onNext(item);
            onNext = true;
        }

        @Override
        public void onComplete() {
            super.onComplete();
            onComplete = true;
        }
    }

}
