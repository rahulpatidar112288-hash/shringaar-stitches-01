package com.shringaar.store.service;

/** Phone number helpers for WhatsApp, which wants digits with the country code and no "+". */
public final class Phones {

    private Phones() {}

    /** Indian 10-digit numbers (optionally with a leading 0) get the 91 code; others are used as typed. */
    public static String toWhatsAppNumber(String phone) {
        String digits = phone == null ? "" : phone.replaceAll("\\D", "");
        if (digits.length() == 11 && digits.startsWith("0")) digits = digits.substring(1);
        return digits.length() == 10 ? "91" + digits : digits;
    }
}
