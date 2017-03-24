package com.anthonycr.bonsai;

import android.support.annotation.Nullable;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by anthonycr on 2/19/17.
 */
public class StreamSubscriberWrapperTest extends BaseUnitTest {

    @Mock
    private StreamOnSubscribe<String> stringStreamOnSubscribe;

    @Test
    public void onNextTest_Succeeds() throws Exception {

        List<String> testList = Arrays.asList("one", "two", "three", "four", "five");

        StreamSubscriberWrapper<String> wrapper = new StreamSubscriberWrapper<>(stringStreamOnSubscribe, null, Schedulers.current());

        wrapper.onStart();
        for (String item : testList) {
            wrapper.onNext(item);
        }
        wrapper.onComplete();

        InOrder inOrder = Mockito.inOrder(stringStreamOnSubscribe);

        inOrder.verify(stringStreamOnSubscribe).onStart();
        inOrder.verify(stringStreamOnSubscribe).onNext(testList.get(0));
        inOrder.verify(stringStreamOnSubscribe).onNext(testList.get(1));
        inOrder.verify(stringStreamOnSubscribe).onNext(testList.get(2));
        inOrder.verify(stringStreamOnSubscribe).onNext(testList.get(3));
        inOrder.verify(stringStreamOnSubscribe).onNext(testList.get(4));
        inOrder.verify(stringStreamOnSubscribe).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test(expected = RuntimeException.class)
    public void onNextTest_Fails_calledAfterOnComplete() throws Exception {
        List<String> testList = Arrays.asList("one", "two", "three", "four", "five");

        StreamSubscriberWrapper<String> wrapper = new StreamSubscriberWrapper<>(stringStreamOnSubscribe, null, Schedulers.current());

        wrapper.onStart();
        wrapper.onComplete();

        InOrder inOrder = Mockito.inOrder(stringStreamOnSubscribe);

        inOrder.verify(stringStreamOnSubscribe).onStart();
        inOrder.verify(stringStreamOnSubscribe).onComplete();
        inOrder.verifyNoMoreInteractions();

        for (String item : testList) {
            wrapper.onNext(item);
        }
    }

    @Test
    public void unsubscribe_itemsNotEmitted() throws Exception {
        List<String> testList = Arrays.asList("one", "two", "three", "four", "five");

        StreamSubscriberWrapper<String> wrapper = new StreamSubscriberWrapper<>(stringStreamOnSubscribe, null, Schedulers.current());

        wrapper.onStart();

        String onlyItem = "onlyItem";

        wrapper.onNext(onlyItem);

        // Unsubscribe from the stream
        wrapper.unsubscribe();

        for (String item : testList) {
            wrapper.onNext(item);
        }
        wrapper.onComplete();

        InOrder inOrder = Mockito.inOrder(stringStreamOnSubscribe);

        inOrder.verify(stringStreamOnSubscribe).onStart();
        inOrder.verify(stringStreamOnSubscribe).onNext(onlyItem);
        inOrder.verifyNoMoreInteractions();
    }

}