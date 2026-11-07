package com.shringaar.store.domain;

import java.time.Instant;

import jakarta.persistence.*;

/**
 * A record of each WhatsApp enquiry click, so the team can see which products
 * draw interest even though the conversation itself happens in WhatsApp.
 */
@Entity
public class Enquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** product, cart, order, booking or general */
    private String source;
    private Long productId;
    private String productName;
    private String size;
    @Column(length = 2000)
    private String message;
    private Instant createdAt = Instant.now();

    protected Enquiry() {}

    public Enquiry(String source, Long productId, String productName, String size, String message) {
        this.source = source;
        this.productId = productId;
        this.productName = productName;
        this.size = size;
        this.message = message;
    }

    public Long getId() { return id; }
    public String getSource() { return source; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getSize() { return size; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
}
