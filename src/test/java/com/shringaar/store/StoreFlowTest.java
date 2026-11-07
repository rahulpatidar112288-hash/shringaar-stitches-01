package com.shringaar.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import com.shringaar.store.domain.Product;
import com.shringaar.store.repository.AppointmentRepository;
import com.shringaar.store.repository.EnquiryRepository;
import com.shringaar.store.repository.OrderRepository;
import com.shringaar.store.repository.ProductRepository;
import com.shringaar.store.service.BookingService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        "store.whatsapp-number=+91 98765 43210",
        "store.upload-dir=target/test-uploads"
})
@AutoConfigureMockMvc
class StoreFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;
    @Autowired EnquiryRepository enquiries;
    @Autowired AppointmentRepository appointments;
    @Autowired BookingService booking;

    private Product lehenga() {
        return products.findBySlugAndActiveTrue("rani-pink-lehenga").orElseThrow();
    }

    @Test
    void homeAndCataloguePagesRender() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Rani pink lehenga")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chat on WhatsApp")));
        mvc.perform(get("/collections").param("category", "suits")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ivory chikankari kurta set")));
        mvc.perform(get("/")).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsStringIgnoringCase("sherwani"))));
        mvc.perform(get("/collections").param("q", "gota")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Gota patti anarkali")));
        mvc.perform(get("/products/rani-pink-lehenga")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/enquire/product/rani-pink-lehenga")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("₹85,000")));
        mvc.perform(get("/products/does-not-exist")).andExpect(status().isNotFound());
    }

    @Test
    void productEnquiryRedirectsToWhatsAppWithPrefilledMessage() throws Exception {
        long before = enquiries.count();
        MvcResult res = mvc.perform(get("/enquire/product/rani-pink-lehenga").param("size", "M").param("qty", "2"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String location = res.getResponse().getRedirectedUrl();
        assertThat(location).startsWith("https://wa.me/919876543210?text=");
        assertThat(location).doesNotContain("+");
        String text = URLDecoder.decode(location.substring(location.indexOf("text=") + 5), StandardCharsets.UTF_8);
        assertThat(text).contains("*Rani pink lehenga* (SS-0001)", "Size: M", "Quantity: 2",
                "http://localhost/products/rani-pink-lehenga", "₹85,000");
        assertThat(enquiries.count()).isEqualTo(before + 1);
    }

    @Test
    void cartCheckoutCreatesOrderAndWhatsAppConfirmation() throws Exception {
        MockHttpSession session = new MockHttpSession();
        Product p = lehenga();

        // size is required for sized products
        mvc.perform(post("/cart/add").session(session).with(csrf()).param("productId", p.getId().toString()))
                .andExpect(redirectedUrl("/products/rani-pink-lehenga"));

        mvc.perform(post("/cart/add").session(session).with(csrf())
                        .param("productId", p.getId().toString()).param("size", "M").param("qty", "2"))
                .andExpect(redirectedUrl("/cart"));
        mvc.perform(get("/cart").session(session)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("₹1,70,000")));

        mvc.perform(get("/enquire/cart").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl()).startsWith("https://wa.me/919876543210"));

        // validation errors re-render the form
        mvc.perform(post("/checkout").session(session).with(csrf()).param("name", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tell us your name")));

        MvcResult placed = mvc.perform(post("/checkout").session(session).with(csrf())
                        .param("name", "Priya Sharma").param("phone", "+91 98765 00000")
                        .param("address", "12 MG Road").param("city", "Jaipur").param("pincode", "302001")
                        .param("paymentMethod", "CASH_ON_DELIVERY"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String confirmUrl = placed.getResponse().getRedirectedUrl();
        String ref = confirmUrl.substring("/orders/".length());

        var order = orders.findByReference(ref).orElseThrow();
        assertThat(order.getTotal()).isEqualByComparingTo("170000");
        assertThat(order.getItems()).hasSize(1);

        mvc.perform(get(confirmUrl).session(session)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(ref)));
        // another visitor can't open someone else's confirmation
        mvc.perform(get(confirmUrl)).andExpect(status().isNotFound());

        mvc.perform(get("/enquire/order/" + ref).session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(URLDecoder.decode(result.getResponse().getRedirectedUrl(), StandardCharsets.UTF_8))
                        .contains(ref, "Cash on delivery"));

        // cart is emptied
        mvc.perform(get("/checkout").session(session)).andExpect(redirectedUrl("/cart"));
    }

    @Test
    void bookingAFitting() throws Exception {
        MockHttpSession session = new MockHttpSession();
        LocalDate day = booking.openDays().get(2);
        mvc.perform(get("/book-a-fitting")).andExpect(status().isOk());

        MvcResult res = mvc.perform(post("/book-a-fitting").session(session).with(csrf())
                        .param("garment", "Anarkali").param("date", day.toString()).param("time", "15:00")
                        .param("location", "STUDIO").param("customerName", "Meera").param("phone", "9876500000")
                        .param("confirmed", "true"))
                .andExpect(status().is3xxRedirection()).andReturn();
        String url = res.getResponse().getRedirectedUrl();
        mvc.perform(get(url).session(session)).andExpect(status().isOk());
        assertThat(appointments.findAll()).anySatisfy(a -> {
            assertThat(a.getCustomerName()).isEqualTo("Meera");
            assertThat(a.isConfirmed()).as("visitors can't mark their own booking confirmed").isFalse();
        });

        // the same slot can't be double booked
        mvc.perform(post("/book-a-fitting").with(csrf())
                        .param("garment", "Anarkali").param("date", day.toString()).param("time", "15:00")
                        .param("location", "STUDIO").param("customerName", "Asha").param("phone", "9876511111"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("just booked")));
    }

    @Test
    void adminIsProtected() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/admin/login"));
        mvc.perform(get("/admin").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/admin/products").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/admin/enquiries").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    void jsonApiListsProducts() throws Exception {
        mvc.perform(get("/api/products").param("category", "bridal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category.slug").value("bridal"))
                .andExpect(jsonPath("$[0].whatsappEnquiryUrl").exists());
    }

    @Test
    void postFormsCarryCsrfToken() throws Exception {
        for (String url : new String[] {"/products/rani-pink-lehenga", "/book-a-fitting", "/admin/login"}) {
            mvc.perform(get(url)).andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        }
    }

    @Test
    void unknownPageShowsBrandedError() throws Exception {
        mvc.perform(get("/no-such-page").accept("text/html")).andExpect(status().isNotFound());
    }
}
