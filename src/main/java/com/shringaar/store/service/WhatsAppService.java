package com.shringaar.store.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.shringaar.store.config.StoreProperties;
import com.shringaar.store.domain.Appointment;
import com.shringaar.store.domain.CustomerOrder;
import com.shringaar.store.domain.OrderItem;
import com.shringaar.store.domain.Product;

import org.springframework.stereotype.Service;

/**
 * Builds click-to-chat links (https://wa.me/&lt;number&gt;?text=...) with a
 * pre-filled message. Opening one starts a WhatsApp chat with the business on
 * phone, desktop app or WhatsApp Web; no WhatsApp API account is needed.
 */
@Service
public class WhatsAppService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private final StoreProperties store;
    private final Formats fmt;

    public WhatsAppService(StoreProperties store, Formats fmt) {
        this.store = store;
        this.fmt = fmt;
    }

    public String link(String message) {
        String number = store.whatsappNumber() == null ? "" : store.whatsappNumber().replaceAll("\\D", "");
        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8).replace("+", "%20");
        return "https://wa.me/" + number + "?text=" + encoded;
    }

    public String productMessage(Product p, String size, Integer quantity, String productUrl) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hi ").append(store.name()).append(", I'm interested in this piece:\n\n");
        sb.append("*").append(p.getName()).append("*");
        if (p.getSku() != null) sb.append(" (").append(p.getSku()).append(")");
        sb.append("\n");
        if (p.getDetail() != null && !p.getDetail().isBlank()) sb.append(p.getDetail()).append("\n");
        sb.append("Price: ").append(money(p.getPrice())).append("\n");
        if (size != null && !size.isBlank()) sb.append("Size: ").append(size).append("\n");
        if (quantity != null && quantity > 1) sb.append("Quantity: ").append(quantity).append("\n");
        sb.append(productUrl).append("\n\n");
        sb.append("Could you share availability, customisation options and delivery time?");
        return sb.toString();
    }

    public String cartMessage(Cart cart, String siteUrl) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hi ").append(store.name()).append(", I'd like to order these pieces:\n\n");
        int i = 1;
        for (CartLine line : cart.getLines()) {
            sb.append(i++).append(". *").append(line.getName()).append("*");
            if (line.getSku() != null) sb.append(" (").append(line.getSku()).append(")");
            if (line.getSize() != null) sb.append(" · Size ").append(line.getSize());
            sb.append(" · Qty ").append(line.getQuantity())
              .append(" · ").append(money(line.getLineTotal())).append("\n");
        }
        sb.append("\nTotal: ").append(money(cart.getSubtotal())).append("\n");
        sb.append(siteUrl);
        return sb.toString();
    }

    public String orderMessage(CustomerOrder order) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hi ").append(store.name()).append(", I just placed order *")
          .append(order.getReference()).append("* on your website.\n\n");
        for (OrderItem item : order.getItems()) {
            sb.append("• ").append(item.getProductName());
            if (item.getSize() != null) sb.append(" · Size ").append(item.getSize());
            sb.append(" · Qty ").append(item.getQuantity()).append("\n");
        }
        sb.append("\nTotal: ").append(money(order.getTotal())).append("\n");
        sb.append("Payment: ").append(order.getPaymentMethod().getLabel()).append("\n");
        sb.append("Name: ").append(order.getCustomerName());
        return sb.toString();
    }

    public String bookingMessage(Appointment a) {
        return "Hi " + store.name() + ", I booked an appointment on your website.\n\n"
                + "For: " + a.getGarment() + "\n"
                + "When: " + a.getDate().format(DAY) + ", " + a.getTime().format(TIME) + "\n"
                + "Type: " + a.getLocation().getLabel() + "\n"
                + "Name: " + a.getCustomerName() + "\n\n"
                + "Please confirm. Thank you!";
    }

    private String money(java.math.BigDecimal amount) {
        return fmt.money(amount);
    }
}
