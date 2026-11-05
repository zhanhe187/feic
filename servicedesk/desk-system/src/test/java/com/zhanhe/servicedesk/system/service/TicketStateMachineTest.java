package com.zhanhe.servicedesk.system.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TicketStateMachineTest {

    @Test
    void allowPendingToProcessing() {
        assertDoesNotThrow(() -> TicketStateMachine.assertTransition(
                TicketStateMachine.STATUS_PENDING,
                TicketStateMachine.STATUS_PROCESSING));
    }

    @Test
    void rejectClosedToProcessing() {
        assertThrows(IllegalStateException.class, () -> TicketStateMachine.assertTransition(
                TicketStateMachine.STATUS_CLOSED,
                TicketStateMachine.STATUS_PROCESSING));
    }

    @Test
    void rejectCompletedToRejected() {
        assertThrows(IllegalStateException.class, () -> TicketStateMachine.assertTransition(
                TicketStateMachine.STATUS_COMPLETED,
                TicketStateMachine.STATUS_REJECTED));
    }
}
