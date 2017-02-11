package com.anthonycr.bonsai;

import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by anthonycr on 2/8/17.
 */
public class Completable {

    private static final String TAG = Single.class.getSimpleName();

    @NonNull private final CompletableAction mAction;
    @NonNull private final Scheduler mDefaultThread;
    @Nullable private Scheduler mSubscriberThread;
    @Nullable private Scheduler mObserverThread;

    private Completable(@NonNull CompletableAction action) {
        mAction = action;
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        Looper looper = Looper.myLooper();
        Preconditions.checkNonNull(looper);
        mDefaultThread = new ThreadScheduler(looper);
    }

    /**
     * Static creator method that creates an Single from the
     * {@link CompletableAction} that is passed in as the parameter. Action
     * must not be null.
     *
     * @param action the Action to perform
     * @return a valid non-null Single.
     */
    @NonNull
    public static Completable create(@NonNull CompletableAction action) {
        Preconditions.checkNonNull(action);
        return new Completable(action);
    }

    /**
     * Static creator that creates an Single that is empty
     * and emits no items, but completes immediately.
     *
     * @return a valid non-null empty Single.
     */
    @NonNull
    public static Completable empty() {
        return new Completable(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                subscriber.onComplete();
            }
        });
    }

    /**
     * Tells the Single what Scheduler that the onSubscribe
     * work should run on.
     *
     * @param subscribeScheduler the Scheduler to run the work on.
     * @return returns this so that calls can be conveniently chained.
     */
    @NonNull
    public Completable subscribeOn(@NonNull Scheduler subscribeScheduler) {
        mSubscriberThread = subscribeScheduler;
        return this;
    }

    /**
     * Tells the Single what Scheduler the onSubscribe should observe
     * the work on.
     *
     * @param observerScheduler the Scheduler to run to callback on.
     * @return returns this so that calls can be conveniently chained.
     */
    @NonNull
    public Completable observeOn(@NonNull Scheduler observerScheduler) {
        mObserverThread = observerScheduler;
        return this;
    }

    /**
     * Subscribes immediately to the Single and ignores
     * all onComplete and onNext calls.
     */
    public void subscribe() {
        executeOnSubscriberThread(new Runnable() {
            @Override
            public void run() {
                mAction.onSubscribe(new Completable.SubscriberImpl(null, Completable.this));
            }
        });
    }

    /**
     * Immediately subscribes to the Single and starts
     * sending events from the Single to the {@link ObservableOnSubscribe}.
     *
     * @param onSubscribe the class that wishes to receive onNext and
     *                    onComplete callbacks from the Single.
     */
    @NonNull
    public Subscription subscribe(@NonNull CompletableOnSubscribe onSubscribe) {

        Preconditions.checkNonNull(onSubscribe);

        final CompletableSubscriber subscriber = new Completable.SubscriberImpl(onSubscribe, this);

        subscriber.onStart();

        executeOnSubscriberThread(new Runnable() {
            @Override
            public void run() {
                try {
                    mAction.onSubscribe(subscriber);
                } catch (Exception exception) {
                    subscriber.onError(exception);
                }
            }
        });

        return subscriber;
    }

    private void executeOnObserverThread(@NonNull Runnable runnable) {
        if (mObserverThread != null) {
            mObserverThread.execute(runnable);
        } else {
            mDefaultThread.execute(runnable);
        }
    }

    private void executeOnSubscriberThread(@NonNull Runnable runnable) {
        if (mSubscriberThread != null) {
            mSubscriberThread.execute(runnable);
        } else {
            mDefaultThread.execute(runnable);
        }
    }

    private static class SubscriberImpl implements CompletableSubscriber {

        @Nullable private volatile CompletableOnSubscribe mOnSubscribe;
        @NonNull private final Completable mCompletable;
        private boolean mOnCompleteExecuted = false;
        private boolean mOnError = false;

        SubscriberImpl(@Nullable CompletableOnSubscribe onSubscribe, @NonNull Completable completable) {
            mOnSubscribe = onSubscribe;
            mCompletable = completable;
        }

        @Override
        public void unsubscribe() {
            mOnSubscribe = null;
        }

        @Override
        public void onComplete() {
            CompletableOnSubscribe onSubscribe = mOnSubscribe;
            if (!mOnCompleteExecuted && onSubscribe != null && !mOnError) {
                mOnCompleteExecuted = true;
                mCompletable.executeOnObserverThread(new OnCompleteRunnable(onSubscribe));
            } else if (!mOnError && mOnCompleteExecuted) {
                throw new RuntimeException("onComplete called more than once");
            }
            unsubscribe();
        }

        @Override
        public void onStart() {
            CompletableOnSubscribe onSubscribe = mOnSubscribe;
            if (onSubscribe != null) {
                mCompletable.executeOnObserverThread(new OnStartRunnable(onSubscribe));
            }
        }

        @Override
        public void onError(@NonNull final Throwable throwable) {
            CompletableOnSubscribe onSubscribe = mOnSubscribe;
            if (onSubscribe != null) {
                mOnError = true;
                mCompletable.executeOnObserverThread(new OnErrorRunnable(onSubscribe, throwable));
            }
            unsubscribe();
        }

        @Override
        public boolean isUnsubscribed() {
            return mOnSubscribe == null;
        }
    }
}
