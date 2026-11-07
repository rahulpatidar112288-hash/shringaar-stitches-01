package com.shringaar.store.web;

import com.shringaar.store.service.CatalogService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final CatalogService catalog;

    public HomeController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("featured", catalog.featured());
        model.addAttribute("categories", catalog.categories());
        return "index";
    }
}
