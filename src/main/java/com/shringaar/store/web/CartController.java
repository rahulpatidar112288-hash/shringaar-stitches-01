package com.shringaar.store.web;

import com.shringaar.store.domain.Product;
import com.shringaar.store.service.Cart;
import com.shringaar.store.service.CatalogService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CartController {

    private final Cart cart;
    private final CatalogService catalog;

    public CartController(Cart cart, CatalogService catalog) {
        this.cart = cart;
        this.catalog = catalog;
    }

    @GetMapping("/cart")
    public String view(Model model) {
        model.addAttribute("cart", cart);
        if (!cart.isEmpty()) model.addAttribute("whatsappLink", "/enquire/cart");
        return "cart";
    }

    @PostMapping("/cart/add")
    public String add(@RequestParam Long productId,
                      @RequestParam(required = false) String size,
                      @RequestParam(defaultValue = "1") int qty,
                      @RequestParam(defaultValue = "add") String action,
                      RedirectAttributes redirect) {
        Product p = catalog.product(productId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!p.getSizeList().isEmpty() && (size == null || !p.getSizeList().contains(size))) {
            redirect.addFlashAttribute("error", "Please choose a size.");
            return "redirect:/products/" + p.getSlug();
        }
        cart.add(p, size, qty);
        if ("buy".equals(action)) return "redirect:/checkout";
        redirect.addFlashAttribute("added", p.getName());
        return "redirect:/cart";
    }

    @PostMapping("/cart/update")
    public String update(@RequestParam int index, @RequestParam int qty) {
        cart.update(index, qty);
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String remove(@RequestParam int index) {
        cart.remove(index);
        return "redirect:/cart";
    }
}
