package com.anthonycr.bonsai

import org.mockito.Mockito

/**
 * Created by anthonycr on 9/9/17.
 */


fun <T> T.verifyOnlyOneInteraction() = Mockito.verify(this)

fun <T> T.verifyNoMoreInteractions() = Mockito.verifyNoMoreInteractions(this)

fun <T> T.verifyZeroInteractions() = Mockito.verifyZeroInteractions(this)