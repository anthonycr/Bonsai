package com.anthonycr.bonsai

import java.util.concurrent.CountDownLatch


/**
 * Calls [CountDownLatch.await] and swallows any exceptions, printing the stacktrace if something
 * goes wrong.
 */
fun CountDownLatch.safeAwait() {
    try {
        await()
    } catch (e: InterruptedException) {
        e.printStackTrace()
    }
}