package com.railway.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Payment {

    public enum Method { UPI, CARD, NETBANKING, WALLET }

    public enum Status { SUCCESS, FAILED, REFUNDED }

    private final Long id;                  // null until saved
    private final String pnr;
    private final BigDecimal amount;
    private final Method method;
    private Status status;
    private final LocalDateTime paidAt;     // null until loaded from the database

    public Payment(Long id, String pnr, BigDecimal amount, Method method,
                   Status status, LocalDateTime paidAt) {
        this.id = id;
        this.pnr = Require.text(pnr, "pnr");
        this.amount = Require.notNull(amount, "amount");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        this.method = Require.notNull(method, "method");
        this.status = Require.notNull(status, "status");
        this.paidAt = paidAt;
    }

    public Long getId() { return id; }
    public String getPnr() { return pnr; }
    public BigDecimal getAmount() { return amount; }
    public Method getMethod() { return method; }
    public Status getStatus() { return status; }
    public LocalDateTime getPaidAt() { return paidAt; }

    public void markRefunded() {
        this.status = Status.REFUNDED;
    }

    @Override
    public String toString() {
        return "Payment{pnr=" + pnr + ", " + amount + ", " + method + ", " + status + "}";
    }
}