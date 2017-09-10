package com.anthonycr.bonsai

import com.anthonycr.bonsai.ReactiveEventException

/**
 * Created by anthonycr on 9/9/17.
 */

internal inline fun requireCondition(condition: Boolean, message: () -> String) {
    if (!condition) {
        throw ReactiveEventException(message())
    }
}