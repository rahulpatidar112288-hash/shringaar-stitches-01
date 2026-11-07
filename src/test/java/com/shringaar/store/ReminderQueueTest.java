package com.shringaar.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import com.shringaar.store.domain.Appointment;
import com.shringaar.store.repository.AppointmentRepository;
import com.shringaar.store.service.ReminderQueue;
import com.shringaar.store.service.ReminderQueue.Kind;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** Manual reminders, with the WhatsApp API switched off (the default). */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:queue;DB_CLOSE_DELAY=-1",
        "store.upload-dir=target/test-uploads"
})
@AutoConfigureMockMvc
class ReminderQueueTest {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Autowired MockMvc mvc;
    @Autowired ReminderQueue queue;
    @Autowired AppointmentRepository appointments;

    @BeforeEach
    void reset() {
        appointments.deleteAll();
    }

    private Appointment book(LocalDate date, LocalTime time, Appointment.Location type, boolean optIn) {
        Appointment a = new Appointment();
        a.setGarment("Bridal lehenga");
        a.setDate(date);
        a.setTime(time);
        a.setLocation(type);
        a.setCustomerName("Priya");
        a.setPhone("98765 43210");
        a.setWhatsappOptIn(optIn);
        return appointments.save(a);
    }

    @Test
    void listsDayBeforeForTomorrowAndHourBeforeForTodayInTimeOrder() {
        // "now" is 2:00 pm on a fixed day
        LocalDate today = LocalDate.of(2026, 11, 12);
        Instant now = today.atTime(14, 0).atZone(IST).toInstant();

        book(today, LocalTime.of(10, 0), Appointment.Location.STUDIO, true);          // already over
        Appointment soon = book(today, LocalTime.of(15, 0), Appointment.Location.VIDEO_CALL, true);
        Appointment later = book(today, LocalTime.of(18, 0), Appointment.Location.PHONE_CALL, true);
        Appointment tomorrow = book(today.plusDays(1), LocalTime.of(11, 30), Appointment.Location.STUDIO, true);
        book(today.plusDays(1), LocalTime.of(13, 0), Appointment.Location.STUDIO, false); // opted out
        book(today.plusDays(2), LocalTime.of(13, 0), Appointment.Location.STUDIO, true);  // too far ahead

        var due = queue.due(now);
        assertThat(due).extracting(r -> r.appointment().getId())
                .containsExactly(soon.getId(), later.getId(), tomorrow.getId());
        assertThat(due).extracting(ReminderQueue.DueReminder::kind)
                .containsExactly(Kind.HOUR_BEFORE, Kind.HOUR_BEFORE, Kind.DAY_BEFORE);
        assertThat(due.get(0).dueNow()).as("15:00 is within 90 minutes").isTrue();
        assertThat(due.get(1).dueNow()).as("18:00 is not").isFalse();
        assertThat(queue.pendingCount(now)).isEqualTo(3);

        queue.markSentAndLink(soon.getId(), Kind.HOUR_BEFORE, now);
        assertThat(queue.pendingCount(now)).isEqualTo(2);
        assertThat(queue.due(now).getFirst().isSent()).isTrue();
    }

    @Test
    void messagesSayTomorrowOrToday() {
        Appointment a = book(LocalDate.of(2026, 11, 13), LocalTime.of(15, 0), Appointment.Location.VIDEO_CALL, true);
        assertThat(queue.message(a, Kind.DAY_BEFORE))
                .contains("Hi Priya", "video call consultation for your bridal lehenga", "tomorrow, Fri 13 Nov 2026 at 3:00 pm",
                        "video call you on WhatsApp");
        assertThat(queue.message(a, Kind.HOUR_BEFORE)).contains("today at 3:00 pm");
    }

    @Test
    void adminSendsMarksAndUndoes() throws Exception {
        LocalDate tomorrow = Instant.now().atZone(IST).toLocalDate().plusDays(1);
        Appointment a = book(tomorrow, LocalTime.of(11, 30), Appointment.Location.PHONE_CALL, true);

        mvc.perform(get("/admin").with(user("admin").roles("ADMIN")))
                .andExpect(content().string(Matchers.containsString("Reminders to send")));
        mvc.perform(get("/admin/appointments").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Reminders due")))
                .andExpect(content().string(Matchers.containsString("Send reminder")));

        var res = mvc.perform(post("/admin/appointments/" + a.getId() + "/remind")
                        .with(user("admin").roles("ADMIN")).with(csrf()).param("kind", "DAY_BEFORE"))
                .andExpect(status().is3xxRedirection()).andReturn();
        String link = res.getResponse().getRedirectedUrl();
        assertThat(link).startsWith("https://wa.me/919876543210?text=");
        assertThat(URLDecoder.decode(link, StandardCharsets.UTF_8)).contains("phone call consultation", "tomorrow");
        assertThat(appointments.findById(a.getId()).orElseThrow().getDayReminderSentAt()).isNotNull();

        mvc.perform(get("/admin/appointments").with(user("admin").roles("ADMIN")))
                .andExpect(content().string(Matchers.containsString("Sent ✓")));

        mvc.perform(post("/admin/appointments/" + a.getId() + "/remind/undo")
                        .with(user("admin").roles("ADMIN")).with(csrf()).param("kind", "DAY_BEFORE"))
                .andExpect(redirectedUrl("/admin/appointments"));
        assertThat(appointments.findById(a.getId()).orElseThrow().getDayReminderSentAt()).isNull();
    }

    @Test
    void remindersRequireStaffLogin() throws Exception {
        Appointment a = book(Instant.now().atZone(IST).toLocalDate().plusDays(1), LocalTime.of(10, 0),
                Appointment.Location.STUDIO, true);
        mvc.perform(post("/admin/appointments/" + a.getId() + "/remind").with(csrf()).param("kind", "DAY_BEFORE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl()).endsWith("/admin/login"));
        assertThat(appointments.findById(a.getId()).orElseThrow().getDayReminderSentAt()).isNull();
    }
}
