package com.anthonycr.bonsai;

import android.support.annotation.Nullable;

import junit.framework.Assert;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by anthonycr on 2/19/17.
 */
public class StreamSubscriberWrapperTest extends BaseUnitTest {

    @Test
    public void onNextTest_Succeeds() throws Exception {

        final AtomicReference<Boolean> onStartCalled = new AtomicReference<>(false);
        final AtomicReference<Boolean> onCompleteCalled = new AtomicReference<>(false);

        List<String> testList = Arrays.asList("one", "two", "three", "four", "five");

        final List<String> subscriptionList = new ArrayList<>(testList.size());

        StreamOnSubscribe<String> onSubscribe = new StreamOnSubscribe<String>() {
            @Override
            public void onStart() {
                onStartCalled.set(true);
            }

            @Override
            public void onComplete() {
                onCompleteCalled.set(true);
            }

            @Override
            public void onNext(@Nullable String item) {
                subscriptionList.add(item);
            }
        };
        StreamSubscriberWrapper<String> wrapper = new StreamSubscriberWrapper<>(onSubscribe, null, Schedulers.current());

        wrapper.onStart();
        for (String item : testList) {
            wrapper.onNext(item);
        }
        wrapper.onComplete();

        Assert.assertTrue(onStartCalled.get());
        Assert.assertTrue(onCompleteCalled.get());
        Assert.assertEquals(testList, subscriptionList);
    }

    @Test(expected = RuntimeException.class)
    public void onNextTest_Fails_calledAfterOnComplete() throws Exception {
        final AtomicReference<Boolean> onStartCalled = new AtomicReference<>(false);
        final AtomicReference<Boolean> onCompleteCalled = new AtomicReference<>(false);

        List<String> testList = Arrays.asList("one", "two", "three", "four", "five");

        final List<String> subscriptionList = new ArrayList<>(testList.size());

        StreamOnSubscribe<String> onSubscribe = new StreamOnSubscribe<String>() {
            @Override
            public void onStart() {
                onStartCalled.set(true);
            }

            @Override
            public void onComplete() {
                onCompleteCalled.set(true);
            }

            @Override
            public void onNext(@Nullable String item) {
                subscriptionList.add(item);
            }
        };
        StreamSubscriberWrapper<String> wrapper = new StreamSubscriberWrapper<>(onSubscribe, null, Schedulers.current());

        wrapper.onStart();
        wrapper.onComplete();

        for (String item : testList) {
            wrapper.onNext(item);
        }
    }

    @Test
    public void unsubscribe_itemsNotEmitted() throws Exception {

        final AtomicReference<Boolean> onStartCalled = new AtomicReference<>(false);
        final AtomicReference<Boolean> onCompleteCalled = new AtomicReference<>(false);

        List<String> testList = Arrays.asList("one", "two", "three", "four", "five");

        final List<String> subscriptionList = new ArrayList<>(testList.size());

        StreamOnSubscribe<String> onSubscribe = new StreamOnSubscribe<String>() {
            @Override
            public void onStart() {
                onStartCalled.set(true);
            }

            @Override
            public void onComplete() {
                onCompleteCalled.set(true);
            }

            @Override
            public void onNext(@Nullable String item) {
                subscriptionList.add(item);
            }
        };
        StreamSubscriberWrapper<String> wrapper = new StreamSubscriberWrapper<>(onSubscribe, null, Schedulers.current());

        wrapper.onStart();

        String onlyItem = "onlyItem";

        wrapper.onNext(onlyItem);

        // Unsubscribe from the stream
        wrapper.unsubscribe();

        for (String item : testList) {
            wrapper.onNext(item);
        }
        wrapper.onComplete();

        Assert.assertTrue(onStartCalled.get());
        Assert.assertFalse(onCompleteCalled.get());
        Assert.assertTrue(subscriptionList.size() == 1);
        Assert.assertTrue(subscriptionList.get(0).equals(onlyItem));
    }

}