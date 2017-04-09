package com.anthonycr.bonsai;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

/**
 * Test for {@link SingleSubscriberWrapper}.
 */
public class SingleSubscriberWrapperTest extends BaseUnitTest {

    @Mock
    private SingleOnSubscribe<String> stringSingleOnSubscribe;

    @Test
    public void onItemTest_Succeeds() throws Exception {
        final String itemToBeEmitted = "test";

        SingleSubscriberWrapper<String> wrapper = new SingleSubscriberWrapper<>(stringSingleOnSubscribe, null, Schedulers.immediate());
        wrapper.onStart();
        wrapper.onItem(itemToBeEmitted);
        wrapper.onComplete();

        InOrder inOrder = Mockito.inOrder(stringSingleOnSubscribe);

        inOrder.verify(stringSingleOnSubscribe).onStart();
        inOrder.verify(stringSingleOnSubscribe).onItem(itemToBeEmitted);
        inOrder.verify(stringSingleOnSubscribe).onComplete();

        Mockito.verifyNoMoreInteractions(stringSingleOnSubscribe);

    }

    @Test(expected = RuntimeException.class)
    public void onItemTest_Fails_calledAfterOnComplete() throws Exception {
        final String itemToBeEmitted = "test";

        SingleSubscriberWrapper<String> wrapper = new SingleSubscriberWrapper<>(stringSingleOnSubscribe, null, Schedulers.immediate());
        wrapper.onStart();
        wrapper.onComplete();

        InOrder inOrder = Mockito.inOrder(stringSingleOnSubscribe);

        inOrder.verify(stringSingleOnSubscribe).onStart();
        inOrder.verify(stringSingleOnSubscribe).onComplete();

        Mockito.verifyNoMoreInteractions(stringSingleOnSubscribe);

        wrapper.onItem(itemToBeEmitted);
    }

    @Test(expected = RuntimeException.class)
    public void onItemTest_Fails_calledMultipleTimes() throws Exception {
        final String itemToBeEmitted = "test";

        SingleSubscriberWrapper<String> wrapper = new SingleSubscriberWrapper<>(stringSingleOnSubscribe, null, Schedulers.immediate());
        wrapper.onStart();
        wrapper.onItem(itemToBeEmitted);

        InOrder inOrder = Mockito.inOrder(stringSingleOnSubscribe);

        inOrder.verify(stringSingleOnSubscribe).onStart();
        inOrder.verify(stringSingleOnSubscribe).onItem(itemToBeEmitted);

        Mockito.verifyNoMoreInteractions(stringSingleOnSubscribe);

        wrapper.onItem(itemToBeEmitted);
    }

    @Test
    public void unsubscribe_itemNotEmitted() throws Exception {
        final String itemToBeEmitted = "test";

        SingleSubscriberWrapper<String> wrapper = new SingleSubscriberWrapper<>(stringSingleOnSubscribe, null, Schedulers.immediate());
        wrapper.onStart();

        // Unsubscribe after onStart
        wrapper.unsubscribe();

        wrapper.onItem(itemToBeEmitted);
        wrapper.onComplete();

        Mockito.verify(stringSingleOnSubscribe).onStart();

        Mockito.verifyNoMoreInteractions(stringSingleOnSubscribe);
    }

    @Test
    public void onError_itemNotEmitted() throws Exception {
        final String itemToBeEmitted = "test";

        SingleSubscriberWrapper<String> wrapper = new SingleSubscriberWrapper<>(stringSingleOnSubscribe, null, Schedulers.immediate());
        wrapper.onStart();

        // Throw an error after onStart
        Throwable throwable = new Exception("Test exception");
        wrapper.onError(throwable);

        wrapper.onItem(itemToBeEmitted);

        InOrder inOrder = Mockito.inOrder(stringSingleOnSubscribe);

        inOrder.verify(stringSingleOnSubscribe).onStart();
        inOrder.verify(stringSingleOnSubscribe).onError(throwable);

        Mockito.verifyNoMoreInteractions(stringSingleOnSubscribe);
    }

    @Test
    public void onError_unsubscribe_itemNotEmitted() throws Exception {
        final String itemToBeEmitted = "test";

        SingleSubscriberWrapper<String> wrapper = new SingleSubscriberWrapper<>(stringSingleOnSubscribe, null, Schedulers.immediate());
        wrapper.onStart();

        // Throw an error after onStart and then unsubscribe
        Throwable throwable = new Exception("Test exception");
        wrapper.onError(throwable);
        wrapper.unsubscribe();

        wrapper.onItem(itemToBeEmitted);

        InOrder inOrder = Mockito.inOrder(stringSingleOnSubscribe);

        inOrder.verify(stringSingleOnSubscribe).onStart();
        inOrder.verify(stringSingleOnSubscribe).onError(throwable);

        Mockito.verifyNoMoreInteractions(stringSingleOnSubscribe);
    }

}