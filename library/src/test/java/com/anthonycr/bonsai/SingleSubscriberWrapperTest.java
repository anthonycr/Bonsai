package com.anthonycr.bonsai;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import junit.framework.Assert;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by anthonycr on 2/19/17.
 */
public class SingleSubscriberWrapperTest extends BaseUnitTest {

    @Test
    public void onItemTest_Succeeds() throws Exception {
        final String itemToBeEmitted = "test";
        final AtomicReference<Boolean> onCompleteCalled = new AtomicReference<>(false);
        final AtomicReference<Boolean> onStartCalled = new AtomicReference<>(false);
        final AtomicReference<Boolean> onErrorCalled = new AtomicReference<>(false);
        final AtomicReference<String> emittedItem = new AtomicReference<>(null);

        SingleOnSubscribe<String> onSubscribe = new SingleOnSubscribe<String>() {
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

            @Override
            public void onItem(@Nullable String item) {
                emittedItem.set(item);
            }
        };
        SingleSubscriberWrapper<String> wrapper = new SingleSubscriberWrapper<>(onSubscribe, null, Schedulers.current());
        wrapper.onStart();
        wrapper.onItem(itemToBeEmitted);
        wrapper.onComplete();

        Assert.assertTrue(onCompleteCalled.get());
        Assert.assertTrue(onStartCalled.get());
        Assert.assertFalse(onErrorCalled.get());
        Assert.assertEquals(itemToBeEmitted, emittedItem.get());

    }

    @Test(expected = RuntimeException.class)
    public void onItemTest_Fails_calledAfterOnComplete() throws Exception {
        final String itemToBeEmitted = "test";
        final AtomicReference<Boolean> onCompleteCalled = new AtomicReference<>(false);
        final AtomicReference<Boolean> onStartCalled = new AtomicReference<>(false);
        final AtomicReference<Boolean> onErrorCalled = new AtomicReference<>(false);
        final AtomicReference<String> emittedItem = new AtomicReference<>(null);

        SingleOnSubscribe<String> onSubscribe = new SingleOnSubscribe<String>() {
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

            @Override
            public void onItem(@Nullable String item) {
                emittedItem.set(item);
            }
        };
        SingleSubscriberWrapper<String> wrapper = new SingleSubscriberWrapper<>(onSubscribe, null, Schedulers.current());
        wrapper.onStart();
        wrapper.onComplete();
        wrapper.onItem(itemToBeEmitted);
    }

    @Test(expected = RuntimeException.class)
    public void onItemTest_Fails_calledMultipleTimes() throws Exception {
        final String itemToBeEmitted = "test";
        final AtomicReference<Boolean> onCompleteCalled = new AtomicReference<>(false);
        final AtomicReference<Boolean> onStartCalled = new AtomicReference<>(false);
        final AtomicReference<Boolean> onErrorCalled = new AtomicReference<>(false);
        final AtomicReference<String> emittedItem = new AtomicReference<>(null);

        SingleOnSubscribe<String> onSubscribe = new SingleOnSubscribe<String>() {
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

            @Override
            public void onItem(@Nullable String item) {
                emittedItem.set(item);
            }
        };
        SingleSubscriberWrapper<String> wrapper = new SingleSubscriberWrapper<>(onSubscribe, null, Schedulers.current());
        wrapper.onStart();
        wrapper.onItem(itemToBeEmitted);
        wrapper.onItem(itemToBeEmitted);
        wrapper.onComplete();
    }

    @Test
    public void unsubscribe_itemNotEmitted() throws Exception {
        final String itemToBeEmitted = "test";
        final AtomicReference<Boolean> onCompleteCalled = new AtomicReference<>(false);
        final AtomicReference<Boolean> onStartCalled = new AtomicReference<>(false);
        final AtomicReference<Boolean> onErrorCalled = new AtomicReference<>(false);
        final AtomicReference<String> emittedItem = new AtomicReference<>(null);

        SingleOnSubscribe<String> onSubscribe = new SingleOnSubscribe<String>() {
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

            @Override
            public void onItem(@Nullable String item) {
                emittedItem.set(item);
            }
        };
        SingleSubscriberWrapper<String> wrapper = new SingleSubscriberWrapper<>(onSubscribe, null, Schedulers.current());
        wrapper.onStart();

        // Unsubscribe after onStart
        wrapper.unsubscribe();

        wrapper.onItem(itemToBeEmitted);
        wrapper.onComplete();

        Assert.assertTrue(onStartCalled.get());

        // Unsubscribed so the following assertions should be made
        Assert.assertFalse(onCompleteCalled.get());
        Assert.assertFalse(onErrorCalled.get());
        Assert.assertNull(emittedItem.get());
    }

}