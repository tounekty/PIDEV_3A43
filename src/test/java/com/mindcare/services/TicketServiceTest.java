package com.mindcare.services;

import com.mindcare.entities.Ticket;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TicketServiceTest {

    @Test
    void validStatusTransitionsShouldPass() {
        Assertions.assertTrue(TicketService.isValidStatusTransition(Ticket.Status.OPEN, Ticket.Status.IN_PROGRESS));
        Assertions.assertTrue(TicketService.isValidStatusTransition(Ticket.Status.IN_PROGRESS, Ticket.Status.WAITING_USER));
        Assertions.assertTrue(TicketService.isValidStatusTransition(Ticket.Status.WAITING_USER, Ticket.Status.CLOSED));
        Assertions.assertTrue(TicketService.isValidStatusTransition(Ticket.Status.CLOSED, Ticket.Status.CLOSED));
    }

    @Test
    void invalidStatusTransitionsShouldFail() {
        Assertions.assertFalse(TicketService.isValidStatusTransition(Ticket.Status.OPEN, Ticket.Status.CLOSED));
        Assertions.assertFalse(TicketService.isValidStatusTransition(Ticket.Status.IN_PROGRESS, Ticket.Status.CLOSED));
        Assertions.assertFalse(TicketService.isValidStatusTransition(Ticket.Status.WAITING_USER, Ticket.Status.OPEN));
        Assertions.assertFalse(TicketService.isValidStatusTransition(Ticket.Status.CLOSED, Ticket.Status.OPEN));
    }
}
