package com.shringaar.store.web;

import com.shringaar.store.domain.Category;
import com.shringaar.store.domain.Product;
import com.shringaar.store.service.CatalogService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/collections")
    public String collections(@RequestParam(required = false) String category,
                              @RequestParam(required = false) String q,
                              Model model) {
        Category selected = category == null || category.isBlank() ? null
                : catalog.category(category).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("categories", catalog.categories());
        model.addAttribute("selected", selected);
        model.addAttribute("q", q);
        model.addAttribute("products", catalog.browse(selected, q));
        return "collections";
    }

    @GetMapping("/products/{slug}")
    public String product(@PathVariable String slug, Model model) {
        Product product = catalog.product(slug).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("product", product);
        model.addAttribute("related", catalog.related(product));
        model.addAttribute("whatsappLink", "/enquire/product/" + product.getSlug());
        return "product";
    }
}
