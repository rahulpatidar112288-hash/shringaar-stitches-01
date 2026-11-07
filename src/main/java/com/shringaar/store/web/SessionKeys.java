package com.shringaar.store.web;

import java.util.HashSet;
import java.util.Set;

import jakarta.servlet.http.HttpSession;

/**
 * Orders and bookings a visitor made in this browser session. Confirmation
 * pages are only shown to that session, so a guessed reference reveals nothing.
 */
final class SessionKeys {

    static final String ORDERS = "placedOrders";
    static final String BOOKINGS = "placedBookings";

    private SessionKeys() {}

    @SuppressWarnings("unchecked")
    static void remember(HttpSession session, String key, String value) {
        Set<String> set = (Set<String>) session.getAttribute(key);
        if (set == null) set = new HashSet<>();
        set.add(value);
        session.setAttribute(key, set);
    }
}
