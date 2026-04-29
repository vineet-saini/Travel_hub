package com.travel.service;

import java.text.NumberFormat;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.travel.entity.Booking;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Async
    public void sendBookingConfirmation(Booking booking) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "TravelHub");
            helper.setTo(booking.getTravelerEmail());
            helper.setSubject("Booking Confirmed! Your trip to " + booking.getPlace() + " — " + booking.getTripId());
            helper.setText(buildHtml(booking), true);

            mailSender.send(message);
        } catch (Exception e) {
            // Log but don't fail the booking flow
            System.err.println("Email send failed: " + e.getMessage());
        }
    }

    private String buildHtml(Booking booking) {
        String fmt = NumberFormat.getNumberInstance(new Locale("en", "IN")).format(booking.getTotalAmount());

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
            + "<style>"
            + "body{margin:0;padding:0;background:#f4f6fb;font-family:'Segoe UI',Arial,sans-serif;}"
            + ".wrap{max-width:600px;margin:30px auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);}"
            + ".header{background:linear-gradient(135deg,#1a1a2e,#0f3460);padding:36px 32px;text-align:center;}"
            + ".header h1{color:#fff;margin:0;font-size:26px;letter-spacing:1px;}"
            + ".header p{color:rgba(255,255,255,0.7);margin:6px 0 0;font-size:14px;}"
            + ".badge{display:inline-block;background:#56ab2f;color:#fff;border-radius:20px;padding:6px 18px;font-size:13px;font-weight:600;margin-top:14px;}"
            + ".body{padding:32px;}"
            + ".greeting{font-size:18px;font-weight:600;color:#1a1a2e;margin-bottom:6px;}"
            + ".sub{color:#666;font-size:14px;margin-bottom:24px;}"
            + ".trip-card{background:linear-gradient(135deg,#1a1a2e,#0f3460);border-radius:14px;padding:24px;color:#fff;margin-bottom:24px;}"
            + ".trip-card .dest{font-size:24px;font-weight:700;margin:0 0 4px;}"
            + ".trip-card .tid{font-size:12px;color:rgba(255,255,255,0.6);margin:0;}"
            + ".details{background:#f8f9fc;border-radius:12px;padding:20px;margin-bottom:24px;}"
            + ".row{display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid #eee;font-size:14px;}"
            + ".row:last-child{border-bottom:none;}"
            + ".row .label{color:#888;}"
            + ".row .value{font-weight:600;color:#1a1a2e;}"
            + ".total-row{background:#fff8f0;border-radius:10px;padding:14px 16px;display:flex;justify-content:space-between;align-items:center;margin-bottom:24px;}"
            + ".total-row .tl{font-size:15px;font-weight:600;color:#1a1a2e;}"
            + ".total-row .tv{font-size:22px;font-weight:700;color:#e94560;}"
            + ".paid-badge{display:inline-block;background:#e8f5e9;color:#2e7d32;border-radius:8px;padding:4px 12px;font-size:12px;font-weight:600;}"
            + ".info-box{background:#fff8e1;border-left:4px solid #f5a623;border-radius:8px;padding:14px 16px;font-size:13px;color:#555;margin-bottom:24px;}"
            + ".btn{display:block;width:fit-content;margin:0 auto 24px;background:#e94560;color:#ffffff !important;text-decoration:none !important;padding:14px 36px;border-radius:10px;font-weight:600;font-size:15px;text-align:center;mso-padding-alt:0;}"
            + ".footer{background:#f8f9fc;padding:20px 32px;text-align:center;font-size:12px;color:#aaa;border-top:1px solid #eee;}"
            + "</style></head><body>"
            + "<div class='wrap'>"

            // Header
            + "<div class='header'>"
            + "<h1>✈ TravelHub</h1>"
            + "<p>Your journey begins here</p>"
            + "<div class='badge'>✓ Booking Confirmed</div>"
            + "</div>"

            // Body
            + "<div class='body'>"
            + "<div class='greeting'>Hi " + booking.getTravelerName() + "! 🎉</div>"
            + "<div class='sub'>Your booking is confirmed and payment has been received. Get ready for an amazing trip!</div>"

            // Trip card
            + "<div class='trip-card'>"
            + "<div class='dest'>" + booking.getPlace() + "</div>"
            + "<div class='tid'>Trip ID: " + booking.getTripId() + "</div>"
            + "</div>"

            // Details
            + "<div class='details'>"
            + "<div class='row'><span class='label'>Traveler Name</span><span class='value'>" + booking.getTravelerName() + "</span></div>"
            + "<div class='row'><span class='label'>Email</span><span class='value'>" + booking.getTravelerEmail() + "</span></div>"
            + "<div class='row'><span class='label'>Phone</span><span class='value'>" + booking.getTravelerPhone() + "</span></div>"
            + "<div class='row'><span class='label'>Travel Date</span><span class='value'>" + booking.getTravelDate() + "</span></div>"
            + "<div class='row'><span class='label'>Travelers</span><span class='value'>" + booking.getNumberOfPeople() + " person(s)</span></div>"
            + "<div class='row'><span class='label'>Payment</span><span class='value'><span class='paid-badge'>✓ PAID</span></span></div>"
            + "<div class='row'><span class='label'>Payment ID</span><span class='value'>" + booking.getRazorpayPaymentId() + "</span></div>"
            + "</div>"

            // Total
            + "<div class='total-row'>"
            + "<span class='tl'>Total Amount Paid</span>"
            + "<span class='tv'>₹" + fmt + "</span>"
            + "</div>"

            // Info box
            + "<div class='info-box'>"
            + "📋 <strong>What's next?</strong> Our team will reach out 48 hours before your trip with detailed instructions, hotel check-in info, and your guide's contact details."
            + "</div>"

            // CTA button
            + "<table width='100%' cellpadding='0' cellspacing='0' style='margin-bottom:24px;'><tr><td align='center'>"
            + "<a href='http://localhost:8080/my-bookings' "
            + "style='display:inline-block;background:#e94560;color:#ffffff !important;text-decoration:none !important;padding:14px 36px;border-radius:10px;font-weight:600;font-size:15px;font-family:Arial,sans-serif;'>View My Bookings</a>"
            + "</td></tr></table>"
            + "</div>"

            // Footer
            + "<div class='footer'>"
            + "© 2026 TravelHub · support@travelhub.com · +91-9015409405<br>"
            + "This is an automated confirmation email. Please do not reply."
            + "</div>"
            + "</div></body></html>";
    }
}
