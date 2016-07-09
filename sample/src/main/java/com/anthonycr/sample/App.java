package com.anthonycr.sample;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Copyright 7/8/2016 Anthony Restaino
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class App extends Application {

    private static Context context;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        context = base;
    }

    /**
     * The application context.
     *
     * @return a non-null application context.
     */
    @NonNull
    public static Context context() {
        if (context == null) {
            throw new RuntimeException("App.context() called before application was initialized");
        }
        return context;
    }

}
