package com.example.claims.service;

import com.example.claims.domain.ForbiddenException;
import com.example.claims.domain.Role;

/**
 * Who is making the request. In this sprint identity comes from X-User-Id / X-User-Role
 * headers (a deliberate stand-in for real JWT auth, see docs/DECISIONS.md); the service
 * layer enforces authorization against this object either way.
 */
public record Caller(String userId, Role role) {
    public void requireRole(Role required) {
        if (role != required) {
            throw new ForbiddenException("This action requires the " + required + " role");
        }
    }
    public boolean isStaff() { return role == Role.OFFICER || role == Role.MANAGER; }
    public void requireStaff() {
        if (!isStaff()) throw new ForbiddenException("Staff access only");
    }
}
