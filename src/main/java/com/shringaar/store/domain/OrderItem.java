package com.shringaar.store.domain;

import java.math.BigDecimal;

import jakarta.persistence.*;

/** A line on an order. Name and price are copied so later catalogue edits don't change past orders. */
@Entity
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private CustomerOrder order;

    private Long productId;
    private String sku;
    private String productName;
    private String size;
    private int quantity;

    @Column(precision = 12, scale = 2)
    private BigDecimal unitPrice;

    protected OrderItem() {}

    public OrderItem(Long productId, String sku, String productName, String size, int quantity, BigDecimal unitPrice) {
        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.size = size;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() { return id; }
    public CustomerOrder getOrder() { return order; }
    void setOrder(CustomerOrder order) { this.order = order; }
    public Long getProductId() { return productId; }
    public String getSku() { return sku; }
    public String getProductName() { return productName; }
    public String getSize() { return size; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
}
