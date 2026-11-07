package com.shringaar.store.web;

import com.shringaar.store.domain.CustomerOrder;
import com.shringaar.store.domain.PaymentMethod;
import com.shringaar.store.repository.OrderRepository;
import com.shringaar.store.service.Cart;
import com.shringaar.store.service.CheckoutForm;
import com.shringaar.store.service.OrderService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CheckoutController {

    private final Cart cart;
    private final OrderService orderService;
    private final OrderRepository orders;

    public CheckoutController(Cart cart, OrderService orderService, OrderRepository orders) {
        this.cart = cart;
        this.orderService = orderService;
        this.orders = orders;
    }

    @ModelAttribute("paymentMethods")
    public PaymentMethod[] paymentMethods() {
        return PaymentMethod.values();
    }

    @GetMapping("/checkout")
    public String form(Model model) {
        if (cart.isEmpty()) return "redirect:/cart";
        model.addAttribute("form", new CheckoutForm());
        model.addAttribute("cart", cart);
        return "checkout";
    }

    @PostMapping("/checkout")
    public String place(@Valid @ModelAttribute("form") CheckoutForm form, BindingResult result,
                        Model model, HttpSession session, RedirectAttributes redirect) {
        if (cart.isEmpty()) return "redirect:/cart";
        if (result.hasErrors()) {
            model.addAttribute("cart", cart);
            return "checkout";
        }
        try {
            CustomerOrder order = orderService.placeOrder(form, cart);
            SessionKeys.remember(session, SessionKeys.ORDERS, order.getReference());
            return "redirect:/orders/" + order.getReference();
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/cart";
        }
    }

    @GetMapping("/orders/{reference}")
    public String confirmation(@PathVariable String reference, HttpSession session, Model model) {
        EnquiryController.requireOwned(session, SessionKeys.ORDERS, reference);
        CustomerOrder order = orders.findByReference(reference).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("order", order);
        model.addAttribute("whatsappLink", "/enquire/order/" + reference);
        return "order-confirmation";
    }
}
