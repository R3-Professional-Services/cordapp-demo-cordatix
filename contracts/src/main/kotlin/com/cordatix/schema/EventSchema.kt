package com.cordatix.schema

import com.cordatix.contracts.Status
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.time.Instant
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table


object EventSchema {
    /**
     * An EventState schema.
     */
    object EventSchemaV1 : MappedSchema(
        schemaFamily = EventSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentEvent::class.java)) {
        override val migrationResource: String?
            get() = "cordatix.changelog-master";
    }
    @Entity
    @Table(name = "event_states")
    class PersistentEvent(
        @Column(name = "linear_id")
        val id: UUID,
        @Column(name = "event_name")
        val eventName: String,
        @Column(name = "event_venue")
        val eventVenue: String,
        @Column(name = "event_capacity")
        val eventVenueCapacity: Int,
        @Column(name = "event_city")
        val eventCity: String,
        @Column(name = "event_start_time")
        val eventStartTime: Instant,
        @Column(name = "event_end_time")
        val eventEndTime: Instant,
        @Column(name = "event_status")
        val status: Status?,
        @Column(name = "ticket_list")
        val tickets: String,
        @Column(name = "event_agency")
        val eventAgency: String?
    ) : PersistentState()
}
