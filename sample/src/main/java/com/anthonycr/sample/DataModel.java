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

import android.database.Cursor;
import android.support.annotation.NonNull;

import com.anthonycr.bonsai.Completable;
import com.anthonycr.bonsai.Stream;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

@SuppressWarnings("WeakerAccess")
public final class DataModel {

    private DataModel() {}

    /**
     * An observable that retrieves all the contacts
     * stored in the database. This is a long operation
     * that should be consumed on a background
     * {@link com.anthonycr.bonsai.Scheduler}.
     *
     * @return an observable that will emit the items
     * in a list when you subscribe.
     */
    @NonNull
    public static Stream<Contact> allContactsStream() {
        return Stream.create(new Function1<Stream.Subscriber<? super Contact>, Unit>() {
            @Override
            public Unit invoke(Stream.Subscriber<? super Contact> subscriber) {
                Cursor contactsCursor = Database.getInstance().getAllContactsCursor();

                if (contactsCursor.moveToFirst()) {

                    do {
                        subscriber.onNext(Database.getContactFromCursor(contactsCursor));
                    } while (contactsCursor.moveToNext());

                    contactsCursor.close();
                }

                subscriber.onComplete();
                return null;
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
    public static Completable addContactCompletable(@NonNull final Contact contact) {
        return Completable.create(new Function1<Completable.Subscriber, Unit>() {
            @Override
            public Unit invoke(Completable.Subscriber subscriber) {
                Database.getInstance().addContact(contact);
                subscriber.onComplete();
                return null;
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
    public static Completable updateContactCompletable(@NonNull final Contact contact) {
        return Completable.create(new Function1<Completable.Subscriber, Unit>() {
            @Override
            public Unit invoke(Completable.Subscriber subscriber) {
                Database.getInstance().updateContact(contact);
                subscriber.onComplete();
                return null;
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
    public static Completable deleteContactCompletable(@NonNull final Contact contact) {
        return Completable.create(new Function1<Completable.Subscriber, Unit>() {
            @Override
            public Unit invoke(Completable.Subscriber subscriber) {
                Database.getInstance().deleteContact(contact);
                subscriber.onComplete();
                return null;
            }
        });
    }
}
