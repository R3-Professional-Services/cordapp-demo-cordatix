package com.cordatix.contracts

import com.cordatix.states.EventState
import org.junit.Test
import kotlin.test.assertEquals

class EventStateTests {
    @Test
    fun hasFieldOfCorrectType() {
        // Does the field exist?
        EventState::class.java.getDeclaredField("msg")
        // Is the field of the correct type?
        assertEquals(EventState::class.java.getDeclaredField("msg").type, String()::class.java)
    }
}