package com.travel.repository;

import com.travel.entity.Booking;
import com.travel.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserOrderByBookedAtDesc(User user);
    Optional<Booking> findByRazorpayOrderId(String orderId);
}
