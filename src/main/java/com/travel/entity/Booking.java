package com.travel.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tripId;
    private String place;
    private String country;
    private String travelerName;
    private String travelerEmail;
    private String travelerPhone;
    private int numberOfPeople;
    private LocalDate travelDate;
    private double totalAmount;
    private String paymentStatus; // PENDING, PAID, FAILED
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private LocalDateTime bookedAt;
    private String bookingCity;
    private String bookingCountry;
    private Double bookingLat;
    private Double bookingLng;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @PrePersist
    public void prePersist() {
        bookedAt = LocalDateTime.now();
        if (paymentStatus == null) paymentStatus = "PENDING";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }
    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getTravelerName() { return travelerName; }
    public void setTravelerName(String travelerName) { this.travelerName = travelerName; }
    public String getTravelerEmail() { return travelerEmail; }
    public void setTravelerEmail(String travelerEmail) { this.travelerEmail = travelerEmail; }
    public String getTravelerPhone() { return travelerPhone; }
    public void setTravelerPhone(String travelerPhone) { this.travelerPhone = travelerPhone; }
    public int getNumberOfPeople() { return numberOfPeople; }
    public void setNumberOfPeople(int numberOfPeople) { this.numberOfPeople = numberOfPeople; }
    public LocalDate getTravelDate() { return travelDate; }
    public void setTravelDate(LocalDate travelDate) { this.travelDate = travelDate; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }
    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }
    public LocalDateTime getBookedAt() { return bookedAt; }
    public void setBookedAt(LocalDateTime bookedAt) { this.bookedAt = bookedAt; }
    public String getBookingCity() { return bookingCity; }
    public void setBookingCity(String bookingCity) { this.bookingCity = bookingCity; }
    public String getBookingCountry() { return bookingCountry; }
    public void setBookingCountry(String bookingCountry) { this.bookingCountry = bookingCountry; }
    public Double getBookingLat() { return bookingLat; }
    public void setBookingLat(Double bookingLat) { this.bookingLat = bookingLat; }
    public Double getBookingLng() { return bookingLng; }
    public void setBookingLng(Double bookingLng) { this.bookingLng = bookingLng; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
