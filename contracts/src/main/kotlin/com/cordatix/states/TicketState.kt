package com.cordatix.states

import com.cordatix.contracts.Status
import com.cordatix.contracts.StatusState
import com.cordatix.contracts.TicketContract
import com.cordatix.schema.TicketSchema
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

// ***************
// * TicketState *
// ***************
@BelongsToContract(TicketContract::class)
data class TicketState(val eventPointer: LinearPointer<EventState>,
                       val ticketCategory: Enum<TicketCategory>,
                       val ticketPrice: Double,
                       val issuer: Party,
                       val holder: AbstractParty,
                       override val participants: List<AbstractParty> = listOf(issuer,holder),
                       override val linearId: UniqueIdentifier,
                       override val status: Status?
) : StatusState, LinearState, QueryableState {
    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is TicketSchema.TicketSchemaV1 -> TicketSchema.PersistentTicket(
                linearId.id,
                eventPointer.pointer.id,
                ticketCategory.name,
                ticketPrice,
                issuer.name.commonName,
                holder.nameOrNull()?.commonName,
                status
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> {
        return listOf(TicketSchema.TicketSchemaV1)
    }

    override fun toString(): String {
        return "TicketState(eventPointer=$eventPointer, ticketCategory=$ticketCategory, ticketPrice=$ticketPrice, issuer=$issuer, holder=$holder, participants=$participants, linearId=$linearId, status=$status)"
    }
}

enum class TicketStatus: Status {
    NEW,
    BOOKED,
    REDEEMED
}

enum class TicketCategory {
    SILVER,
    GOLD,
    PLATINUM
}
