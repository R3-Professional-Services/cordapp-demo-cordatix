package com.cordatix.contracts

import com.cordatix.states.TicketState
import org.junit.Test
import kotlin.test.assertEquals

class TicketStateTests {
    @Test
    fun hasFieldOfCorrectType() {
        // Does the field exist?
        TicketState::class.java.getDeclaredField("msg")
        // Is the field of the correct type?
        assertEquals(TicketState::class.java.getDeclaredField("msg").type, String()::class.java)
    }
}