package com.cordatix.contracts

import com.cordatix.states.EventState
import com.cordatix.states.EventStatus
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.LedgerTransaction
import org.apache.commons.lang3.ObjectUtils

// ******************
// * Event Contract *
// ******************

class EventContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.cordatix.contracts.EventContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
        verifyUniversalConstraints(tx)
        verifyStatusConstraints(tx)
        verifyLinearIDConstraints(tx)
        verifyCommandConstraints(tx)
        verifySigningConstraints(tx)
    }

    private fun verifyUniversalConstraints(tx: LedgerTransaction) {
        val allStates = tx.inputsOfType<EventState>() + tx.outputsOfType<EventState>()
        for (s in allStates) {
            requireThat {
                "Event details must not be empty or null." using (ObjectUtils.isNotEmpty(s.eventDetails))
                "Event capacity must be greater than zero." using (s.eventDetails.eventVenueCapacity > 0)
                "Event end time must be greater than event start time." using (s.eventDetails.eventStartTime < s.eventDetails.eventEndTime)
            }
        }
    }

    private fun verifyStatusConstraints(tx: LedgerTransaction) {
        val allStates = tx.inputsOfType<EventState>() + tx.outputsOfType<EventState>()
        for (s in allStates) {
            when (s.status) {
                EventStatus.CANCELLED, EventStatus.ENDED -> requireThat {
                    "List of event tickets must be empty." using (s.ticketList.isEmpty())
                }
            }
        }
    }

    private fun verifyLinearIDConstraints(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<EventContract.Commands>()
        val inputStates = tx.inputsOfType<EventState>()
        val outputStates = tx.outputsOfType<EventState>()

        requireThat{
            "When using LinearStates there should be a maximum of one Primary input state." using (inputStates.size <= 1)
            "When using LinearStates there should be a maximum of one Primary output state." using (outputStates.size <= 1)
        }

        val inputState = inputStates.singleOrNull()
        val outputState = outputStates.singleOrNull()

        val commandName = command.value::class.java.simpleName
        when (command.value){
            is Commands.Start,
            is Commands.Cancel,
            is Commands.End-> {
                requireThat {"When the Command is $commandName the LinearID must not change." using(inputState?.linearId == outputState?.linearId)}
            }
        }
    }

    private fun verifyCommandConstraints(tx: LedgerTransaction) {
        when {
            tx.commands.isEmpty() -> {
                throw IllegalArgumentException("One command is Expected!")
            }
            else -> {
                val command = tx.commands[0]
                when (command.value) {
                    is Commands.Create -> requireThat {
                        "No inputs should be consumed while creating an Event" using (tx.inputs.isEmpty())
                        "Only one output should be created" using (tx.outputStates.size == 1)
                        val output = tx.outputsOfType<EventState>()[0]
                        "Ticket list must be initialised and updated." using (output.ticketList.isNotEmpty())
                        "Ticket list size must be less than or equal to event capacity." using (output.ticketList.size <= output.eventDetails.eventVenueCapacity)
                    }
                    is Commands.Start, is Commands.End -> requireThat {
                        "One Input Expected" using (tx.inputStates.size == 1)
                        "One Output Expected" using (tx.outputStates.size == 1)
                        val input = tx.inputsOfType<EventState>()[0]
                        val output = tx.outputsOfType<EventState>()[0]
                        "Event details must remain same" using (input.eventDetails == output.eventDetails)
                    }
                    is Commands.Cancel -> requireThat {
                        "One Input Expected" using (tx.inputStates.size == 1)
                        "One Output Expected" using (tx.outputStates.size == 1)
                        val input = tx.inputsOfType<EventState>()[0]
                        val output = tx.outputsOfType<EventState>()[0]
                        "Event description should be updated with cancellation reason." using (output.eventDetails.eventDescription.contains(
                            "Cancelled because "
                        ))
                        "All other event details must remain same" using (output.eventDetails.eventVenueCapacity == input.eventDetails.eventVenueCapacity
                                && output.eventDetails.eventVenue == input.eventDetails.eventVenue
                                && output.eventDetails.eventCity == input.eventDetails.eventCity
                                && output.eventDetails.eventName == input.eventDetails.eventName
                                && output.eventDetails.eventType == input.eventDetails.eventType
                                && output.eventDetails.eventStartTime == input.eventDetails.eventStartTime
                                && output.eventDetails.eventEndTime == input.eventDetails.eventEndTime)
                    }
                    is Commands.Complete -> requireThat {
                        "One Input Expected" using (tx.inputStates.size == 1)
                        "No Output Expected" using (tx.outputStates.isEmpty())
                    }
                }
            }
        }
    }

    private fun verifySigningConstraints(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<EventContract.Commands>()
        val commandName = command.value::class.java.simpleName
        val inputStates = tx.inputsOfType<EventState>()
        val outputStates = tx.outputsOfType<EventState>()
        val inputState = inputStates.singleOrNull()
        val outputState = outputStates.singleOrNull()
        fun checkSigner(signerDescription: String, signer: AbstractParty?){
            requireThat { "When the Command is $commandName the $signerDescription must sign." using (command.signers.contains(signer?.owningKey))}
        }
        when (command.value) {
            is Commands.Create,
            is Commands.Start,
            is Commands.Cancel,
            is Commands.End-> {
                checkSigner("output.eventAgency", outputState?.eventAgency)
            }
            is Commands.Complete -> {
                checkSigner("input.eventAgency", inputState?.eventAgency)
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Create : Commands
        class Start : Commands
        class Cancel : Commands
        class End : Commands
        class Complete : Commands
    }
}