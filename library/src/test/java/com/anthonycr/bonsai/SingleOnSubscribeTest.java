package com.anthonycr.bonsai;

import org.junit.Test;

/**
 * Test for {@link SingleOnSubscribe}
 */
public class SingleOnSubscribeTest {

    private static class DummySingleOnSubscribe extends SingleOnSubscribe<Object> {

    }

    @Test
    public void onStart_notOverridden_noException() throws Exception {
        DummySingleOnSubscribe dummySingleOnSubscribe = new DummySingleOnSubscribe();

        dummySingleOnSubscribe.onStart();
    }

    @Test
    public void onItem_notOverridden_noException() throws Exception {
        DummySingleOnSubscribe dummySingleOnSubscribe = new DummySingleOnSubscribe();

        dummySingleOnSubscribe.onItem(new Object());
    }

    @Test
    public void onComplete_notOverridden_noException() throws Exception {
        DummySingleOnSubscribe dummySingleOnSubscribe = new DummySingleOnSubscribe();

        dummySingleOnSubscribe.onComplete();
    }

    @Test(expected = RuntimeException.class)
    public void onError_notOverridden_throwsException() {
        DummySingleOnSubscribe dummySingleOnSubscribe = new DummySingleOnSubscribe();

        dummySingleOnSubscribe.onError(new Exception("Test exception"));
    }

}