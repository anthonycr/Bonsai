package com.anthonycr.bonsai;

import org.junit.Test;

/**
 * Test fo {@link CompletableOnSubscribe}
 */
public class CompletableOnSubscribeTest {

    private static class DummyCompletableOnSubscribe extends CompletableOnSubscribe {

    }

    @Test
    public void onStart_notOverridden_noException() throws Exception {
        DummyCompletableOnSubscribe dummyCompletableOnSubscribe = new DummyCompletableOnSubscribe();

        dummyCompletableOnSubscribe.onStart();
    }

    @Test
    public void onComplete_notOverridden_noException() throws Exception {
        DummyCompletableOnSubscribe dummyCompletableOnSubscribe = new DummyCompletableOnSubscribe();

        dummyCompletableOnSubscribe.onComplete();
    }

    @Test(expected = RuntimeException.class)
    public void onError_notOverridden_throwsException() {
        DummyCompletableOnSubscribe dummyCompletableOnSubscribe = new DummyCompletableOnSubscribe();

        dummyCompletableOnSubscribe.onError(new Exception("Test exception"));
    }

}