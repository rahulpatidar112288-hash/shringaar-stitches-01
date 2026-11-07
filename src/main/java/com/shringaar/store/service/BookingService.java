package com.shringaar.store.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.shringaar.store.domain.Appointment;
import com.shringaar.store.repository.AppointmentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    public static final List<String> GARMENTS = List.of("Bridal lehenga", "Saree blouse", "Anarkali", "Salwar suit / kurta set", "Saree draping", "Alteration");
    public static final List<LocalTime> SLOTS = List.of(
            LocalTime.of(10, 0), LocalTime.of(11, 30), LocalTime.of(13, 0),
            LocalTime.of(15, 0), LocalTime.of(16, 30), LocalTime.of(18, 0));
    private static final int DAYS_AHEAD = 14;
    private static final DayOfWeek CLOSED_DAY = DayOfWeek.MONDAY;

    private final AppointmentRepository appointments;
    private final AppointmentNotifier notifier;

    public BookingService(AppointmentRepository appointments, AppointmentNotifier notifier) {
        this.appointments = appointments;
        this.notifier = notifier;
    }

    /** What visitors can choose on the booking page. */
    public static List<Appointment.Location> bookableLocations() {
        return java.util.Arrays.stream(Appointment.Location.values()).filter(Appointment.Location::isOffered).toList();
    }

    /** Open days starting tomorrow. */
    public List<LocalDate> openDays() {
        List<LocalDate> days = new ArrayList<>();
        LocalDate d = LocalDate.now().plusDays(1);
        while (days.size() < DAYS_AHEAD) {
            if (d.getDayOfWeek() != CLOSED_DAY) days.add(d);
            d = d.plusDays(1);
        }
        return days;
    }

    /** "2026-11-12T11:30" keys for slots already taken in the booking window. */
    public Set<String> takenSlots() {
        List<LocalDate> days = openDays();
        return appointments.findByDateBetween(days.getFirst(), days.getLast()).stream()
                .map(a -> a.getDate() + "T" + a.getTime())
                .collect(Collectors.toSet());
    }

    public boolean isSlotValid(LocalDate date, LocalTime time) {
        return openDays().contains(date) && SLOTS.contains(time);
    }

    @Transactional
    public Appointment book(Appointment a) {
        if (a.getLocation() == null || !a.getLocation().isOffered()) throw new SlotUnavailableException("Choose a studio visit, video call or phone call.");
        if (!isSlotValid(a.getDate(), a.getTime())) throw new SlotUnavailableException("That time isn't offered. Please pick another.");
        if (appointments.existsByDateAndTime(a.getDate(), a.getTime())) throw new SlotUnavailableException("That slot was just booked. Please pick another.");
        Appointment saved = appointments.save(a);
        notifier.sendConfirmation(saved);
        return saved;
    }

    public static class SlotUnavailableException extends RuntimeException {
        public SlotUnavailableException(String msg) { super(msg); }
    }
}
