package com.shringaar.store.config;

import com.shringaar.store.service.Cart;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Values every page template needs: store details, cart badge, default WhatsApp link. */
@ControllerAdvice
public class GlobalModelAdvice {

    private final StoreProperties store;
    private final Cart cart;

    public GlobalModelAdvice(StoreProperties store, Cart cart) {
        this.store = store;
        this.cart = cart;
    }

    @ModelAttribute("store")
    public StoreProperties store() {
        return store;
    }

    @ModelAttribute("cartCount")
    public int cartCount() {
        return cart.getItemCount();
    }

    /**
     * Target of the floating WhatsApp button. Pages override it (a product page
     * points at that product's enquiry). Links go through /enquire so each click
     * is logged before the visitor is redirected to WhatsApp.
     */
    @ModelAttribute("whatsappLink")
    public String whatsappLink() {
        return "/enquire";
    }
}
