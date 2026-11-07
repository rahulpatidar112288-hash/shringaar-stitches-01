package com.shringaar.store.api;

import java.math.BigDecimal;
import java.util.List;

import com.shringaar.store.domain.Category;
import com.shringaar.store.domain.Product;
import com.shringaar.store.service.CatalogService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Read-only JSON catalogue for a future mobile app or marketplace feeds. */
@RestController
@RequestMapping("/api")
public class ProductApiController {

    public record CategoryDto(String name, String slug) {
        static CategoryDto of(Category c) { return new CategoryDto(c.getName(), c.getSlug()); }
    }

    public record ProductDto(String sku, String name, String slug, CategoryDto category, String detail,
                             String description, BigDecimal price, String imageUrl, List<String> sizes,
                             boolean madeToOrder, String leadTime, String whatsappEnquiryUrl) {
        static ProductDto of(Product p) {
            return new ProductDto(p.getSku(), p.getName(), p.getSlug(), CategoryDto.of(p.getCategory()), p.getDetail(),
                    p.getDescription(), p.getPrice(), p.getImageUrl(), p.getSizeList(), p.isMadeToOrder(),
                    p.getLeadTime(), "/enquire/product/" + p.getSlug());
        }
    }

    private final CatalogService catalog;

    public ProductApiController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/categories")
    public List<CategoryDto> categories() {
        return catalog.categories().stream().map(CategoryDto::of).toList();
    }

    @GetMapping("/products")
    public List<ProductDto> products(@RequestParam(required = false) String category,
                                     @RequestParam(required = false) String q) {
        Category c = category == null ? null
                : catalog.category(category).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return catalog.browse(c, q).stream().map(ProductDto::of).toList();
    }

    @GetMapping("/products/{slug}")
    public ProductDto product(@PathVariable String slug) {
        return catalog.product(slug).map(ProductDto::of)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
