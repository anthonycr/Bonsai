package com.anthonycr.bonsai;

import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by anthonycr on 2/8/17.
 */
public class Single<T> {

    private static final String TAG = Single.class.getSimpleName();

    @NonNull private final SingleAction<T> mAction;
    @NonNull private final Scheduler mDefaultThread;
    @Nullable private Scheduler mSubscriberThread;
    @Nullable private Scheduler mObserverThread;

    private Single(@NonNull SingleAction<T> action) {
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
     * @param <T>    the type that will be emitted to the onSubscribe
     * @return a valid non-null Single.
     */
    @NonNull
    public static <T> Single<T> create(@NonNull SingleAction<T> action) {
        Preconditions.checkNonNull(action);
        return new Single<>(action);
    }

    /**
     * Static creator that creates an Single that is empty
     * and emits no items, but completes immediately.
     *
     * @param <T> the type that will be emitted to the onSubscribe
     * @return a valid non-null empty Single.
     */
    @NonNull
    public static <T> Single<T> empty() {
        return new Single<>(new SingleAction<T>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<T> subscriber) {
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
    public Single<T> subscribeOn(@NonNull Scheduler subscribeScheduler) {
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
    public Single<T> observeOn(@NonNull Scheduler observerScheduler) {
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
                mAction.onSubscribe(new Single.SubscriberImpl<>(null, Single.this));
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
    public Subscription subscribe(@NonNull SingleOnSubscribe<T> onSubscribe) {

        Preconditions.checkNonNull(onSubscribe);

        final SingleSubscriber<T> subscriber = new Single.SubscriberImpl<>(onSubscribe, this);

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

    private static class SubscriberImpl<T> implements SingleSubscriber<T> {

        @Nullable private volatile SingleOnSubscribe<T> mOnSubscribe;
        @NonNull private final Single<T> mSingle;
        private boolean mOnCompleteExecuted = false;
        private boolean mOnOnlyExecuted = false;
        private boolean mOnError = false;

        SubscriberImpl(@Nullable SingleOnSubscribe<T> onSubscribe, @NonNull Single<T> Single) {
            mOnSubscribe = onSubscribe;
            mSingle = Single;
        }

        @Override
        public void unsubscribe() {
            mOnSubscribe = null;
        }

        @Override
        public void onComplete() {
            SingleOnSubscribe<T> onSubscribe = mOnSubscribe;
            if (!mOnCompleteExecuted && onSubscribe != null && !mOnError) {
                mOnCompleteExecuted = true;
                mSingle.executeOnObserverThread(new OnCompleteRunnable(onSubscribe));
            } else if (!mOnError && mOnCompleteExecuted) {
                Log.e(TAG, "onComplete called more than once");
                throw new RuntimeException("onComplete called more than once");
            }
            unsubscribe();
        }

        @Override
        public void onStart() {
            SingleOnSubscribe<T> onSubscribe = mOnSubscribe;
            if (onSubscribe != null) {
                mSingle.executeOnObserverThread(new OnStartRunnable(onSubscribe));
            }
        }

        @Override
        public void onError(@NonNull final Throwable throwable) {
            SingleOnSubscribe<T> onSubscribe = mOnSubscribe;
            if (onSubscribe != null) {
                mOnError = true;
                mSingle.executeOnObserverThread(new OnErrorRunnable(onSubscribe, throwable));
            }
            unsubscribe();
        }

        @Override
        public void onItem(@Nullable T item) {
            SingleOnSubscribe<T> onSubscribe = mOnSubscribe;
            if (!mOnCompleteExecuted && !mOnOnlyExecuted && onSubscribe != null && !mOnError) {
                mOnOnlyExecuted = true;
                mSingle.executeOnObserverThread(new OnItemRunnable<>(onSubscribe, item));
            } else if (mOnCompleteExecuted) {
                Log.e(TAG, "onComplete has been already called, onItem should not be called");
                throw new RuntimeException("onItem should not be called after onComplete has been called");
            } else if (mOnOnlyExecuted) {
                Log.e(TAG, "onItem has been already called, onItem should not be called multiple times");
                throw new RuntimeException("onItem should not be called multiple times");
            } else {
                // Subscription has been unsubscribed, ignore it
            }
        }

        @Override
        public boolean isUnsubscribed() {
            return mOnSubscribe == null;
        }
    }
}
