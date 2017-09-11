package com.anthonycr.bonsai;

/**
 * An exception thrown by bonsai when problems caused by misuse of the library arise.
 */
class ReactiveEventException extends Exception {
    public ReactiveEventException(String message) {
        super(message);
    }

    public ReactiveEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
