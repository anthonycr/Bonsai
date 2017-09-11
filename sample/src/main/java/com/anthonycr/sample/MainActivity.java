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

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.anthonycr.bonsai.Schedulers;
import com.anthonycr.bonsai.Subscription;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

public class MainActivity extends AppCompatActivity {

    private interface DialogCallback {
        /**
         * Called when the positive button on the
         * dialog is pressed.
         *
         * @param contact the contact contained by
         *                the dialog.
         */
        void onPositiveClicked(@NonNull Contact contact);

        /**
         * Called when the negative button on the
         * dialog is pressed.
         *
         * @param contact the contact contained by
         *                the dialog.
         */
        void onNegativeClicked(@NonNull Contact contact);
    }

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

    // list view data adapter
    private RecyclerAdapter contactsAdapter;

    private RecyclerView listView;
    private ProgressBar progressBar;

    // data model subscriptions
    @Nullable private Subscription getAllContactsSubscription;
    @Nullable private Subscription addContactSubscription;
    @Nullable private Subscription editContactSubscription;
    @Nullable private Subscription deleteContactSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (RecyclerView) findViewById(R.id.list_view);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        showLoadingSpinner();

        listView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        contactsAdapter = new RecyclerAdapter(this);
        contactsAdapter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = listView.getChildAdapterPosition(v);
                Contact clickedContact = contactsAdapter.getItem(index);
                if (clickedContact != null) {
                    contactClicked(clickedContact);
                }
            }
        });

        listView.setAdapter(contactsAdapter);

        // Loads all the initial data from the database on a separate thread
        // then notifies the main thread after the data is loaded. Then we
        // add all the items we received to the adapter and they get displayed.
        contactsAdapter.clearItems();
        showLoadingSpinner();
        getAllContactsSubscription = DataModel.allContactsStream()
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.main())
            .subscribe(new Function1<Contact, Unit>() {
                @Override
                public Unit invoke(Contact contact) {
                    contactsAdapter.addItem(contact);

                    showListView();
                    return null;
                }
            }, new Function0<Unit>() {
                @Override
                public Unit invoke() {
                    showListView();
                    return null;
                }
            }, new Function1<Throwable, Unit>() {
                @Override
                public Unit invoke(Throwable throwable) {
                    return null;
                }
            });
    }

    private void showListView() {
        if (listView.getVisibility() != View.VISIBLE || progressBar.getVisibility() != View.GONE) {
            listView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
    }

    private void showLoadingSpinner() {
        if (listView.getVisibility() != View.INVISIBLE || progressBar.getVisibility() != View.VISIBLE) {
            listView.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Set up the options menu
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Set up the options menu
        if (item.getItemId() == R.id.add_item) {
            // When the user clicks to add a contact
            // call the addContactClicked() method.
            addContactClicked();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Here we unsubscribe from the observables
        // that all these Subscriptions were subscribed
        // to. The purpose for this is to prevent a
        // memory leak. In reality, these database operations
        // are quite fast, and will never leak the activity,
        // but if the observables were something longer lived,
        // or even infinite tasks (always listening for data),
        // then you need to explicitly unsubscribe so that you
        // don't get leaked.
        unsubscribeIfNecessary(getAllContactsSubscription);
        unsubscribeIfNecessary(deleteContactSubscription);
        unsubscribeIfNecessary(addContactSubscription);
        unsubscribeIfNecessary(editContactSubscription);
    }

    private static void unsubscribeIfNecessary(@Nullable Subscription subscription) {
        if (subscription != null) {
            subscription.unsubscribe();
        }
    }

    /**
     * Handles when the user presses the add
     * contact menu option item. Launches the
     * add contact dialog.
     */
    private void addContactClicked() {
        Contact newContact = new Contact();
        showContactDialog(newContact, new DialogCallback() {
            @Override
            public void onPositiveClicked(@NonNull final Contact contact) {
                if (contact.getName().isEmpty()) {
                    Toast.makeText(MainActivity.this, R.string.message_blank_name, Toast.LENGTH_LONG).show();
                } else {

                    // When we click add contact and the user clicks okay,
                    // if the name is not empty, we add the new contact to
                    // the database on the background thread, then receive
                    // notification when it has finished inserting into the
                    // database.
                    addContactSubscription = DataModel.addContactCompletable(contact)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.main())
                        .subscribe(new Function0<Unit>() {
                            @Override
                            public Unit invoke() {
                                contactsAdapter.addItem(contact);
                                return null;
                            }
                        }, new Function1<Throwable, Unit>() {
                            @Override
                            public Unit invoke(Throwable throwable) {
                                return null;
                            }
                        });
                }
            }

            @Override
            public void onNegativeClicked(@NonNull Contact contact) {
                // Do nothing if cancel was clicked because
                // the user has indicated they don't want to
                // enter this into the contacts database.
            }
        }, R.string.cancel, R.string.add_contact);
    }

    /**
     * Handles when a contact was clicked. Launches
     * the edit contact dialog.
     *
     * @param contact the contact that was clicked
     */
    private void contactClicked(@NonNull Contact contact) {

        showContactDialog(contact, new DialogCallback() {
            @Override
            public void onPositiveClicked(@NonNull final Contact contact) {

                // When the user clicks okay, we update the database
                // with the new values for the contact. The update is
                // done on a background thread, and then calls back
                // onto the main thread, where we update the adapter.
                editContactSubscription = DataModel.updateContactCompletable(contact)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.main())
                    .subscribe(new Function0<Unit>() {
                        @Override
                        public Unit invoke() {
                            contactsAdapter.changedItem(contact);
                            return null;
                        }
                    }, new Function1<Throwable, Unit>() {
                        @Override
                        public Unit invoke(Throwable throwable) {
                            return null;
                        }
                    });
            }

            @Override
            public void onNegativeClicked(@NonNull final Contact contact) {

                // When the user clicks delete, we asynchronously
                // delete the item from the database. When the operation
                // completes, we call back to the main thread to update
                // the UI and remove the item from the list.
                deleteContactSubscription = DataModel.deleteContactCompletable(contact)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.main())
                    .subscribe(new Function0<Unit>() {
                        @Override
                        public Unit invoke() {
                            contactsAdapter.removeItem(contact);
                            return null;
                        }
                    }, new Function1<Throwable, Unit>() {
                        @Override
                        public Unit invoke(Throwable throwable) {
                            return null;
                        }
                    });
            }
        }, R.string.delete, R.string.edit_contact);
    }

    /**
     * Method for showing the edit/add contact dialog
     * and receiving a callback based on the users action.
     *
     * @param contact      the contact to edit.
     * @param callback     the callback to receive when the user
     *                     selects ok or cancel.
     * @param cancelButton the string resource for the cancel button
     * @param headerTitle  the title of the dialog.
     */
    private void showContactDialog(@NonNull final Contact contact,
                                   @NonNull final DialogCallback callback,
                                   @StringRes int cancelButton,
                                   @StringRes int headerTitle) {
        View view = LayoutInflater.from(this).inflate(R.layout.contact_dialog, null);
        EditText nameText = (EditText) view.findViewById(R.id.name_edit_text);
        EditText numberText = (EditText) view.findViewById(R.id.number_edit_text);
        final Button dateSpinner = (Button) view.findViewById(R.id.date_picker);

        nameText.setText(contact.getName());
        if (contact.getNumber() != -1) {
            numberText.setText(String.valueOf(contact.getNumber()));
        }
        final Calendar oldCalendar = Calendar.getInstance();
        oldCalendar.setTime(new Date(contact.getBirthday()));
        dateSpinner.setText(dateFormat.format(new Date(contact.getBirthday())));

        // Update the contact and the spinner when the user changes the date
        dateSpinner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

                        Calendar newCalendar = Calendar.getInstance();
                        newCalendar.set(year, monthOfYear, dayOfMonth);
                        contact.setBirthday(newCalendar.getTime().getTime());
                        dateSpinner.setText(dateFormat.format(newCalendar.getTime()));

                    }
                }, oldCalendar.get(Calendar.YEAR),
                    oldCalendar.get(Calendar.MONTH),
                    oldCalendar.get(Calendar.DAY_OF_MONTH))
                    .show();
            }
        });

        // Update the contact when the name is changed
        nameText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                contact.setName(String.valueOf(s));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Update the contact when the number is changed
        numberText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    contact.setNumber(Long.valueOf(String.valueOf(s)));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Build the dialog with the custom view.
        // Add callbacks for positive and negative buttons.
        new AlertDialog.Builder(this)
            .setView(view)
            .setTitle(headerTitle)
            .setNegativeButton(cancelButton, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    callback.onNegativeClicked(contact);
                }
            })
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    callback.onPositiveClicked(contact);
                }
            }).show();
    }

    /**
     * The view holder class for the Contacts list.
     */
    private static class AdapterView extends RecyclerView.ViewHolder {

        @NonNull private final TextView textView;

        AdapterView(@NonNull View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(R.id.contact_name);
        }

        void setText(@Nullable String text) {
            textView.setText(text);
        }
    }

    /**
     * The adapter class for the Contacts list.
     */
    private static class RecyclerAdapter extends RecyclerView.Adapter implements View.OnClickListener {

        @NonNull private final Context context;
        @NonNull private final List<Contact> contactList = new ArrayList<>();
        @Nullable private View.OnClickListener onClickListener;

        RecyclerAdapter(@NonNull Context context) {
            this.context = context;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.contact_layout, parent, false);
            view.setOnClickListener(this);
            return new AdapterView(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            AdapterView adapterView = (AdapterView) holder;

            Contact contact = contactList.get(position);
            adapterView.setText(contact != null ? contact.getName() : null);
        }

        @Override
        public synchronized int getItemCount() {
            return contactList.size();
        }

        @Override
        public void onClick(View v) {
            if (onClickListener != null) {
                onClickListener.onClick(v);
            }
        }

        void setOnClickListener(@Nullable View.OnClickListener onClickListener) {
            this.onClickListener = onClickListener;
        }

        @Nullable
        private synchronized Contact getItem(int index) {
            if (index < 0 || contactList.size() <= index) {
                return null;
            }
            return contactList.get(index);
        }

        private synchronized void addItem(@NonNull Contact contact) {
            contactList.add(contact);
            notifyItemInserted(getItemCount() - 1);
        }

        private synchronized void removeItem(@NonNull Contact contact) {
            int index = contactList.indexOf(contact);
            contactList.remove(contact);
            notifyItemRemoved(index);
        }

        private synchronized void changedItem(@NonNull Contact contact) {
            int index = contactList.indexOf(contact);
            notifyItemChanged(index);
        }

        private synchronized void clearItems() {
            contactList.clear();
            notifyDataSetChanged();
        }
    }
}
