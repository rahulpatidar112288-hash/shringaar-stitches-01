package com.shringaar.store.service;

import java.math.BigDecimal;
import java.security.SecureRandom;

import com.shringaar.store.domain.CustomerOrder;
import com.shringaar.store.domain.OrderItem;
import com.shringaar.store.domain.Product;
import com.shringaar.store.repository.OrderRepository;
import com.shringaar.store.repository.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderRepository orders;
    private final ProductRepository products;

    public OrderService(OrderRepository orders, ProductRepository products) {
        this.orders = orders;
        this.products = products;
    }

    /**
     * Turns the cart into an order. Prices come from the catalogue, not the
     * session, and items that were removed from the catalogue are skipped.
     */
    @Transactional
    public CustomerOrder placeOrder(CheckoutForm form, Cart cart) {
        if (cart.isEmpty()) throw new IllegalStateException("Cart is empty");

        CustomerOrder order = new CustomerOrder();
        order.setReference(newReference());
        order.setCustomerName(form.getName().trim());
        order.setPhone(form.getPhone().trim());
        order.setEmail(form.getEmail());
        order.setAddress(form.getAddress());
        order.setCity(form.getCity());
        order.setPincode(form.getPincode());
        order.setNotes(form.getNotes());
        order.setPaymentMethod(form.getPaymentMethod());

        BigDecimal total = BigDecimal.ZERO;
        for (CartLine line : cart.getLines()) {
            Product p = products.findById(line.getProductId()).filter(Product::isActive).orElse(null);
            if (p == null) continue;
            OrderItem item = new OrderItem(p.getId(), p.getSku(), p.getName(), line.getSize(), line.getQuantity(), p.getPrice());
            order.addItem(item);
            total = total.add(item.getLineTotal());
        }
        if (order.getItems().isEmpty()) throw new IllegalStateException("None of the cart items are available any more");
        order.setTotal(total);

        CustomerOrder saved = orders.save(order);
        cart.clear();
        return saved;
    }

    private String newReference() {
        String ref;
        do {
            StringBuilder sb = new StringBuilder("SS-");
            for (int i = 0; i < 6; i++) sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            ref = sb.toString();
        } while (orders.existsByReference(ref));
        return ref;
    }
}
