package com.anthonycr.bonsai.refactor

/**
 * Created by anthonycr on 9/9/17.
 */

internal class ReactiveEventException : Exception {

    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)

}