package com.anthonycr.bonsai;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

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
        for (String item : testList) {
            inOrder.verify(stringStreamOnSubscribe).onNext(item);
        }
        inOrder.verify(stringStreamOnSubscribe).onComplete();

        Mockito.verifyNoMoreInteractions(stringStreamOnSubscribe);
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

        Mockito.verifyNoMoreInteractions(stringStreamOnSubscribe);

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

        Mockito.verifyNoMoreInteractions(stringStreamOnSubscribe);
    }

    @Test
    public void onError_itemsNotEmitted() throws Exception {
        List<String> testList = Arrays.asList("one", "two", "three", "four", "five");

        StreamSubscriberWrapper<String> wrapper = new StreamSubscriberWrapper<>(stringStreamOnSubscribe, null, Schedulers.current());

        wrapper.onStart();

        String onlyItem = "onlyItem";

        wrapper.onNext(onlyItem);

        // Throw an error after on start
        Throwable throwable = new Exception("Test exception");
        wrapper.onError(throwable);

        for (String item : testList) {
            wrapper.onNext(item);
        }

        InOrder inOrder = Mockito.inOrder(stringStreamOnSubscribe);

        inOrder.verify(stringStreamOnSubscribe).onStart();
        inOrder.verify(stringStreamOnSubscribe).onNext(onlyItem);
        inOrder.verify(stringStreamOnSubscribe).onError(throwable);

        Mockito.verifyNoMoreInteractions(stringStreamOnSubscribe);
    }

    @Test
    public void onError_unsubscribe_itemsNotEmitted() throws Exception {
        List<String> testList = Arrays.asList("one", "two", "three", "four", "five");

        StreamSubscriberWrapper<String> wrapper = new StreamSubscriberWrapper<>(stringStreamOnSubscribe, null, Schedulers.current());

        wrapper.onStart();

        String onlyItem = "onlyItem";

        wrapper.onNext(onlyItem);

        // Throw an error after on start and call unsubscribe
        Throwable throwable = new Exception("Test exception");
        wrapper.onError(throwable);
        wrapper.unsubscribe();

        for (String item : testList) {
            wrapper.onNext(item);
        }

        InOrder inOrder = Mockito.inOrder(stringStreamOnSubscribe);

        inOrder.verify(stringStreamOnSubscribe).onStart();
        inOrder.verify(stringStreamOnSubscribe).onNext(onlyItem);
        inOrder.verify(stringStreamOnSubscribe).onError(throwable);

        Mockito.verifyNoMoreInteractions(stringStreamOnSubscribe);
    }

}