package com.railway.service;

import com.railway.model.Payment;

import java.math.BigDecimal;

/** Simulated payment gateway: every valid request succeeds. */
public class PaymentService {

    public Payment process(String pnr, BigDecimal amount, Payment.Method method) {
        // The Payment constructor rejects a blank PNR, a null or non-positive amount, and a null method.
        return new Payment(null, pnr, amount, method, Payment.Status.SUCCESS, null);
    }
}