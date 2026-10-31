package com.vladt.lab5.model;

public enum OrderStatus {
    ASSIGNED,
    IN_PROGRESS,
    DONE,
    CANCELLED;

    public boolean isActive() {
        return this == ASSIGNED || this == IN_PROGRESS;
    }
}
