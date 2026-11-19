package com.example.claims.domain;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) { super(message); }
}
