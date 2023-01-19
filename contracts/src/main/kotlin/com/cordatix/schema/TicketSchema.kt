package com.cordatix.schema

import com.cordatix.contracts.Status
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

class TicketSchema {
    /**
     * The family of schemas for TicketState.
     */
    object TicketSchema
    /**
     * A TicketState schema.
     */
    object TicketSchemaV1 : MappedSchema(
        schemaFamily = TicketSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentTicket::class.java)) {
        override val migrationResource: String?
            get() = "cordatix.changelog-master";
    }

    @Entity
    @Table(name = "ticket_states")
    class PersistentTicket(
        @Column(name = "linear_id")
        val id: UUID,
        @Column(name = "event_id")
        val eventId: UUID,
        @Column(name = "ticket_category")
        val ticketCategory: String,
        @Column(name = "ticket_price")
        val ticketPrice: Double,
        @Column(name = "ticket_issuer")
        val ticketIssuer: String?,
        @Column(name = "ticket_holder")
        val ticketHolder: String?,
        @Column(name = "ticket_status")
        val ticketStatus: Status?
    ) : PersistentState()
}