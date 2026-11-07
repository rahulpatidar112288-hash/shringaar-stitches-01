package com.shringaar.store.web.admin;

import java.math.BigDecimal;

import com.shringaar.store.domain.Product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.springframework.web.multipart.MultipartFile;

public class ProductForm {

    private Long id;
    private String sku;
    @NotBlank(message = "Name is required")
    private String name;
    @NotNull(message = "Choose a category")
    private Long categoryId;
    private String detail;
    private String description;
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price can't be negative")
    private BigDecimal price;
    private String imageUrl;
    private MultipartFile image;
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Use a hex colour like #F3E1E4")
    private String washColor = "#F3E1E4";
    private String sizes = "XS,S,M,L,XL,Custom";
    private boolean madeToOrder = true;
    private String leadTime = "3–4 weeks";
    private boolean featured;
    private boolean active = true;

    public static ProductForm from(Product p) {
        ProductForm f = new ProductForm();
        f.id = p.getId();
        f.sku = p.getSku();
        f.name = p.getName();
        f.categoryId = p.getCategory().getId();
        f.detail = p.getDetail();
        f.description = p.getDescription();
        f.price = p.getPrice();
        f.imageUrl = p.getImageUrl();
        f.washColor = p.getWashColor();
        f.sizes = p.getSizes();
        f.madeToOrder = p.isMadeToOrder();
        f.leadTime = p.getLeadTime();
        f.featured = p.isFeatured();
        f.active = p.isActive();
        return f;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public MultipartFile getImage() { return image; }
    public void setImage(MultipartFile image) { this.image = image; }
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
}
