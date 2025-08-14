package com.giguniverse.backend.Auth.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.giguniverse.backend.Auth.Repository.AdminRepository;
import com.giguniverse.backend.Auth.JWT.JwtUtil;
import com.giguniverse.backend.Auth.Model.Admin;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class AdminAuthService {
    private static final int CODE_LENGTH = 8;

    @Autowired private AdminRepository adminRepo;
    @Autowired private JavaMailSender mail;
    @Autowired private PasswordEncoder encoder;

    private String generateCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom rand = new SecureRandom();
        return rand.ints(CODE_LENGTH, 0, chars.length())
            .mapToObj(chars::charAt)
            .map(Object::toString)
            .collect(Collectors.joining());
    }

    public void forgotPassword(String email) {
        adminRepo.findByEmail(email).ifPresent(user -> {
            String code = generateCode();
            user.setResetPasswordToken(code);
            user.setResetPasswordTokenExpiry(LocalDateTime.now());
            adminRepo.save(user);
            sendResetCodeEmail(user.getEmail(), code);
        });
    }

    public boolean verifyCode(String email, String code) {
        return adminRepo.findByEmail(email)
            .map(u -> {
                System.out.println("DB code: " + u.getResetPasswordToken());
                System.out.println("DB createdAt: " + u.getResetPasswordTokenExpiry());
                System.out.println("Now: " + LocalDateTime.now());
                System.out.println("Matches code? " + code.equalsIgnoreCase(u.getResetPasswordToken()));
                System.out.println("Is within 5 minutes? " +
                    u.getResetPasswordTokenExpiry().plusMinutes(5).isAfter(LocalDateTime.now()));

                return code.equalsIgnoreCase(u.getResetPasswordToken()) &&
                    u.getResetPasswordTokenExpiry().plusMinutes(5).isAfter(LocalDateTime.now());
            })
            .orElse(false);
    }

    public void resetPassword(String email, String code, String newPassword) {
        System.out.println(">>> Received email: " + email);
        System.out.println(">>> Received code: " + code);

        Admin user = adminRepo.findByEmail(email)
            .orElseThrow(() -> {
                System.out.println(">>> Email not found in DB");
                return new IllegalArgumentException("Invalid or expired code");
            });

        System.out.println(">>> Stored code: " + user.getResetPasswordToken());

        if (!code.equals(user.getResetPasswordToken())) {
            System.out.println(">>> Code mismatch");
            throw new IllegalArgumentException("Invalid or expired code");
        }

        user.setPassword(encoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        adminRepo.save(user);
    }
        
    public void resendResetCode(String email) {
        Admin user = adminRepo.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        LocalDateTime now = LocalDateTime.now();

        if (user.getResetPasswordTokenExpiry() != null &&
            ChronoUnit.SECONDS.between(user.getResetPasswordTokenExpiry(), now) < 60) {
            throw new IllegalStateException("You must wait 60 seconds before resending.");
        }

        String code = generateCode();
        user.setResetPasswordToken(code);
        user.setResetPasswordTokenExpiry(now);
        adminRepo.save(user);

        sendResetCodeEmail(user.getEmail(), code);
    }

    private void sendResetCodeEmail(String email, String code) {
        try {
            MimeMessage message = mail.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            String body = "<div style='font-family:Arial,sans-serif;font-size:14px;color:#333;'>"
                + "<p>Dear <strong>" + email + "</strong>,</p>"
                + "<p>The following is your <strong>Password Reset Code</strong> for your Admin account:</p>"
                + "<p style='font-size:18px;font-weight:bold;color:#004085;'>" + code + "</p>"
                + "<p>This password reset code is <strong>valid for only 5 minutes</strong>.</p>"
                + "<p>If you did not request this, you may safely ignore the message.</p>"
                + "<br>"
                + "<p>Regards,<br>GigsUniverse Team</p>"
                + "</div>";

            helper.setFrom("admin@gigsuniverse.studio");
            helper.setTo(email);
            helper.setSubject("Your Password Reset Code");
            helper.setText(body, true);

            mail.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public boolean emailExists(String email) {
        return adminRepo.findByEmail(email).isPresent();
    }

    @Autowired
    JwtUtil jwtUtil;

    // Login Section
    public String emailProviderLogin(String email, String rawPassword){
        Optional<Admin> optional = adminRepo.findByEmail(email);
        if (optional.isEmpty()) return null;

        Admin admin = optional.get();
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        if (passwordEncoder.matches(rawPassword, admin.getPassword())) {
            admin.setLastLoginDate(LocalDateTime.now());
            adminRepo.save(admin);
            return jwtUtil.generateJwtToken(admin.getAdminUserId(), admin.getEmail(), admin.getRole());
        } else {
            return null;
        }
    }

}
