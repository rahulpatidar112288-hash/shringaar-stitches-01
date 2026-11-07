package com.shringaar.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.shringaar.store.domain.Appointment;
import com.shringaar.store.repository.AppointmentRepository;
import com.shringaar.store.service.AppointmentNotifier;
import com.shringaar.store.service.BookingService;
import com.shringaar.store.service.WhatsAppMessenger;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:reminders;DB_CLOSE_DELAY=-1",
        "store.upload-dir=target/test-uploads",
        "store.whatsapp-api.confirmation-template=appointment_confirmation",
        "store.whatsapp-api.reminder-template=appointment_reminder"
})
@AutoConfigureMockMvc
class AppointmentReminderTest {

    record Sent(String to, String template, List<String> params) {}

    /** Records messages instead of calling Meta's API. */
    static class FakeMessenger implements WhatsAppMessenger {
        final List<Sent> sent = new ArrayList<>();
        @Override public boolean isEnabled() { return true; }
        @Override public void sendTemplate(String to, String template, List<String> parameters) {
            sent.add(new Sent(to, template, parameters));
        }
    }

    @TestConfiguration
    static class Config {
        @Bean @Primary FakeMessenger fakeMessenger() { return new FakeMessenger(); }
    }

    @Autowired MockMvc mvc;
    @Autowired FakeMessenger messenger;
    @Autowired AppointmentNotifier notifier;
    @Autowired AppointmentRepository appointments;
    @Autowired BookingService booking;
    @Autowired com.shringaar.store.config.StoreProperties storeProperties;
    @Autowired com.shringaar.store.service.Formats formats;

    @BeforeEach
    void reset() {
        messenger.sent.clear();
        appointments.deleteAll();
    }

    @Test
    void bookingPageOffersStudioVideoAndPhoneButNotHomeVisits() throws Exception {
        mvc.perform(get("/book-a-fitting")).andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Video call")))
                .andExpect(content().string(Matchers.containsString("Phone call")))
                .andExpect(content().string(Matchers.containsString("Studio fitting")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("HOME_VISIT"))));

        mvc.perform(post("/book-a-fitting").with(csrf())
                        .param("garment", "Anarkali").param("date", booking.openDays().get(1).toString())
                        .param("time", "11:30").param("location", "HOME_VISIT")
                        .param("customerName", "Asha").param("phone", "9876511111"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Choose a studio visit, video call or phone call")));
        assertThat(appointments.count()).isZero();
    }

    @Test
    void videoCallBookingSendsConfirmationThenDayAndHourReminders() throws Exception {
        LocalDate day = booking.openDays().get(3);
        mvc.perform(post("/book-a-fitting").with(csrf())
                        .param("garment", "Bridal lehenga").param("date", day.toString()).param("time", "16:30")
                        .param("location", "VIDEO_CALL").param("customerName", "Priya")
                        .param("phone", "98765 43210").param("whatsappOptIn", "true").param("_whatsappOptIn", "on"))
                .andExpect(status().is3xxRedirection());

        assertThat(messenger.sent).hasSize(1);
        Sent confirmation = messenger.sent.getFirst();
        assertThat(confirmation.to()).isEqualTo("919876543210");
        assertThat(confirmation.template()).isEqualTo("appointment_confirmation");
        assertThat(confirmation.params()).hasSize(4);
        assertThat(confirmation.params().get(0)).isEqualTo("Priya");
        assertThat(confirmation.params().get(1)).isEqualTo("video call consultation for your bridal lehenga");
        assertThat(confirmation.params().get(2)).contains("4:30 pm");
        assertThat(confirmation.params().get(3)).contains("video call you on WhatsApp");

        Appointment a = appointments.findAll().getFirst();
        Instant start = notifier.startOf(a);

        // two days out: nothing due
        assertThat(notifier.sendDueReminders(start.minus(Duration.ofHours(30)))).isZero();
        // within 24 hours: day-before reminder, once only
        assertThat(notifier.sendDueReminders(start.minus(Duration.ofHours(20)))).isEqualTo(1);
        assertThat(notifier.sendDueReminders(start.minus(Duration.ofHours(19)))).isZero();
        // within the last hour: 1-hour reminder, once only
        assertThat(notifier.sendDueReminders(start.minus(Duration.ofMinutes(55)))).isEqualTo(1);
        assertThat(notifier.sendDueReminders(start.minus(Duration.ofMinutes(50)))).isZero();
        // after the start: nothing more
        assertThat(notifier.sendDueReminders(start.plus(Duration.ofMinutes(5)))).isZero();

        assertThat(messenger.sent).extracting(Sent::template)
                .containsExactly("appointment_confirmation", "appointment_reminder", "appointment_reminder");
    }

    @Test
    void customersWhoOptOutGetNoAutomaticMessages() throws Exception {
        LocalDate day = booking.openDays().get(2);
        // unchecked checkbox: only Spring's "_whatsappOptIn" marker is sent
        mvc.perform(post("/book-a-fitting").with(csrf())
                        .param("garment", "Anarkali").param("date", day.toString()).param("time", "10:00")
                        .param("location", "PHONE_CALL").param("customerName", "Meera")
                        .param("phone", "9876500000").param("_whatsappOptIn", "on"))
                .andExpect(status().is3xxRedirection());

        Appointment a = appointments.findAll().getFirst();
        assertThat(a.isWhatsappOptIn()).isFalse();
        notifier.sendDueReminders(notifier.startOf(a).minus(Duration.ofMinutes(30)));
        assertThat(messenger.sent).isEmpty();
    }

    @Test
    void failedSendIsRecordedForStaffAndNotRetriedEndlessly() throws Exception {
        WhatsAppMessenger failing = new WhatsAppMessenger() {
            @Override public boolean isEnabled() { return true; }
            @Override public void sendTemplate(String to, String template, List<String> p) {
                throw new WhatsAppSendException("WhatsApp API 400: template not approved", null);
            }
        };
        Appointment a = new Appointment();
        a.setGarment("Anarkali");
        a.setDate(booking.openDays().getFirst());
        a.setTime(BookingService.SLOTS.getFirst());
        a.setLocation(Appointment.Location.STUDIO);
        a.setCustomerName("Asha");
        a.setPhone("9876511111");
        a = appointments.save(a);

        var notifierWithFailure = new AppointmentNotifier(appointments, failing, storeProperties, formats);
        Instant start = notifierWithFailure.startOf(a);
        notifierWithFailure.sendDueReminders(start.minus(Duration.ofMinutes(30)));

        Appointment after = appointments.findById(a.getId()).orElseThrow();
        assertThat(after.getWhatsappStatus()).contains("failed", "template not approved");
        assertThat(after.getHourReminderSentAt()).isNotNull();
    }
}
