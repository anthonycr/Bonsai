package com.anthonycr.sample;

import android.support.annotation.NonNull;

import com.anthonycr.bonsai.Action;
import com.anthonycr.bonsai.Observable;
import com.anthonycr.bonsai.Subscriber;

import java.util.List;

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
public final class DataModel {

    private DataModel() {

    }

    /**
     * An observable that retrieves all the contacts
     * stored in the database.
     *
     * @return an observable that will return a list
     * when you subscribe to it. Only one list will
     * be emitted.
     */
    @NonNull
    public static Observable<List<Contact>> allContactsObservable() {
        return Observable.create(new Action<List<Contact>>() {
            @Override
            public void onSubscribe(@NonNull Subscriber<List<Contact>> subscriber) {
                subscriber.onNext(Database.getInstance().getAllContacts());
                subscriber.onComplete();
            }
        });
    }

    /**
     * An observable that adds a contact to the database.
     *
     * @param contact the contact to add.
     * @return an obseravable that will return nothing,
     * the only event used by this obserable is onComplete
     * which will notify you when the observable finishes completing.
     */
    @NonNull
    public static Observable<Void> addContactObservable(final Contact contact) {
        return Observable.create(new Action<Void>() {
            @Override
            public void onSubscribe(@NonNull Subscriber<Void> subscriber) {
                Database.getInstance().addContact(contact);
                subscriber.onComplete();
            }
        });
    }

    /**
     * An observable that updates the contact in the database.
     *
     * @param contact the contact to update.
     * @return an obseravable that will return nothing,
     * the only event used by this obserable is onComplete
     * which will notify you when the observable finishes completing.
     */
    @NonNull
    public static Observable<Void> updateContactObservable(final Contact contact) {
        return Observable.create(new Action<Void>() {
            @Override
            public void onSubscribe(@NonNull Subscriber<Void> subscriber) {
                Database.getInstance().updateContact(contact);
                subscriber.onComplete();
            }
        });
    }

    /**
     * An observable that deletes a contact from the database.
     *
     * @param contact the contact to delete.
     * @return an obseravable that will return nothing,
     * the only event used by this obserable is onComplete
     * which will notify you when the observable finishes completing.
     */
    @NonNull
    public static Observable<Void> deleteContactObservable(final Contact contact) {
        return Observable.create(new Action<Void>() {
            @Override
            public void onSubscribe(@NonNull Subscriber<Void> subscriber) {
                Database.getInstance().deleteContact(contact);
                subscriber.onComplete();
            }
        });
    }
}
