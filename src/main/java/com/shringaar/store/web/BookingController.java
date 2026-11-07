package com.shringaar.store.web;

import com.shringaar.store.domain.Appointment;
import com.shringaar.store.repository.AppointmentRepository;
import com.shringaar.store.service.AppointmentNotifier;
import com.shringaar.store.service.BookingService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class BookingController {

    private final BookingService booking;
    private final AppointmentRepository appointments;
    private final AppointmentNotifier notifier;

    public BookingController(BookingService booking, AppointmentRepository appointments, AppointmentNotifier notifier) {
        this.booking = booking;
        this.appointments = appointments;
        this.notifier = notifier;
    }

    /** Visitors may only fill in the booking fields, never staff-only ones. */
    @InitBinder("appointment")
    void restrictFields(WebDataBinder binder) {
        binder.setAllowedFields("garment", "date", "time", "location", "customerName", "phone", "notes", "whatsappOptIn", "_whatsappOptIn");
    }

    @GetMapping("/book-a-fitting")
    public String form(@RequestParam(required = false) String garment, Model model) {
        Appointment a = new Appointment();
        a.setGarment(garment != null && BookingService.GARMENTS.contains(garment) ? garment : BookingService.GARMENTS.getFirst());
        model.addAttribute("appointment", a);
        addOptions(model);
        return "booking";
    }

    @PostMapping("/book-a-fitting")
    public String book(@Valid @ModelAttribute("appointment") Appointment appointment, BindingResult result,
                       Model model, HttpSession session) {
        if (appointment.getLocation() != null && !appointment.getLocation().isOffered()) {
            result.rejectValue("location", "unavailable", "Choose a studio visit, video call or phone call");
        }
        if (!result.hasErrors()) {
            try {
                Appointment saved = booking.book(appointment);
                SessionKeys.remember(session, SessionKeys.BOOKINGS, String.valueOf(saved.getId()));
                return "redirect:/book-a-fitting/" + saved.getId();
            } catch (BookingService.SlotUnavailableException e) {
                result.rejectValue("time", "taken", e.getMessage());
            }
        }
        addOptions(model);
        return "booking";
    }

    @GetMapping("/book-a-fitting/{id}")
    public String confirmed(@PathVariable Long id, HttpSession session, Model model) {
        EnquiryController.requireOwned(session, SessionKeys.BOOKINGS, String.valueOf(id));
        Appointment a = appointments.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("appointment", a);
        model.addAttribute("howToJoin", notifier.howToJoin(a));
        model.addAttribute("autoReminders", notifier.isAutomatic() && a.isWhatsappOptIn());
        model.addAttribute("whatsappLink", "/enquire/booking/" + id);
        return "booking-confirmation";
    }

    private void addOptions(Model model) {
        model.addAttribute("garments", BookingService.GARMENTS);
        model.addAttribute("days", booking.openDays());
        model.addAttribute("slots", BookingService.SLOTS);
        model.addAttribute("taken", booking.takenSlots());
        model.addAttribute("locations", BookingService.bookableLocations());
    }
}
