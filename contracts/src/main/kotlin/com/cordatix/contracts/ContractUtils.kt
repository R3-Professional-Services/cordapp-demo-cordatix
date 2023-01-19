package com.cordatix.contracts

import net.corda.core.contracts.ContractState
import net.corda.core.serialization.CordaSerializable

/**
 * ContractUtils.kt provides a set of classes, interfaces and helper functions which can be used in Corda Contracts
 * to simplify the implementation of the SMart Contract designs described in CorDapp Design Language (CDL) Smart Contract Diagrams
 *
 */

/**
 * The StatusState interface should be implemented for all [ContractState]s that require a status field.
 *
 * [status] is nullable so that when there is no input or output state in a transaction, the status can be represented as `null`
 *
 */
interface StatusState : ContractState {
    val status: Status?
}

/**
 * Statuses are defined as enum classes in the StatusState which should implement this Status interface.
 */
@CordaSerializable
interface Status