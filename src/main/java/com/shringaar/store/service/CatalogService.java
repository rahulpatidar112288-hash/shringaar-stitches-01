package com.shringaar.store.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.shringaar.store.domain.Category;
import com.shringaar.store.domain.Product;
import com.shringaar.store.repository.CategoryRepository;
import com.shringaar.store.repository.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final ProductRepository products;
    private final CategoryRepository categories;

    public CatalogService(ProductRepository products, CategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    public List<Category> categories() {
        return categories.findAllByOrderBySortOrderAscNameAsc();
    }

    public Optional<Category> category(String slug) {
        return categories.findBySlug(slug);
    }

    public List<Product> featured() {
        List<Product> featured = products.findByActiveTrueAndFeaturedTrueOrderByCreatedAtDesc();
        return featured.isEmpty() ? products.findByActiveTrueOrderByCreatedAtDesc().stream().limit(4).toList() : featured;
    }

    public List<Product> browse(Category category, String query) {
        if (query != null && !query.isBlank()) {
            List<Product> found = products.search(query.trim());
            return category == null ? found : found.stream().filter(p -> p.getCategory().getId().equals(category.getId())).toList();
        }
        return category == null
                ? products.findByActiveTrueOrderByCreatedAtDesc()
                : products.findByActiveTrueAndCategoryOrderByCreatedAtDesc(category);
    }

    public Optional<Product> product(String slug) {
        return products.findBySlugAndActiveTrue(slug);
    }

    public Optional<Product> product(Long id) {
        return products.findById(id).filter(Product::isActive);
    }

    public List<Product> related(Product p) {
        return products.findTop4ByActiveTrueAndCategoryAndIdNotOrderByCreatedAtDesc(p.getCategory(), p.getId());
    }

    /** "Rani Pink Lehenga!" → "rani-pink-lehenga", made unique with a numeric suffix. */
    public String uniqueSlug(String name, Long ignoreId) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isEmpty()) base = "product";
        String slug = base;
        int n = 2;
        while (true) {
            Optional<Product> clash = products.findBySlug(slug);
            if (clash.isEmpty() || clash.get().getId().equals(ignoreId)) return slug;
            slug = base + "-" + n++;
        }
    }

}
