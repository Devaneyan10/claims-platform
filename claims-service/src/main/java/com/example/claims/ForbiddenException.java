package com.example.claims.domain;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) { super(message); }
}
