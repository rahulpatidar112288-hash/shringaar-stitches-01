package com.shringaar.store.web;

import java.util.Set;

import com.shringaar.store.config.StoreProperties;
import com.shringaar.store.domain.Appointment;
import com.shringaar.store.domain.CustomerOrder;
import com.shringaar.store.domain.Enquiry;
import com.shringaar.store.domain.Product;
import com.shringaar.store.repository.AppointmentRepository;
import com.shringaar.store.repository.EnquiryRepository;
import com.shringaar.store.repository.OrderRepository;
import com.shringaar.store.service.Cart;
import com.shringaar.store.service.CatalogService;
import com.shringaar.store.service.WhatsAppService;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Every "chat on WhatsApp" button lands here: we log the enquiry, then
 * redirect to wa.me with a message pre-filled for that product, cart, order
 * or booking. The visitor only has to press Send in WhatsApp.
 */
@Controller
public class EnquiryController {

    private final StoreProperties store;
    private final WhatsAppService whatsApp;
    private final CatalogService catalog;
    private final EnquiryRepository enquiries;
    private final OrderRepository orders;
    private final AppointmentRepository appointments;
    private final Cart cart;

    public EnquiryController(StoreProperties store, WhatsAppService whatsApp, CatalogService catalog,
                             EnquiryRepository enquiries, OrderRepository orders,
                             AppointmentRepository appointments, Cart cart) {
        this.store = store;
        this.whatsApp = whatsApp;
        this.catalog = catalog;
        this.enquiries = enquiries;
        this.orders = orders;
        this.appointments = appointments;
        this.cart = cart;
    }

    @GetMapping("/enquire")
    public String general() {
        String message = store.whatsappGreeting();
        enquiries.save(new Enquiry("general", null, null, null, message));
        return redirect(message);
    }

    @GetMapping("/enquire/product/{slug}")
    public String product(@PathVariable String slug,
                          @RequestParam(required = false) String size,
                          @RequestParam(required = false) Integer qty) {
        Product p = catalog.product(slug).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String validSize = size != null && p.getSizeList().contains(size) ? size : null;
        String url = ServletUriComponentsBuilder.fromCurrentContextPath().path("/products/{slug}").buildAndExpand(slug).toUriString();
        String message = whatsApp.productMessage(p, validSize, qty, url);
        enquiries.save(new Enquiry("product", p.getId(), p.getName(), validSize, message));
        return redirect(message);
    }

    @GetMapping("/enquire/cart")
    public String cart() {
        if (cart.isEmpty()) return "redirect:/enquire";
        String siteUrl = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
        String message = whatsApp.cartMessage(cart, siteUrl);
        enquiries.save(new Enquiry("cart", null, null, null, message));
        return redirect(message);
    }

    @GetMapping("/enquire/order/{reference}")
    public String order(@PathVariable String reference, HttpSession session) {
        requireOwned(session, SessionKeys.ORDERS, reference);
        CustomerOrder order = orders.findByReference(reference).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String message = whatsApp.orderMessage(order);
        enquiries.save(new Enquiry("order", null, order.getReference(), null, message));
        return redirect(message);
    }

    @GetMapping("/enquire/booking/{id}")
    public String booking(@PathVariable Long id, HttpSession session) {
        requireOwned(session, SessionKeys.BOOKINGS, String.valueOf(id));
        Appointment a = appointments.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String message = whatsApp.bookingMessage(a);
        enquiries.save(new Enquiry("booking", null, a.getGarment(), null, message));
        return redirect(message);
    }

    private String redirect(String message) {
        return "redirect:" + whatsApp.link(message);
    }

    static void requireOwned(HttpSession session, String key, String value) {
        @SuppressWarnings("unchecked")
        Set<String> owned = (Set<String>) session.getAttribute(key);
        if (owned == null || !owned.contains(value)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
