package com.railway.service;

import com.railway.exception.InvalidInputException;
import com.railway.exception.SeatNotAvailableException;
import com.railway.exception.TrainNotFoundException;
import com.railway.model.BookingStatus;
import com.railway.model.Passenger;
import com.railway.model.Payment;
import com.railway.model.Route;
import com.railway.model.SeatClass;
import com.railway.model.Ticket;
import com.railway.model.Train;
import com.railway.repository.TicketRepository;
import com.railway.repository.TrainRepository;
import com.railway.util.PNRGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

public class BookingService {

    public static final int MAX_PASSENGERS = 6;

    private final TrainRepository trains;
    private final TicketRepository tickets;
    private final FareCalculator fareCalculator;
    private final PaymentService paymentService;

    public BookingService(TrainRepository trains, TicketRepository tickets,
                          FareCalculator fareCalculator, PaymentService paymentService) {
        this.trains = trains;
        this.tickets = tickets;
        this.fareCalculator = fareCalculator;
        this.paymentService = paymentService;
    }

    /** Checks everything that can be checked up front, prices the trip, then books atomically. */
    public Ticket book(long userId, int trainNumber, String from, String to, LocalDate journeyDate,
                       SeatClass seatClass, List<Passenger> passengers, Payment.Method paymentMethod)
            throws InvalidInputException, TrainNotFoundException, SeatNotAvailableException {

        if (passengers == null || passengers.isEmpty() || passengers.size() > MAX_PASSENGERS) {
            throw new InvalidInputException("Between 1 and " + MAX_PASSENGERS + " passengers are allowed");
        }
        if (seatClass == null || paymentMethod == null) {
            throw new InvalidInputException("Seat class and payment method are required");
        }
        String fromCode = from == null ? "" : from.trim().toUpperCase();
        String toCode = to == null ? "" : to.trim().toUpperCase();
        if (fromCode.isEmpty() || toCode.isEmpty() || fromCode.equals(toCode)) {
            throw new InvalidInputException("From and to stations must be given and must differ");
        }
        TrainService.requireBookableDate(journeyDate);

        Train train = trains.findByNumber(trainNumber)
                .filter(Train::isActive)
                .orElseThrow(() -> new TrainNotFoundException("Train " + trainNumber + " not found"));

        Route route = train.getRoute();
        if (route == null || !route.connects(fromCode, toCode)) {
            throw new InvalidInputException("Train " + trainNumber + " does not run from " + fromCode + " to " + toCode);
        }
        if (!train.runsOn(journeyDate)) {
            throw new InvalidInputException("Train " + trainNumber + " does not run on "
                    + journeyDate + " (" + journeyDate.getDayOfWeek() + ")");
        }
        if (!train.getTrainClass(seatClass).isPresent()) {
            throw new InvalidInputException("Train " + trainNumber + " has no " + seatClass.getCode() + " class");
        }
        ZonedDateTime departure = TrainService.departureOf(route, fromCode, journeyDate);
        if (!departure.isAfter(ZonedDateTime.now(TrainService.IST))) {
            throw new InvalidInputException("The train has already left " + fromCode);
        }

        BigDecimal fare = fareCalculator.calculate(train, seatClass, fromCode, toCode, passengers.size());
        String pnr = newUniquePnr();

        Ticket ticket = new Ticket(pnr, userId, trainNumber, seatClass, journeyDate, fromCode, toCode,
                BookingStatus.CONFIRMED, fare, null, null, null);
        for (Passenger p : passengers) {
            ticket.addPassenger(p);
        }

        // Simulated payment. A real gateway would authorise only after the seats are held.
        Payment payment = paymentService.process(pnr, fare, paymentMethod);

        tickets.book(ticket, payment);   // one DB transaction; throws SeatNotAvailableException
        return ticket;
    }

    private String newUniquePnr() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String pnr = PNRGenerator.generate();
            if (!tickets.pnrExists(pnr)) {
                return pnr;
            }
        }
        throw new IllegalStateException("Could not generate a unique PNR");
    }
}