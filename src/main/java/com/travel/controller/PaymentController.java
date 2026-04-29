package com.travel.controller;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.travel.entity.Booking;
import com.travel.entity.User;
import com.travel.service.EmailService;
import com.travel.repository.BookingRepository;
import com.travel.repository.UserRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> request,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String place = (String) request.get("place");
            String country = (String) request.get("country");
            String travelerName = (String) request.get("travelerName");
            String travelerEmail = (String) request.get("travelerEmail");
            String travelerPhone = (String) request.get("travelerPhone");
            int people = Integer.parseInt(request.get("numberOfPeople").toString());
            String dateStr = (String) request.get("travelDate");
            double pricePerPerson = Double.parseDouble(request.get("pricePerPerson").toString());
            double totalAmount = pricePerPerson * people;

            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", (int)(totalAmount * 100)); // paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "TRV" + ((int)(Math.random() * 90000) + 10000));

            Order razorpayOrder = client.orders.create(orderRequest);

            // Save pending booking
            Booking booking = new Booking();
            booking.setTripId(razorpayOrder.get("receipt"));
            booking.setPlace(place);
            booking.setCountry(country);
            booking.setTravelerName(travelerName);
            booking.setTravelerEmail(travelerEmail);
            booking.setTravelerPhone(travelerPhone);
            booking.setNumberOfPeople(people);
            booking.setTravelDate(LocalDate.parse(dateStr));
            booking.setTotalAmount(totalAmount);
            booking.setRazorpayOrderId(razorpayOrder.get("id"));
            booking.setPaymentStatus("PENDING");

            // Save booking location
            if (request.get("bookingCity") != null)
                booking.setBookingCity(request.get("bookingCity").toString());
            if (request.get("bookingCountry") != null)
                booking.setBookingCountry(request.get("bookingCountry").toString());
            if (request.get("bookingLat") != null)
                booking.setBookingLat(Double.parseDouble(request.get("bookingLat").toString()));
            if (request.get("bookingLng") != null)
                booking.setBookingLng(Double.parseDouble(request.get("bookingLng").toString()));

            User user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
            booking.setUser(user);
            bookingRepository.save(booking);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", razorpayOrder.get("id").toString());
            response.put("amount", razorpayOrder.get("amount").toString());
            response.put("currency", razorpayOrder.get("currency").toString());
            response.put("keyId", keyId);
            response.put("tripId", booking.getTripId());
            return ResponseEntity.ok(response);

        } catch (RazorpayException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> request) {
        try {
            String orderId = request.get("razorpay_order_id");
            String paymentId = request.get("razorpay_payment_id");
            String signature = request.get("razorpay_signature");

            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", orderId);
            attributes.put("razorpay_payment_id", paymentId);
            attributes.put("razorpay_signature", signature);

            boolean valid = Utils.verifyPaymentSignature(attributes, keySecret);

            if (valid) {
                Optional<Booking> opt = bookingRepository.findByRazorpayOrderId(orderId);
                if (opt.isPresent()) {
                    Booking booking = opt.get();
                    booking.setPaymentStatus("PAID");
                    booking.setRazorpayPaymentId(paymentId);
                    bookingRepository.save(booking);
                    // Send confirmation email asynchronously
                    emailService.sendBookingConfirmation(booking);
                    return ResponseEntity.ok(Map.of("status", "success", "tripId", booking.getTripId()));
                }
            }
            return ResponseEntity.badRequest().body(Map.of("status", "failed"));
        } catch (RazorpayException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
