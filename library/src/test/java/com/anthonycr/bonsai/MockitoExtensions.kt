package com.anthonycr.bonsai

import org.mockito.Mockito

/**
 * @see Mockito.verify
 */
fun <T> T.verifyOnlyOneInteraction(): T = Mockito.verify(this)

/**
 * @see Mockito.verifyNoMoreInteractions
 */
fun <T> T.verifyNoMoreInteractions() = Mockito.verifyNoMoreInteractions(this)

/**
 * @see Mockito.verifyZeroInteractions
 */
fun <T> T.verifyZeroInteractions() = Mockito.verifyZeroInteractions(this)