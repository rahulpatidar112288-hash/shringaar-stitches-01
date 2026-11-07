package com.shringaar.store.service;

import java.util.List;
import java.util.Map;

import com.shringaar.store.config.StoreProperties;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * WhatsApp Business Cloud API client (https://developers.facebook.com/docs/whatsapp/cloud-api).
 * Business-initiated messages such as reminders must use templates approved by Meta.
 */
@Component
public class CloudApiWhatsAppMessenger implements WhatsAppMessenger {

    private final StoreProperties.WhatsAppApi config;
    private final RestClient http;

    public CloudApiWhatsAppMessenger(StoreProperties props, RestClient.Builder builder) {
        this.config = props.whatsappApi();
        this.http = builder.baseUrl("https://graph.facebook.com").build();
    }

    @Override
    public boolean isEnabled() {
        return config != null && config.enabled()
                && notBlank(config.accessToken()) && notBlank(config.phoneNumberId());
    }

    @Override
    public void sendTemplate(String to, String template, List<String> parameters) {
        List<Map<String, String>> params = parameters.stream()
                .map(p -> Map.of("type", "text", "text", p))
                .toList();
        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "template",
                "template", Map.of(
                        "name", template,
                        "language", Map.of("code", config.language()),
                        "components", List.of(Map.of("type", "body", "parameters", params))));
        try {
            http.post()
                    .uri("/{version}/{phoneNumberId}/messages", config.apiVersion(), config.phoneNumberId())
                    .headers(h -> h.setBearerAuth(config.accessToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new WhatsAppSendException("WhatsApp API " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString(), e);
        } catch (RuntimeException e) {
            throw new WhatsAppSendException("WhatsApp API unreachable: " + e.getMessage(), e);
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
