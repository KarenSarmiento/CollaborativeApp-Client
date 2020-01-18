package com.karensarmiento.collaborationapp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpstreamRequestHandlerTest {

    @Test
    fun `sample test`() {
        assertEquals(1+1, 2)
    }
}