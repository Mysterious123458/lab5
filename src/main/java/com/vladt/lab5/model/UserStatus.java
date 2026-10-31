package com.vladt.lab5.model;

public enum UserStatus {
    ACTIVE,
    BLOCKED;

    public boolean isActive() {
        return this == ACTIVE;
    }
}
