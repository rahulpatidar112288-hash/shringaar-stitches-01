package com.shringaar.store.service;

import java.util.List;

/** Sends WhatsApp messages from the business number without anyone pressing Send. */
public interface WhatsAppMessenger {

    /** False until the WhatsApp Business Cloud API is configured. */
    boolean isEnabled();

    /**
     * Sends an approved template message.
     *
     * @param to         customer number, digits with country code
     * @param template   template name as approved in Meta's WhatsApp Manager
     * @param parameters values for the template's {{1}}, {{2}}, ... placeholders, in order
     * @throws WhatsAppSendException when WhatsApp rejects the message
     */
    void sendTemplate(String to, String template, List<String> parameters);

    class WhatsAppSendException extends RuntimeException {
        public WhatsAppSendException(String message, Throwable cause) { super(message, cause); }
    }
}
