package com.anthonycr.bonsai;

import android.support.annotation.NonNull;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

/**
 * Created by anthonycr on 2/19/17.
 */
public class CompletableSubscriberWrapperTest extends BaseUnitTest {

    @Mock
    private CompletableOnSubscribe completableOnSubscribe;

    @Test
    public void onCompleteTest_Succeeds() throws Exception {
        CompletableSubscriberWrapper<CompletableOnSubscribe> wrapper = new CompletableSubscriberWrapper<>(completableOnSubscribe, null, Schedulers.current());
        wrapper.onComplete();

        Mockito.verify(completableOnSubscribe).onComplete();

        Mockito.verifyNoMoreInteractions(completableOnSubscribe);
    }

    @Test(expected = RuntimeException.class)
    public void onCompleteTest_calledMultipleTimes_throwsException() throws Exception {
        CompletableSubscriberWrapper<CompletableOnSubscribe> wrapper = new CompletableSubscriberWrapper<>(completableOnSubscribe, null, Schedulers.current());
        wrapper.onComplete();

        Mockito.verify(completableOnSubscribe).onComplete();

        Mockito.verifyNoMoreInteractions(completableOnSubscribe);

        wrapper.onComplete();
    }

    @Test
    public void onErrorTest_Succeeds_overridden() throws Exception {
        CompletableSubscriberWrapper<CompletableOnSubscribe> wrapper = new CompletableSubscriberWrapper<>(completableOnSubscribe, null, Schedulers.current());

        Exception exception = new Exception("Test exception");

        wrapper.onError(exception);

        Mockito.verify(completableOnSubscribe).onError(exception);

        Mockito.verifyNoMoreInteractions(completableOnSubscribe);
    }

    @Test
    public void onErrorTest_onCompleteNotCalled() throws Exception {
        CompletableSubscriberWrapper<CompletableOnSubscribe> wrapper = new CompletableSubscriberWrapper<>(completableOnSubscribe, null, Schedulers.current());

        Exception exception = new Exception("Test exception");

        wrapper.onError(exception);
        wrapper.onComplete();

        Mockito.verify(completableOnSubscribe).onError(exception);

        Mockito.verifyNoMoreInteractions(completableOnSubscribe);
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
        CompletableSubscriberWrapper<CompletableOnSubscribe> wrapper = new CompletableSubscriberWrapper<>(completableOnSubscribe, null, Schedulers.current());
        wrapper.onStart();

        Mockito.verify(completableOnSubscribe).onStart();

        Mockito.verifyNoMoreInteractions(completableOnSubscribe);
    }

    @Test(expected = RuntimeException.class)
    public void onStartTest_calledMultipleTimes_throwsException() throws Exception {
        CompletableSubscriberWrapper<CompletableOnSubscribe> wrapper = new CompletableSubscriberWrapper<>(completableOnSubscribe, null, Schedulers.current());
        wrapper.onStart();

        Mockito.verify(completableOnSubscribe).onStart();

        Mockito.verifyNoMoreInteractions(completableOnSubscribe);

        wrapper.onStart();
    }

    @Test
    public void unsubscribeTest() throws Exception {
        CompletableSubscriberWrapper<CompletableOnSubscribe> wrapper = new CompletableSubscriberWrapper<>(completableOnSubscribe, null, Schedulers.current());
        wrapper.unsubscribe();

        Assert.assertTrue(wrapper.isUnsubscribed());

        wrapper.onStart();
        wrapper.onComplete();
        wrapper.onError(new Exception("Test exception"));

        Mockito.verifyZeroInteractions(completableOnSubscribe);
    }

}