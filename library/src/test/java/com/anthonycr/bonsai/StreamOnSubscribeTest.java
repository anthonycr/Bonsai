package com.anthonycr.bonsai;

import org.junit.Test;

/**
 * Test for {@link StreamOnSubscribe}
 */
public class StreamOnSubscribeTest {

    private static class DummyStreamOnSubscribe extends StreamOnSubscribe<Object> {

    }

    @Test
    public void onStart_notOverridden_noException() throws Exception {
        DummyStreamOnSubscribe dummyStreamOnSubscribe = new DummyStreamOnSubscribe();

        dummyStreamOnSubscribe.onStart();
    }

    @Test
    public void onItem_notOverridden_noException() throws Exception {
        DummyStreamOnSubscribe dummyStreamOnSubscribe = new DummyStreamOnSubscribe();

        dummyStreamOnSubscribe.onNext(new Object());
        dummyStreamOnSubscribe.onNext(new Object());
    }

    @Test
    public void onComplete_notOverridden_noException() throws Exception {
        DummyStreamOnSubscribe dummyStreamOnSubscribe = new DummyStreamOnSubscribe();

        dummyStreamOnSubscribe.onComplete();
    }

    @Test(expected = RuntimeException.class)
    public void onError_notOverridden_throwsException() {
        DummyStreamOnSubscribe dummyStreamOnSubscribe = new DummyStreamOnSubscribe();

        dummyStreamOnSubscribe.onError(new Exception("Test exception"));
    }
}