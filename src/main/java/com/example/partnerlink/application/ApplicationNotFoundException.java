package com.example.partnerlink.application;

public class ApplicationNotFoundException extends RuntimeException {

    public ApplicationNotFoundException(String applicationId) {
        super("Merchant application not found: " + applicationId);
    }
}
