package com.cordatix.contracts

import com.cordatix.states.EventState
import com.cordatix.states.EventStatus
import com.cordatix.states.TicketState
import com.cordatix.states.TicketStatus
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.LedgerTransaction
import org.apache.commons.lang3.ObjectUtils

// *******************
// * Ticket Contract *
// *******************
class TicketContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.cordatix.contracts.TicketContract"
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
        val allStates = tx.inputsOfType<TicketState>() + tx.outputsOfType<TicketState>()
        for (s in allStates) {
            requireThat {
                "Event on the ticket must exist." using (ObjectUtils.isNotEmpty(s.eventPointer))
                "Ticket price must be a non zero positive value." using (s.ticketPrice > 0.0)
            }
        }
    }

    private fun verifyStatusConstraints(tx: LedgerTransaction) {
        val allStates = tx.inputsOfType<TicketState>() + tx.outputsOfType<TicketState>()

        // Note, in kotlin non-nullable properties must be populated, hence only need to check the nullable properties of the AgreementState
        for (s in allStates) {
            when(s.status){
                TicketStatus.NEW -> {
                    requireThat {
                        "Holder must be same as issuer." using (s.holder == s.issuer)
                    }
                }
                TicketStatus.BOOKED -> {
                    requireThat {
                        "Holder must not be same as issuer." using (s.holder != s.issuer)
                    }
                }
            }
        }
    }

    private fun verifyLinearIDConstraints(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<TicketContract.Commands>()
        val inputStates = tx.inputsOfType<TicketState>()
        val outputStates = tx.outputsOfType<TicketState>()

        requireThat{
            "When using LinearStates there should be a maximum of one Primary input state." using (inputStates.size <= 1)
            "When using LinearStates there should be a maximum of one Primary output state." using (outputStates.size <= 1)
        }

        val inputState = inputStates.singleOrNull()
        val outputState = outputStates.singleOrNull()

        val commandName = command.value::class.java.simpleName
        when (command.value){
            is TicketContract.Commands.Book,
            is TicketContract.Commands.Redeem-> {
                requireThat {"When the Command is $commandName the LinearID must not change." using(inputState?.linearId == outputState?.linearId)}
            }
        }
    }

    private fun verifyCommandConstraints(tx: LedgerTransaction) {
        if(tx.commands.isEmpty()){
            throw IllegalArgumentException("One command Expected")
        }

        val command = tx.commands[0]
        when (command.value) {
            is TicketContract.Commands.Create -> requireThat {
                "Event on the ticket must be an upcoming event only." using (tx.referenceInputRefsOfType<EventState>().isNotEmpty()
                        && tx.referenceInputRefsOfType<EventState>()[0].state.data.status == EventStatus.UPCOMING)
            }
            is TicketContract.Commands.Book -> requireThat {
                "One Input Expected" using (tx.inputStates.size == 1)
                "One Output Expected" using (tx.outputStates.size == 1)
                val input = tx.inputsOfType<TicketState>()[0]
                val output = tx.outputsOfType<TicketState>()[0]
                "Event, price, category and issuer on the ticket must be same." using(output.eventPointer == input.eventPointer
                        && output.ticketPrice == input.ticketPrice
                        && output.ticketCategory == input.ticketCategory
                        && output.issuer == input.issuer)
                "Holder must be updated." using (output.holder != input.holder)
            }
            is TicketContract.Commands.Cancel -> requireThat {
                "One Input Expected" using (tx.inputStates.size == 1)
                "One Output Expected" using (tx.outputStates.size == 1)
                val input = tx.inputsOfType<TicketState>()[0]
                val output = tx.outputsOfType<TicketState>()[0]
                "Event, price, category and issuer on the ticket must be same." using(output.eventPointer == input.eventPointer
                        && output.ticketPrice == input.ticketPrice
                        && output.ticketCategory == input.ticketCategory
                        && output.issuer == input.issuer)
                "Holder must be updated." using (output.holder != input.holder)
            }
            is TicketContract.Commands.Redeem -> requireThat {
                "One Input Expected" using (tx.inputStates.size == 1)
                "One Output Expected" using (tx.outputStates.size == 1)
                val input = tx.inputsOfType<TicketState>()[0]
                val output = tx.outputsOfType<TicketState>()[0]
                "Event, price, category, holder and issuer on the ticket must be same." using(output.eventPointer == input.eventPointer
                        && output.ticketPrice == input.ticketPrice
                        && output.ticketCategory == input.ticketCategory
                        && output.issuer == input.issuer
                        && output.holder == input.holder)
                "Event on the ticket must be an ongoing event only." using (tx.referenceInputRefsOfType<EventState>().isNotEmpty()
                        && tx.referenceInputRefsOfType<EventState>()[0].state.data.status == EventStatus.ONGOING)
            }
            is EventContract.Commands.Complete -> requireThat {
                "One Input Expected" using (tx.inputStates.size == 1)
                "No Output Expected" using (tx.outputStates.isEmpty())
            }
        }
    }

    private fun verifySigningConstraints(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<TicketContract.Commands>()
        val commandName = command.value::class.java.simpleName
        val inputStates = tx.inputsOfType<TicketState>()
        val outputStates = tx.outputsOfType<TicketState>()
        val inputState = inputStates.singleOrNull()
        val outputState = outputStates.singleOrNull()
        fun checkSigner(signerDescription: String, signer: AbstractParty?){
            requireThat { "When the Command is $commandName the $signerDescription must sign." using (command.signers.contains(signer?.owningKey))}
        }
        when (command.value) {
            is TicketContract.Commands.Create-> {
                checkSigner("output.issuer", outputState?.issuer)
            }
            is TicketContract.Commands.Book,
            is TicketContract.Commands.Cancel-> {
                checkSigner("output.holder", outputState?.holder)
            }
            is TicketContract.Commands.Redeem-> {
                checkSigner("output.issuer", outputState?.issuer)
                checkSigner("output.holder", outputState?.holder)
            }
            is TicketContract.Commands.Complete -> {
                checkSigner("input.issuer", inputState?.issuer)
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Create : Commands
        class Book : Commands
        class Cancel : Commands
        class Redeem : Commands
        class Complete : Commands
    }
}