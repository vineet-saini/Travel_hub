package com.travel.controller;

import com.travel.entity.Booking;
import com.travel.entity.User;
import com.travel.repository.BookingRepository;
import com.travel.repository.UserRepository;
import com.travel.service.PackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    @Autowired private PackageService packageService;
    @Autowired private UserRepository userRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    // Price per person per destination (INR)
    private static final Map<String, Integer> PRICES = Map.ofEntries(
        Map.entry("kashmir", 25000), Map.entry("shimla", 18000),
        Map.entry("mumbai", 15000), Map.entry("agra", 12000),
        Map.entry("newyork", 85000), Map.entry("losangeles", 80000),
        Map.entry("miami", 75000), Map.entry("toronto", 90000),
        Map.entry("vancouver", 88000), Map.entry("niagara", 70000),
        Map.entry("london", 95000), Map.entry("edinburgh", 85000),
        Map.entry("manchester", 80000)
    );

    @GetMapping("/")
    public String index() { return "redirect:/home"; }

    @GetMapping("/home")
    public String home(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        model.addAttribute("countries", new String[]{"India", "US", "Canada", "UK"});
        if (userDetails != null) {
            userRepository.findByUsername(userDetails.getUsername())
                .ifPresent(u -> model.addAttribute("currentUser", u));
        }
        return "home";
    }

    @GetMapping("/country/{country}")
    public String country(@PathVariable String country, Model model) {
        Map<String, String[]> placesMap = Map.of(
            "India", new String[]{"Kashmir", "Shimla", "Mumbai", "Agra"},
            "US", new String[]{"New York", "Los Angeles", "Miami"},
            "Canada", new String[]{"Toronto", "Vancouver", "Niagara"},
            "UK", new String[]{"London", "Edinburgh", "Manchester"}
        );
        model.addAttribute("country", country);
        model.addAttribute("places", placesMap.getOrDefault(country, new String[]{}));
        return "country";
    }

    @GetMapping("/place/{place}")
    public String place(@PathVariable String place, Model model) {
        String key = place.toLowerCase().replace(" ", "");
        int price = PRICES.getOrDefault(key, 30000);
        model.addAttribute("place", place);
        model.addAttribute("packageContent", packageService.getPackageContent(place));
        model.addAttribute("pricePerPerson", price);
        return "place";
    }

    @GetMapping("/booking/traveller/{place}")
    public String travellerDetails(@PathVariable String place, Model model,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        String key = place.toLowerCase().replace(" ", "");
        int price = PRICES.getOrDefault(key, 30000);
        model.addAttribute("place", place);
        model.addAttribute("pricePerPerson", price);
        model.addAttribute("razorpayKeyId", razorpayKeyId);
        if (userDetails != null) {
            userRepository.findByUsername(userDetails.getUsername())
                .ifPresent(u -> model.addAttribute("currentUser", u));
        }
        return "traveller-details";
    }

    @GetMapping("/booking-success")
    public String bookingSuccess(@RequestParam String tripId, @RequestParam String place, Model model) {
        model.addAttribute("tripId", tripId);
        model.addAttribute("place", place);
        return "booking-success";
    }

    @GetMapping("/profile")
    public String profile(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        userRepository.findByUsername(userDetails.getUsername()).ifPresent(user -> {
            model.addAttribute("user", user);
            List<Booking> bookings = bookingRepository.findByUserOrderByBookedAtDesc(user);
            model.addAttribute("totalBookings", bookings.size());
            model.addAttribute("confirmedBookings",
                bookings.stream().filter(b -> "PAID".equals(b.getPaymentStatus())).count());
            model.addAttribute("destinationsVisited",
                bookings.stream().map(Booking::getPlace).distinct().count());
        });
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String fullName,
                                 @RequestParam String phone,
                                 @RequestParam(required = false) String newPassword,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirect) {
        userRepository.findByUsername(userDetails.getUsername()).ifPresent(user -> {
            user.setFullName(fullName);
            user.setPhone(phone);
            if (newPassword != null && !newPassword.isBlank()) {
                user.setPassword(passwordEncoder.encode(newPassword));
            }
            userRepository.save(user);
        });
        redirect.addFlashAttribute("success", "Profile updated successfully!");
        return "redirect:/profile";
    }

    @GetMapping("/my-bookings")
    public String myBookings(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        userRepository.findByUsername(userDetails.getUsername()).ifPresent(user -> {
            List<Booking> bookings = bookingRepository.findByUserOrderByBookedAtDesc(user);
            long paidCount = bookings.stream().filter(b -> "PAID".equals(b.getPaymentStatus())).count();
            long pendingCount = bookings.stream().filter(b -> "PENDING".equals(b.getPaymentStatus())).count();
            double totalSpent = bookings.stream().filter(b -> "PAID".equals(b.getPaymentStatus())).mapToDouble(Booking::getTotalAmount).sum();
            model.addAttribute("bookings", bookings);
            model.addAttribute("paidCount", paidCount);
            model.addAttribute("pendingCount", pendingCount);
            model.addAttribute("totalSpent", totalSpent);
            model.addAttribute("currentUser", user);
        });
        return "my-bookings";
    }

    @GetMapping("/contact")
    public String contact() { return "contact"; }

    @GetMapping("/about")
    public String about() { return "about"; }

    @GetMapping("/trip-optimizer")
    public String tripOptimizer() { return "trip-optimizer"; }

    @GetMapping("/booking/detail/{id}")
    public String bookingDetail(@PathVariable Long id, Model model,
                                @AuthenticationPrincipal UserDetails userDetails) {
        userRepository.findByUsername(userDetails.getUsername()).ifPresent(user -> {
            bookingRepository.findById(id).ifPresent(booking -> {
                // Security: only owner can view
                if (booking.getUser() != null && booking.getUser().getId().equals(user.getId())) {
                    model.addAttribute("booking", booking);
                    model.addAttribute("pricePerPerson",
                        booking.getNumberOfPeople() > 0
                            ? (int)(booking.getTotalAmount() / booking.getNumberOfPeople())
                            : (int) booking.getTotalAmount());
                }
            });
        });
        return "booking-detail";
    }
}
