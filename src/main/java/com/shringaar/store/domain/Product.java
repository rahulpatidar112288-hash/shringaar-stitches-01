package com.shringaar.store.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Short code customers can quote on WhatsApp, e.g. SS-0001. */
    @Column(unique = true)
    private String sku;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String slug;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Category category;

    /** One-line detail under the name, e.g. "Raw silk · zardozi". */
    private String detail;

    @Column(length = 4000)
    private String description;

    @NotNull
    @DecimalMin("0.0")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /** Relative (/uploads/..) or absolute image URL. Empty shows the arch placeholder. */
    private String imageUrl;

    /** Placeholder background behind the arch when there is no photo. */
    private String washColor = "#F3E1E4";

    /** Comma separated, e.g. "XS,S,M,L,XL,Custom". Empty means one size / made to measure. */
    private String sizes;

    /** Made-to-order pieces show a lead time instead of stock. */
    private boolean madeToOrder = true;
    private String leadTime = "3–4 weeks";

    private boolean featured;
    private boolean active = true;

    private Instant createdAt = Instant.now();

    public List<String> getSizeList() {
        if (sizes == null || sizes.isBlank()) return List.of();
        return Arrays.stream(sizes.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public boolean hasImage() {
        return imageUrl != null && !imageUrl.isBlank();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getWashColor() { return washColor; }
    public void setWashColor(String washColor) { this.washColor = washColor; }
    public String getSizes() { return sizes; }
    public void setSizes(String sizes) { this.sizes = sizes; }
    public boolean isMadeToOrder() { return madeToOrder; }
    public void setMadeToOrder(boolean madeToOrder) { this.madeToOrder = madeToOrder; }
    public String getLeadTime() { return leadTime; }
    public void setLeadTime(String leadTime) { this.leadTime = leadTime; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
}
