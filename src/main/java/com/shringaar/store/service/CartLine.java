package com.shringaar.store.service;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

import com.shringaar.store.domain.Product;

/** A product snapshot in the cart; the price is re-read from the catalogue at checkout. */
public class CartLine implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long productId;
    private final String sku;
    private final String name;
    private final String slug;
    private final String detail;
    private final String imageUrl;
    private final String washColor;
    private final String size;
    private final BigDecimal unitPrice;
    private int quantity;

    CartLine(Product p, String size, int quantity) {
        this.productId = p.getId();
        this.sku = p.getSku();
        this.name = p.getName();
        this.slug = p.getSlug();
        this.detail = p.getDetail();
        this.imageUrl = p.getImageUrl();
        this.washColor = p.getWashColor();
        this.size = size;
        this.unitPrice = p.getPrice();
        this.quantity = quantity;
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public boolean hasImage() {
        return imageUrl != null && !imageUrl.isBlank();
    }

    public Long getProductId() { return productId; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getDetail() { return detail; }
    public String getImageUrl() { return imageUrl; }
    public String getWashColor() { return washColor; }
    public String getSize() { return size; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    void setQuantity(int quantity) { this.quantity = quantity; }
}
