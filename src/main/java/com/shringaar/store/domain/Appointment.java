package com.shringaar.store.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** A studio fitting or a video/phone consultation booked from the "Book a fitting" page. */
@Entity
public class Appointment {

    public enum Location {
        STUDIO("Studio fitting", "fitting at our studio", true),
        VIDEO_CALL("Video call", "video call consultation", true),
        PHONE_CALL("Phone call", "phone call consultation", true),
        /** No longer offered; kept so older bookings still load. */
        HOME_VISIT("Home visit", "home visit", false);

        private final String label;
        private final String description;
        private final boolean offered;

        Location(String label, String description, boolean offered) {
            this.label = label;
            this.description = description;
            this.offered = offered;
        }

        public String getLabel() { return label; }
        /** Lower-case phrase used inside messages: "your video call consultation". */
        public String getDescription() { return description; }
        public boolean isOffered() { return offered; }
        public boolean isRemote() { return this == VIDEO_CALL || this == PHONE_CALL; }
    }

    /**
     * Stores the type as plain text (not a database enum) so new appointment
     * types can be added without changing the table.
     */
    @Converter
    public static class LocationConverter implements AttributeConverter<Location, String> {
        @Override public String convertToDatabaseColumn(Location l) { return l == null ? null : l.name(); }
        @Override public Location convertToEntityAttribute(String s) { return s == null ? null : Location.valueOf(s); }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Choose what we are making")
    private String garment;

    @NotNull(message = "Pick a day")
    @FutureOrPresent(message = "Pick a day from today onwards")
    private LocalDate date;

    @NotNull(message = "Pick a time")
    private LocalTime time;

    @NotNull(message = "Choose a studio visit, video call or phone call")
    @Convert(converter = LocationConverter.class)
    @Column(length = 20)
    private Location location = Location.STUDIO;

    @NotBlank(message = "Tell us your name")
    private String customerName;

    @NotBlank(message = "We need a phone number to confirm")
    @Pattern(regexp = "^[+0-9 ()-]{7,20}$", message = "Enter a valid phone number")
    private String phone;

    @Column(length = 1000)
    private String address;

    @Column(length = 2000)
    private String notes;

    private boolean confirmed;
    private Instant createdAt = Instant.now();

    /** Customer agreed to receive the confirmation and reminders on WhatsApp. */
    private Boolean whatsappOptIn = true;
    /** When each automatic WhatsApp message was handled (sent, or attempted and failed). */
    private Instant confirmationSentAt;
    private Instant dayReminderSentAt;
    private Instant hourReminderSentAt;
    /** Result of the last automatic WhatsApp message, shown in the admin. */
    @Column(length = 500)
    private String whatsappStatus;

    /** Older bookings made before this option existed count as opted in. */
    public boolean isWhatsappOptIn() { return !Boolean.FALSE.equals(whatsappOptIn); }
    public Boolean getWhatsappOptIn() { return isWhatsappOptIn(); }
    public void setWhatsappOptIn(Boolean whatsappOptIn) { this.whatsappOptIn = whatsappOptIn != null && whatsappOptIn; }
    public Instant getConfirmationSentAt() { return confirmationSentAt; }
    public void setConfirmationSentAt(Instant t) { this.confirmationSentAt = t; }
    public Instant getDayReminderSentAt() { return dayReminderSentAt; }
    public void setDayReminderSentAt(Instant t) { this.dayReminderSentAt = t; }
    public Instant getHourReminderSentAt() { return hourReminderSentAt; }
    public void setHourReminderSentAt(Instant t) { this.hourReminderSentAt = t; }
    public String getWhatsappStatus() { return whatsappStatus; }
    public void setWhatsappStatus(String whatsappStatus) { this.whatsappStatus = whatsappStatus; }

    public Long getId() { return id; }
    public String getGarment() { return garment; }
    public void setGarment(String garment) { this.garment = garment; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }
    public Instant getCreatedAt() { return createdAt; }
}
