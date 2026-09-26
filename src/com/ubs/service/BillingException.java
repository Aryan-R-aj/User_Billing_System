package com.ubs.service;

/**
 * Thrown when a requested operation violates a business rule of the
 * system (e.g. starting usage on a resource that is already at full
 * capacity, or stopping a session that does not exist).
 */
public class BillingException extends Exception {
    public BillingException(String message) {
        super(message);
    }
}
