package com.shringaar.store.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.shringaar.store.config.StoreProperties;
import com.shringaar.store.domain.Appointment;
import com.shringaar.store.repository.AppointmentRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends the customer a WhatsApp confirmation when she books, then reminders a
 * day before and an hour before. Runs only when the WhatsApp Cloud API is set
 * up; otherwise staff send reminders by hand from the admin.
 */
@Service
public class AppointmentNotifier {

    private static final Logger log = LoggerFactory.getLogger(AppointmentNotifier.class);
    static final Duration DAY = Duration.ofHours(24);
    static final Duration HOUR = Duration.ofHours(1);

    private final AppointmentRepository appointments;
    private final WhatsAppMessenger messenger;
    private final StoreProperties store;
    private final Formats fmt;

    public AppointmentNotifier(AppointmentRepository appointments, WhatsAppMessenger messenger,
                               StoreProperties store, Formats fmt) {
        this.appointments = appointments;
        this.messenger = messenger;
        this.store = store;
        this.fmt = fmt;
    }

    public boolean isAutomatic() {
        return messenger.isEnabled();
    }

    public Instant startOf(Appointment a) {
        return a.getDate().atTime(a.getTime()).atZone(zone()).toInstant();
    }

    /** Called right after a booking is saved. */
    @Transactional
    public void sendConfirmation(Appointment a) {
        if (!messenger.isEnabled() || !a.isWhatsappOptIn() || a.getConfirmationSentAt() != null) return;
        send(a, store.whatsappApi().confirmationTemplate(), "Confirmation");
        a.setConfirmationSentAt(Instant.now());
        appointments.save(a);
    }

    /** Checks every 5 minutes for reminders that are due. */
    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
    public void scheduledRun() {
        sendDueReminders(Instant.now());
    }

    @Transactional
    public int sendDueReminders(Instant now) {
        if (!messenger.isEnabled()) return 0;
        LocalDate today = now.atZone(zone()).toLocalDate();
        int sent = 0;
        List<Appointment> changed = new java.util.ArrayList<>();
        for (Appointment a : appointments.findByDateBetween(today, today.plusDays(2))) {
            if (!a.isWhatsappOptIn()) continue;
            Instant start = startOf(a);
            Duration until = Duration.between(now, start);
            if (until.isNegative() || until.isZero()) continue;

            if (until.compareTo(HOUR) <= 0) {
                if (a.getHourReminderSentAt() == null) {
                    send(a, store.whatsappApi().reminderTemplate(), "1-hour reminder");
                    a.setHourReminderSentAt(now);
                    // a 1-hour reminder makes a pending day-before one pointless
                    if (a.getDayReminderSentAt() == null) a.setDayReminderSentAt(now);
                    changed.add(a);
                    sent++;
                }
            } else if (until.compareTo(DAY) <= 0 && a.getDayReminderSentAt() == null) {
                // Skip the day-before reminder if she booked less than a day ahead:
                // the confirmation she just received already covers it.
                if (Duration.between(a.getCreatedAt(), start).compareTo(DAY) > 0) {
                    send(a, store.whatsappApi().reminderTemplate(), "Day-before reminder");
                    sent++;
                }
                a.setDayReminderSentAt(now);
                changed.add(a);
            }
        }
        // Saved explicitly: the scheduler calls this method directly, outside a transaction,
        // and the sent-marks must persist or the same reminder would go out every run.
        appointments.saveAll(changed);
        return sent;
    }

    /**
     * Template parameters, in order:
     * {{1}} name, {{2}} appointment type, {{3}} date and time, {{4}} how to join.
     */
    public List<String> parameters(Appointment a) {
        return List.of(
                a.getCustomerName(),
                a.getLocation().getDescription() + " for your " + a.getGarment().toLowerCase(),
                fmt.day(a.getDate()) + " at " + fmt.time(a.getTime()),
                howToJoin(a));
    }

    public String howToJoin(Appointment a) {
        return switch (a.getLocation()) {
            case VIDEO_CALL -> "We will video call you on WhatsApp at " + a.getPhone() + ". Please keep your phone handy.";
            case PHONE_CALL -> "We will call you at " + a.getPhone() + ".";
            case STUDIO, HOME_VISIT -> "Please come to " + store.address() + ", " + store.city() + ".";
        };
    }

    /** Text staff send by hand when automatic messages are off. */
    public String manualReminder(Appointment a) {
        List<String> p = parameters(a);
        return "Hi " + p.get(0) + ", this is " + store.name() + " reminding you of your "
                + p.get(1) + " on " + p.get(2) + ".\n\n" + p.get(3)
                + "\n\nReply here if you need to change the time.";
    }

    private void send(Appointment a, String template, String what) {
        try {
            messenger.sendTemplate(Phones.toWhatsAppNumber(a.getPhone()), template, parameters(a));
            a.setWhatsappStatus(what + " sent " + fmt.stamp(Instant.now()));
        } catch (WhatsAppMessenger.WhatsAppSendException e) {
            log.warn("WhatsApp {} for appointment {} failed: {}", what, a.getId(), e.getMessage());
            a.setWhatsappStatus(what + " failed: " + truncate(e.getMessage()));
        }
    }

    private ZoneId zone() {
        return ZoneId.of(store.timezone());
    }

    private static String truncate(String s) {
        return s == null ? "" : s.length() > 400 ? s.substring(0, 400) : s;
    }
}
