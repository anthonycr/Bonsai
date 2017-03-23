package com.anthonycr.bonsai;

import android.support.annotation.NonNull;

import junit.framework.Assert;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by anthonycr on 2/19/17.
 */
public class CompletableSubscriberWrapperTest extends BaseUnitTest {

    @Test
    public void onCompleteTest_Succeeds() throws Exception {
        final AtomicReference<Boolean> onCompleteCalled = new AtomicReference<>(false);
        final AtomicReference<Boolean> onStartCalled = new AtomicReference<>(false);
        final AtomicReference<Boolean> onErrorCalled = new AtomicReference<>(false);
        CompletableOnSubscribe onSubscribe = new CompletableOnSubscribe() {
            @Override
            public void onError(@NonNull Throwable throwable) {
                onErrorCalled.set(true);
            }

            @Override
            public void onStart() {
                onStartCalled.set(true);
            }

            @Override
            public void onComplete() {
                onCompleteCalled.set(true);
            }
        };
        CompletableSubscriberWrapper<CompletableOnSubscribe> wrapper = new CompletableSubscriberWrapper<>(onSubscribe, null, Schedulers.current());
        wrapper.onComplete();

        Assert.assertTrue(onCompleteCalled.get());
        Assert.assertFalse(onStartCalled.get());
        Assert.assertFalse(onErrorCalled.get());
    }

    @Test(expected = RuntimeException.class)
    public void onCompleteTest_calledMultipleTimes_throwsException() throws Exception {
        final AtomicReference<Boolean> onCompleteCalled = new AtomicReference<>(false);
        final AtomicReference<Boolean> onStartCalled = new AtomicReference<>(false);
        final AtomicReference<Boolean> onErrorCalled = new AtomicReference<>(false);
        CompletableOnSubscribe onSubscribe = new CompletableOnSubscribe() {
            @Override
            public void onError(@NonNull Throwable throwable) {
                onErrorCalled.set(true);
            }

            @Override
            public void onComplete() {
                onCompleteCalled.set(true);
            }

            @Override
            public void onStart() {
                onStartCalled.set(true);
            }
        };
        CompletableSubscriberWrapper<CompletableOnSubscribe> wrapper = new CompletableSubscriberWrapper<>(onSubscribe, null, Schedulers.current());
        wrapper.onComplete();

        Assert.assertFalse(onStartCalled.get());
        Assert.assertTrue(onCompleteCalled.get());
        Assert.assertFalse(onErrorCalled.get());

        wrapper.onComplete();
    }

    @Test
    public void onErrorTest_Succeeds_overridden() throws Exception {
        final AtomicReference<Boolean> onCompleteCalled = new AtomicReference<>(false);
        final AtomicReference<Boolean> onStartCalled = new AtomicReference<>(false);
        final AtomicReference<Boolean> onErrorCalled = new AtomicReference<>(false);
        CompletableOnSubscribe onSubscribe = new CompletableOnSubscribe() {
            @Override
            public void onStart() {
                onStartCalled.set(true);
            }

            @Override
            public void onComplete() {
                onCompleteCalled.set(true);
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                onErrorCalled.set(true);
            }
        };
        CompletableSubscriberWrapper<CompletableOnSubscribe> wrapper = new CompletableSubscriberWrapper<>(onSubscribe, null, Schedulers.current());
        wrapper.onError(new Exception("Test exception"));

        Assert.assertTrue(onErrorCalled.get());
        Assert.assertFalse(onStartCalled.get());
        Assert.assertFalse(onCompleteCalled.get());
    }

    @Test(expected = RuntimeException.class)
    public void onErrorTest_throwsException_notOverridden() throws Exception {
        CompletableOnSubscribe onSubscribe = new CompletableOnSubscribe() {
            @Override
            public void onError(@NonNull Throwable throwable) {
                // By not doing anything and calling super, we should throw an exception
                super.onError(throwable);
            }
        };
        CompletableSubscriberWrapper<CompletableOnSubscribe> wrapper = new CompletableSubscriberWrapper<>(onSubscribe, null, Schedulers.current());
        wrapper.onError(new Exception("Test exception"));
    }

    @Test
    public void onStartTest_Succeeds() throws Exception {
        final AtomicReference<Boolean> onCompleteCalled = new AtomicReference<>(false);
        final AtomicReference<Boolean> onStartCalled = new AtomicReference<>(false);
        final AtomicReference<Boolean> onErrorCalled = new AtomicReference<>(false);
        CompletableOnSubscribe onSubscribe = new CompletableOnSubscribe() {
            @Override
            public void onError(@NonNull Throwable throwable) {
                onErrorCalled.set(true);
            }

            @Override
            public void onComplete() {
                onCompleteCalled.set(true);
            }

            @Override
            public void onStart() {
                onStartCalled.set(true);
            }
        };
        CompletableSubscriberWrapper<CompletableOnSubscribe> wrapper = new CompletableSubscriberWrapper<>(onSubscribe, null, Schedulers.current());
        wrapper.onStart();

        Assert.assertTrue(onStartCalled.get());
        Assert.assertFalse(onCompleteCalled.get());
        Assert.assertFalse(onErrorCalled.get());
    }

    @Test(expected = RuntimeException.class)
    public void onStartTest_calledMultipleTimes_throwsException() throws Exception {
        final AtomicReference<Boolean> onCompleteCalled = new AtomicReference<>(false);
        final AtomicReference<Boolean> onStartCalled = new AtomicReference<>(false);
        final AtomicReference<Boolean> onErrorCalled = new AtomicReference<>(false);
        CompletableOnSubscribe onSubscribe = new CompletableOnSubscribe() {
            @Override
            public void onError(@NonNull Throwable throwable) {
                onErrorCalled.set(true);
            }

            @Override
            public void onComplete() {
                onCompleteCalled.set(true);
            }

            @Override
            public void onStart() {
                onStartCalled.set(true);
            }
        };
        CompletableSubscriberWrapper<CompletableOnSubscribe> wrapper = new CompletableSubscriberWrapper<>(onSubscribe, null, Schedulers.current());
        wrapper.onStart();

        Assert.assertTrue(onStartCalled.get());
        Assert.assertFalse(onCompleteCalled.get());
        Assert.assertFalse(onErrorCalled.get());

        wrapper.onStart();
    }

    @Test
    public void unsubscribeTest() throws Exception {
        final AtomicReference<Boolean> onCompleteCalled = new AtomicReference<>(false);
        final AtomicReference<Boolean> onStartCalled = new AtomicReference<>(false);
        final AtomicReference<Boolean> onErrorCalled = new AtomicReference<>(false);
        CompletableOnSubscribe onSubscribe = new CompletableOnSubscribe() {
            @Override
            public void onError(@NonNull Throwable throwable) {
                onErrorCalled.set(true);
            }

            @Override
            public void onComplete() {
                onCompleteCalled.set(true);
            }

            @Override
            public void onStart() {
                onStartCalled.set(true);
            }
        };
        CompletableSubscriberWrapper<CompletableOnSubscribe> wrapper = new CompletableSubscriberWrapper<>(onSubscribe, null, Schedulers.current());
        wrapper.unsubscribe();

        Assert.assertTrue(wrapper.isUnsubscribed());

        wrapper.onStart();
        wrapper.onComplete();
        wrapper.onError(new Exception("Test exception"));

        Assert.assertFalse(onStartCalled.get());
        Assert.assertFalse(onCompleteCalled.get());
        Assert.assertFalse(onErrorCalled.get());
    }

}