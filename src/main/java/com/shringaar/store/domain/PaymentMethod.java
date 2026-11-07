package com.shringaar.store.domain;

public enum PaymentMethod {
    CASH_ON_DELIVERY("Cash on delivery"),
    PAY_AT_STUDIO("Pay at the studio"),
    UPI_ON_WHATSAPP("UPI — we send a payment link on WhatsApp");

    private final String label;

    PaymentMethod(String label) { this.label = label; }

    public String getLabel() { return label; }
}
