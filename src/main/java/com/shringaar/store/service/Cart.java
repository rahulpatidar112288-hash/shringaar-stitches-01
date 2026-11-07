package com.shringaar.store.service;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.shringaar.store.domain.Product;

import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

/** The visitor's shopping bag, kept in their HTTP session. */
@Component
@SessionScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class Cart implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final int MAX_QUANTITY = 10;

    private final List<CartLine> lines = new ArrayList<>();

    public synchronized void add(Product product, String size, int quantity) {
        String normalizedSize = (size == null || size.isBlank()) ? null : size.trim();
        int qty = Math.max(1, Math.min(quantity, MAX_QUANTITY));
        for (CartLine line : lines) {
            if (line.getProductId().equals(product.getId()) && Objects.equals(line.getSize(), normalizedSize)) {
                line.setQuantity(Math.min(line.getQuantity() + qty, MAX_QUANTITY));
                return;
            }
        }
        lines.add(new CartLine(product, normalizedSize, qty));
    }

    public synchronized void update(int index, int quantity) {
        if (index < 0 || index >= lines.size()) return;
        if (quantity <= 0) {
            lines.remove(index);
        } else {
            lines.get(index).setQuantity(Math.min(quantity, MAX_QUANTITY));
        }
    }

    public synchronized void remove(int index) {
        if (index >= 0 && index < lines.size()) lines.remove(index);
    }

    public synchronized void clear() {
        lines.clear();
    }

    public synchronized List<CartLine> getLines() {
        return Collections.unmodifiableList(new ArrayList<>(lines));
    }

    public synchronized boolean isEmpty() {
        return lines.isEmpty();
    }

    public synchronized int getItemCount() {
        return lines.stream().mapToInt(CartLine::getQuantity).sum();
    }

    public synchronized BigDecimal getSubtotal() {
        return lines.stream().map(CartLine::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
