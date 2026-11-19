package com.example.claims.domain;

public class InvalidTransitionException extends RuntimeException {
    public InvalidTransitionException(ClaimStatus from, ClaimStatus to) {
        super("Cannot move claim from " + from + " to " + to);
    }
}
