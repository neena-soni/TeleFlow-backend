package com.teleflow.orchestrator_server.model.enums;

/**
 * Identifies each step in the activation saga.
 *
 * Execution order: INVENTORY → NETWORK → BILLING → NOTIFICATION
 * Compensation (rollback) order: BILLING → NETWORK → INVENTORY → NOTIFICATION (failure alert)
 */
public enum StepName {

    /** Reserve a physical port / SIM card in inventory-service. */
    INVENTORY,

    /** Provision VLAN / bandwidth in network-service. */
    NETWORK,

    /** Charge the customer and create a subscription in billing-service. */
    BILLING,

    /** Send SMS/email confirmation via notification-service. */
    NOTIFICATION
}
