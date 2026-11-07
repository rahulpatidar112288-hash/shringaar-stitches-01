package com.shringaar.store.service;

import com.shringaar.store.domain.PaymentMethod;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CheckoutForm {

    @NotBlank(message = "Tell us your name")
    @Size(max = 120)
    private String name;

    @NotBlank(message = "We need a phone number to confirm your order")
    @Pattern(regexp = "^[+0-9 ()-]{7,20}$", message = "Enter a valid phone number")
    private String phone;

    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "Enter a delivery address")
    @Size(max = 1000)
    private String address;

    @NotBlank(message = "Enter your city")
    private String city;

    @NotBlank(message = "Enter your PIN code")
    @Pattern(regexp = "^[0-9]{6}$", message = "PIN code is 6 digits")
    private String pincode;

    @Size(max = 2000)
    private String notes;

    @NotNull(message = "Choose how you'd like to pay")
    private PaymentMethod paymentMethod = PaymentMethod.CASH_ON_DELIVERY;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
}
