package com.railway.service;

import com.railway.exception.InvalidPNRException;
import com.railway.model.Person;
import com.railway.model.Ticket;
import com.railway.repository.TicketRepository;

import java.util.List;

/** Read access to tickets with the ownership rule: users see their own, admins see all. */
public class TicketService {

    private final TicketRepository tickets;

    public TicketService(TicketRepository tickets) {
        this.tickets = tickets;
    }

    public Ticket find(Person requester, String pnr) throws InvalidPNRException {
        String key = pnr == null ? "" : pnr.trim();
        return tickets.findByPnr(key)
                .filter(t -> requester.isAdmin()
                        || (requester.getId() != null && t.getUserId() == requester.getId()))
                .orElseThrow(() -> new InvalidPNRException("No ticket " + key + " found for this account"));
    }

    public List<Ticket> forUser(Person user) {
        return tickets.findByUser(user.getId());
    }
}