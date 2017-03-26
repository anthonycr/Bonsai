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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("WeakerAccess")
@WorkerThread
public final class Database extends SQLiteOpenHelper {

    private static final String TAG = Database.class.getSimpleName();

    private static final String DATABASE_NAME = "CONTACTS_DATABASE";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_CONTACTS = "TABLE_CONTACTS";
    private static final String KEY_ID = "KEY_ID";
    private static final String KEY_NAME = "KEY_NAME";
    private static final String KEY_NUMBER = "KEY_NUMBER";
    private static final String KEY_BIRTHDAY = "KEY_BIRTHDAY";

    @Nullable private static Database instance;

    @NonNull private final SQLiteDatabase database;

    @WorkerThread
    @NonNull
    public static synchronized Database getInstance() {
        if (instance == null) {
            instance = new Database(App.context());
        }
        return instance;
    }

    @WorkerThread
    private Database(@NonNull Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        // Getting the writable database can be an expensive operation
        database = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_HISTORY_TABLE = "CREATE TABLE " + TABLE_CONTACTS + '(' + KEY_ID
            + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT," + KEY_NUMBER + " INTEGER,"
            + KEY_BIRTHDAY + " INTEGER" + ')';
        db.execSQL(CREATE_HISTORY_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if it exists
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS);
        // Create table again
        onCreate(db);
    }

    /**
     * Adds a contact to the database synchronously.
     * Should not be run on the main thread.
     *
     * @param contact the contact to add, must not be null.
     */
    @WorkerThread
    public synchronized void addContact(@NonNull Contact contact) {
        long time = System.nanoTime();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, contact.getName());
        values.put(KEY_NUMBER, contact.getNumber());
        values.put(KEY_BIRTHDAY, contact.getBirthday());

        database.insert(TABLE_CONTACTS, null, values);
        Log.d(TAG, "Contact added in " + currentTimeDiff(time) + " milliseconds");
    }

    /**
     * Updates a contact in the database synchronously.
     * Should not be run on the main thread.
     *
     * @param contact the contact to update by id,
     *                must not be null.
     */
    @WorkerThread
    public synchronized void updateContact(@NonNull Contact contact) {
        long time = System.nanoTime();
        ContentValues values = new ContentValues();
        values.put(KEY_ID, contact.getId());
        values.put(KEY_NAME, contact.getName());
        values.put(KEY_NUMBER, contact.getNumber());
        values.put(KEY_BIRTHDAY, contact.getBirthday());

        database.update(TABLE_CONTACTS, values, KEY_ID + "= ?", new String[]{String.valueOf(contact.getId())});
        Log.d(TAG, "Contact updated in " + currentTimeDiff(time) + " milliseconds");
    }

    /**
     * Deletes a contact from the database synchronously.
     * Should not be run on the main thread.
     *
     * @param contact the contact to delete by id,
     *                must not be null.
     */
    @WorkerThread
    public synchronized void deleteContact(@NonNull Contact contact) {
        long time = System.nanoTime();
        database.delete(TABLE_CONTACTS, KEY_ID + "= ?", new String[]{String.valueOf(contact.getId())});
        Log.d(TAG, "Contact deleted in " + currentTimeDiff(time) + " milliseconds");
    }

    /**
     * Returns a cursor over all the contacts
     * in the database. This is a blocking operation
     * and should be consumed from a worker thread.
     *
     * @return a cursor over the entire data set.
     */
    @WorkerThread
    @NonNull
    public synchronized Cursor getAllContactsCursor() {
        long time = System.nanoTime();

        Cursor cursor = database.query(TABLE_CONTACTS, null, null, null, null, null, null);

        Log.d(TAG, "Retrieved all contacts from database in " + currentTimeDiff(time) + " milliseconds");

        return cursor;
    }

    /**
     * Parses a {@link Contact} from
     * a cursor.
     *
     * @param cursor the cursor to parse.
     * @return a valid {@link Contact} retrieved
     * from the cursor.
     */
    @WorkerThread
    @NonNull
    public static Contact getContactFromCursor(@NonNull Cursor cursor) {
        int id = cursor.getColumnIndex(KEY_ID);
        int name = cursor.getColumnIndex(KEY_NAME);
        int number = cursor.getColumnIndex(KEY_NUMBER);
        int birthday = cursor.getColumnIndex(KEY_BIRTHDAY);

        try {
            // Simulate complex data parsing by sleeping here
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
            // Ignoring the exception
        }

        Contact contact = new Contact(cursor.getString(name),
            cursor.getLong(number),
            cursor.getLong(birthday));
        contact.setId(cursor.getInt(id));

        return contact;
    }

    /**
     * Returns the difference between the current
     * time in nanoseconds and the parameter passed
     * to the method.
     *
     * @param nanos the starting time in nano seconds
     * @return the difference between {@link System#nanoTime()}
     * and the parameter passed to this method.
     */
    private static long currentTimeDiff(long nanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanos);
    }
}
