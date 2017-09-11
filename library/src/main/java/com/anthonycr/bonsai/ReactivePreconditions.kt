package com.anthonycr.bonsai

/**
 * Requires a condition be true, otherwise it throws a [ReactiveEventException].
 */
internal inline fun requireCondition(condition: Boolean, message: () -> String) {
    if (!condition) {
        throw ReactiveEventException(message())
    }
}