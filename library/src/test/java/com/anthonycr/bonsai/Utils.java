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

import junit.framework.Assert;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

public class Utils {

    private static final String DEFAULT_LOG_TAG = "BonsaiUnitTest";

    @SuppressWarnings("TryWithIdenticalCatches")
    public static <T> void testNonInstantiableClass(Class<T> clazz) throws Exception {
        Constructor<T> constructor;
        boolean instantiable = true;
        try {
            Assert.assertTrue("Class should be final", Modifier.isFinal(clazz.getModifiers()));
            constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            Assert.assertTrue("Constructor should be private", Modifier.isPrivate(constructor.getModifiers()));
            constructor.newInstance();
        } catch (NoSuchMethodException e) {
            instantiable = false;
        } catch (IllegalAccessException e) {
            instantiable = false;
        } catch (InstantiationException e) {
            instantiable = false;
        } catch (InvocationTargetException e) {
            instantiable = false;
        } catch (UnsupportedOperationException e) {
            instantiable = false;
        }
        Assert.assertFalse("Class should not be instantiable", instantiable);
    }

    public static void prepareLooper() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
    }

    public static void log(@NonNull CharSequence tag, @NonNull CharSequence message) {
        System.out.println(tag + ": " + message);
    }

    public static void log(@NonNull CharSequence message) {
        log(DEFAULT_LOG_TAG, message);
    }

}
