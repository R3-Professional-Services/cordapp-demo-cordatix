package com.cordatix.states

import com.cordatix.contracts.EventContract
import com.cordatix.contracts.Status
import com.cordatix.contracts.StatusState
import com.cordatix.dto.EventDetails
import com.cordatix.schema.EventSchema
import net.corda.core.contracts.*
import net.corda.core.flows.FlowLogicRefFactory
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.util.stream.Collectors

// **************
// * EventState *
// **************
@BelongsToContract(EventContract::class)
data class EventState(val eventDetails: EventDetails,
                      val ticketList: List<LinearPointer<TicketState>>,
                      val eventAgency: AbstractParty,
                      override val participants: List<AbstractParty> = listOf(eventAgency),
                      override val linearId: UniqueIdentifier,
                      override val status: Status?
) : StatusState, LinearState, SchedulableState, QueryableState {
    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        val tickets = ticketList.stream().map{t -> t.pointer.id}.collect(Collectors.toList()).toString()
        return when (schema) {
            is EventSchema.EventSchemaV1 -> EventSchema.PersistentEvent(
                linearId.id,
                eventDetails.eventName,
                eventDetails.eventVenue,
                eventDetails.eventVenueCapacity,
                eventDetails.eventCity,
                eventDetails.eventStartTime,
                eventDetails.eventEndTime,
                status,
                tickets,
                eventAgency.nameOrNull()?.commonName
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> {
        return listOf(EventSchema.EventSchemaV1)
    }

    override fun toString(): String {
        return "EventState(eventDetails=$eventDetails, ticketList=$ticketList, eventAgency=$eventAgency, participants=$participants, linearId=$linearId, status=$status)"
    }
    override fun nextScheduledActivity(thisStateRef: StateRef, flowLogicRefFactory: FlowLogicRefFactory): ScheduledActivity? {
        return when (status) {
            EventStatus.UPCOMING -> {
                val flowLogicRef = flowLogicRefFactory.create("net.corda.samples.auction.flows.StartEventFlow", linearId)
                ScheduledActivity(flowLogicRef, eventDetails.eventStartTime)
            }
            EventStatus.ONGOING -> {
                val flowLogicRef = flowLogicRefFactory.create("net.corda.samples.auction.flows.EndEventFlow", linearId)
                ScheduledActivity(flowLogicRef, eventDetails.eventEndTime)
            }
            else -> null
        }
    }
}

enum class EventStatus: Status {
    UPCOMING,
    ONGOING,
    CANCELLED,
    ENDED
}
