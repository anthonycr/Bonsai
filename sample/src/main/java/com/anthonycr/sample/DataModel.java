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
package com.anthonycr.sample;

import android.support.annotation.NonNull;

import com.anthonycr.bonsai.Completable;
import com.anthonycr.bonsai.CompletableAction;
import com.anthonycr.bonsai.CompletableSubscriber;
import com.anthonycr.bonsai.ObservableAction;
import com.anthonycr.bonsai.Observable;
import com.anthonycr.bonsai.ObservableSubscriber;
import com.anthonycr.bonsai.Single;
import com.anthonycr.bonsai.SingleAction;
import com.anthonycr.bonsai.SingleSubscriber;

import java.util.List;
import java.util.concurrent.TimeUnit;

public final class DataModel {

    private DataModel() {

    }

    /**
     * An observable that retrieves all the contacts
     * stored in the database. This is a long operation
     * that should be consumed on a background
     * {@link com.anthonycr.bonsai.Scheduler}.
     *
     * @return an observable that will return a list
     * when you subscribe to it. Only one list will
     * be emitted.
     */
    @NonNull
    public static Single<List<Contact>> allContactsObservable() {
        return Single.create(new SingleAction<List<Contact>>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<List<Contact>> subscriber) {
                long currentTime = System.nanoTime();

                // Get all the contacts from the database.
                // This will be fast in practice, but because it
                // is slow, it should be done on a background thread.
                List<Contact> allContacts = Database.getInstance().getAllContacts();

                long diffMillis = (System.nanoTime() - currentTime) / TimeUnit.MILLISECONDS.toNanos(1);

                if (diffMillis > 0) {
                    try {
                        // Simulate the effect of a long disk operation or
                        // network request that takes at least 1 second.
                        Thread.sleep(TimeUnit.SECONDS.toMillis(1) - diffMillis);
                    } catch (InterruptedException e) {
                        subscriber.onError(e);
                    }
                }

                subscriber.onItem(allContacts);
                subscriber.onComplete();
            }
        });
    }

    /**
     * An observable that adds a contact to the database.
     *
     * @param contact the contact to add.
     * @return an observable that will return nothing,
     * the only event used by this observable is onComplete
     * which will notify you when the observable finishes completing.
     */
    @NonNull
    public static Completable addContactObservable(@NonNull final Contact contact) {
        return Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                Database.getInstance().addContact(contact);
                subscriber.onComplete();
            }
        });
    }

    /**
     * An observable that updates the contact in the database.
     *
     * @param contact the contact to update.
     * @return an observable that will return nothing,
     * the only event used by this observable is onComplete
     * which will notify you when the observable finishes completing.
     */
    @NonNull
    public static Completable updateContactObservable(@NonNull final Contact contact) {
        return Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                Database.getInstance().updateContact(contact);
                subscriber.onComplete();
            }
        });
    }

    /**
     * An observable that deletes a contact from the database.
     *
     * @param contact the contact to delete.
     * @return an observable that will return nothing,
     * the only event used by this observable is onComplete
     * which will notify you when the observable finishes completing.
     */
    @NonNull
    public static Completable deleteContactObservable(@NonNull final Contact contact) {
        return Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                Database.getInstance().deleteContact(contact);
                subscriber.onComplete();
            }
        });
    }
}
