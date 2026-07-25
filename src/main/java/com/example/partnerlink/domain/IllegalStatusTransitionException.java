package com.example.partnerlink.domain;

public class IllegalStatusTransitionException extends RuntimeException {

    private final ApplicationStatus from;
    private final ApplicationStatus to;

    public IllegalStatusTransitionException(ApplicationStatus from, ApplicationStatus to) {
        super("Illegal status transition: " + from + " -> " + to);
        this.from = from;
        this.to = to;
    }

    public ApplicationStatus getFrom() {
        return from;
    }

    public ApplicationStatus getTo() {
        return to;
    }
}
