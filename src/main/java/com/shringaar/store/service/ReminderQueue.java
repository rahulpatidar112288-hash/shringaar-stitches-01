package com.shringaar.store.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.shringaar.store.config.StoreProperties;
import com.shringaar.store.domain.Appointment;
import com.shringaar.store.repository.AppointmentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The staff's to-do list of WhatsApp reminders to send by hand: a day-before
 * reminder for tomorrow's appointments and a 1-hour reminder for today's.
 * Uses the same "sent" marks as the automatic sender, so turning on the
 * WhatsApp API later never repeats a reminder someone already sent.
 */
@Service
public class ReminderQueue {

    public enum Kind {
        DAY_BEFORE("Day-before reminder"),
        HOUR_BEFORE("1-hour reminder");

        private final String label;

        Kind(String label) { this.label = label; }

        public String getLabel() { return label; }
    }

    /** Highlight a 1-hour reminder from this long before the start. */
    static final Duration DUE_NOW = Duration.ofMinutes(90);

    public record DueReminder(Appointment appointment, Kind kind, Instant start,
                              Instant sentAt, boolean dueNow) {
        public boolean isSent() { return sentAt != null; }
    }

    private final AppointmentRepository appointments;
    private final AppointmentNotifier notifier;
    private final StoreProperties store;
    private final Formats fmt;

    public ReminderQueue(AppointmentRepository appointments, AppointmentNotifier notifier,
                         StoreProperties store, Formats fmt) {
        this.appointments = appointments;
        this.notifier = notifier;
        this.store = store;
        this.fmt = fmt;
    }

    @Transactional(readOnly = true)
    public List<DueReminder> due(Instant now) {
        LocalDate today = now.atZone(zone()).toLocalDate();
        List<DueReminder> list = new ArrayList<>();
        for (Appointment a : appointments.findByDateBetween(today, today.plusDays(1))) {
            if (!a.isWhatsappOptIn()) continue;
            Instant start = notifier.startOf(a);
            if (!start.isAfter(now)) continue; // already started or over
            if (a.getDate().equals(today)) {
                boolean dueNow = Duration.between(now, start).compareTo(DUE_NOW) <= 0;
                list.add(new DueReminder(a, Kind.HOUR_BEFORE, start, a.getHourReminderSentAt(), dueNow));
            } else {
                list.add(new DueReminder(a, Kind.DAY_BEFORE, start, a.getDayReminderSentAt(), false));
            }
        }
        list.sort(Comparator.comparing(DueReminder::start));
        return list;
    }

    public long pendingCount(Instant now) {
        return due(now).stream().filter(r -> !r.isSent()).count();
    }

    /** Marks the reminder as sent by staff and returns the WhatsApp link that opens it. */
    @Transactional
    public String markSentAndLink(Long id, Kind kind, Instant now) {
        Appointment a = appointments.findById(id).orElseThrow();
        if (kind == Kind.DAY_BEFORE) a.setDayReminderSentAt(now);
        else a.setHourReminderSentAt(now);
        a.setWhatsappStatus(kind.getLabel() + " sent by staff " + fmt.stamp(now));
        appointments.save(a);
        return whatsappLink(a, kind);
    }

    @Transactional
    public void undo(Long id, Kind kind) {
        Appointment a = appointments.findById(id).orElseThrow();
        if (kind == Kind.DAY_BEFORE) a.setDayReminderSentAt(null);
        else a.setHourReminderSentAt(null);
        a.setWhatsappStatus(null);
        appointments.save(a);
    }

    public String whatsappLink(Appointment a, Kind kind) {
        return "https://wa.me/" + Phones.toWhatsAppNumber(a.getPhone()) + "?text="
                + URLEncoder.encode(message(a, kind), StandardCharsets.UTF_8).replace("+", "%20");
    }

    /** Message text for each reminder; the day-before one names the date, the 1-hour one says "today". */
    public String message(Appointment a, Kind kind) {
        String what = a.getLocation().getDescription() + " for your " + a.getGarment().toLowerCase();
        String when = kind == Kind.DAY_BEFORE
                ? "tomorrow, " + fmt.day(a.getDate()) + " at " + fmt.time(a.getTime())
                : "today at " + fmt.time(a.getTime());
        return "Hi " + a.getCustomerName() + ", this is " + store.name() + " reminding you of your "
                + what + " " + when + ".\n\n" + notifier.howToJoin(a)
                + "\n\nReply here if you need to change the time.";
    }

    private ZoneId zone() {
        return ZoneId.of(store.timezone());
    }
}
