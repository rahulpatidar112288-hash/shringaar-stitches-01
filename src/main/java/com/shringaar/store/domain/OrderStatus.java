package com.shringaar.store.domain;

public enum OrderStatus {
    NEW("New"),
    CONFIRMED("Confirmed"),
    IN_PRODUCTION("In production"),
    READY("Ready"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    private final String label;

    OrderStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
