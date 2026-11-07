package com.shringaar.store.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Business details shown across the site. Set them in application.properties
 * or with environment variables (e.g. STORE_WHATSAPP_NUMBER=919876543210).
 */
@ConfigurationProperties(prefix = "store")
public record StoreProperties(
        String name,
        String tagline,
        /** Digits only, with country code and no "+", e.g. 919876543210. */
        String whatsappNumber,
        String whatsappGreeting,
        String phone,
        String email,
        String address,
        String city,
        String instagramUrl,
        String currencySymbol,
        Admin admin,
        String uploadDir,
        /** Time zone the studio's appointment times are in, e.g. Asia/Kolkata. */
        String timezone,
        WhatsAppApi whatsappApi) {

    public record Admin(String username, String password) {}

    /**
     * WhatsApp Business Cloud API (Meta), used to send booking confirmations and
     * reminders automatically. Leave disabled until you have an access token,
     * phone number ID and approved message templates.
     */
    public record WhatsAppApi(
            boolean enabled,
            String accessToken,
            String phoneNumberId,
            String apiVersion,
            String language,
            String confirmationTemplate,
            String reminderTemplate) {}
}
