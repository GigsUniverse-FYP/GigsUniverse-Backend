package com.giguniverse.backend.Auth.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.giguniverse.backend.Auth.DTO.FreelancerRegisterRequest;
import com.giguniverse.backend.Auth.JWT.JwtUtil;
import com.giguniverse.backend.Auth.Model.Freelancer;
import com.giguniverse.backend.Auth.Repository.FreelancerRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class FreelancerAuthService {

    @Value("${frontend.url}")
    private String frontendURL;

    @Autowired
    private FreelancerRepository freelancerRepo;

    @Autowired
    private JavaMailSender mailSender;

    public ResponseEntity<?> register(FreelancerRegisterRequest request) {
        if (freelancerRepo.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email already in use");
        }

        if (freelancerRepo.existsByFreelancerUserId(request.getFreelancerUserId())) {
            return ResponseEntity.badRequest().body("Freelancer ID already in use");
        }

        String token = UUID.randomUUID().toString();
        Freelancer user = new Freelancer();
        user.setFreelancerUserId(request.getFreelancerUserId());
        user.setEmail(request.getEmail());
        user.setPassword(new BCryptPasswordEncoder().encode(request.getPassword()));
        user.setRole("freelancer");
        user.setRegistrationProvider("email");
        user.setEmailConfirmationToken(token);
        user.setRegistrationDate(LocalDateTime.now());

        freelancerRepo.save(user);

        sendConfirmationEmail(user.getEmail(), token);
        return ResponseEntity.ok("Registration successful. Please confirm your email.");
    }

    public ResponseEntity<?> confirmEmail(String token) {
        Freelancer user = freelancerRepo.findByEmailConfirmationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));
        user.setEmailConfirmed(true);
        user.setEmailConfirmationToken(null);
        freelancerRepo.save(user);
        return ResponseEntity.ok("Email confirmed successfully.");
    }

    public void sendConfirmationEmail(String to, String token) {
        String subject = "Confirm your GigsUniverse Freelancer Account";
        String confirmationUrl = frontendURL + "/register/freelancer/confirm-email?email=" + to + "&token=" + token;
        
        String body = "Dear " + to + ",\n\n"
            + "Thank you for registering with GigsUniverse.\n\n"
            + "Click the following link to confirm your email:\n"
            + confirmationUrl + "\n\n"
            + "This verification duration for the account will only valid for 10 minutes. \n"
            + "Registration will be automatically terminated after 10 minutes.\n\n"
            + "If you did not sign up, please ignore this email.\n\n"
            + "Regards,\nGigsUniverse Team";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("admin@gigsuniverse.studio");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new MailSendException("Failed to send confirmation email", e);
        }
    }

    public ResponseEntity<?> checkIdAvailability(String freelancerId) {
        boolean idTaken = freelancerRepo.existsByFreelancerUserId(freelancerId);
        return ResponseEntity.ok(Map.of("idTaken", idTaken));
    }

    public void resendConfirmationEmail(String email) {
        Freelancer freelancer = freelancerRepo.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        LocalDateTime now = LocalDateTime.now();
        if (freelancer.getLastEmailSentAt() != null &&
            ChronoUnit.SECONDS.between(freelancer.getLastEmailSentAt(), now) < 60) {
            throw new IllegalStateException("You must wait 60 seconds before resending.");
        }

        freelancer.setLastEmailSentAt(now);
        freelancerRepo.save(freelancer);

        sendConfirmationEmail(freelancer.getEmail(), freelancer.getEmailConfirmationToken());
    }

    // Password Reset Section
    private static final int CODE_LENGTH = 8;

    @Autowired private FreelancerRepository repo;
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
        repo.findByEmail(email).ifPresent(user -> {
            String code = generateCode();
            user.setResetPasswordToken(code);
            user.setResetPasswordTokenExpiry(LocalDateTime.now());
            repo.save(user);
            sendResetCodeEmail(user.getEmail(), code);
        });
    }

    public boolean verifyCode(String email, String code) {
        return repo.findByEmail(email)
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

        Freelancer user = repo.findByEmail(email)
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
        repo.save(user);
    }
        
    public void resendResetCode(String email) {
        Freelancer user = repo.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        LocalDateTime now = LocalDateTime.now();

        if (user.getResetPasswordTokenExpiry() != null &&
            ChronoUnit.SECONDS.between(user.getResetPasswordTokenExpiry(), now) < 60) {
            throw new IllegalStateException("You must wait 60 seconds before resending.");
        }

        String code = generateCode();
        user.setResetPasswordToken(code);
        user.setResetPasswordTokenExpiry(now);
        repo.save(user);

        sendResetCodeEmail(user.getEmail(), code);
    }

    private void sendResetCodeEmail(String email, String code) {
        try {
            MimeMessage message = mail.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            String body = "<div style='font-family:Arial,sans-serif;font-size:14px;color:#333;'>"
                + "<p>Dear <strong>" + email + "</strong>,</p>"
                + "<p>The following is your <strong>Password Reset Code</strong> for your Freelancer account:</p>"
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
        return freelancerRepo.findByEmail(email).isPresent();
    }

    public String getProviderByEmail(String email) {
        return freelancerRepo.findProviderByEmail(email)
            .orElseThrow(() -> new RuntimeException("Provider not found for email: " + email));
    }

    @Autowired
    JwtUtil jwtUtil;

    // Login Section
    public String emailProviderLogin(String email, String rawPassword){
        Optional<Freelancer> optional = freelancerRepo.findByEmail(email);
        if (optional.isEmpty()) return null;

        Freelancer freelancer = optional.get();
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        if (passwordEncoder.matches(rawPassword, freelancer.getPassword())) {
            freelancer.setLastLoginDate(LocalDateTime.now());
            freelancerRepo.save(freelancer);
            return jwtUtil.generateJwtToken(freelancer.getFreelancerUserId(), freelancer.getEmail(), freelancer.getRole());
        } else {
            return null;
        }
    }

    public String checkAccountRegistrationStatus(String email) {
        Optional<Freelancer> freelancerOpt = freelancerRepo.findByEmail(email);

        if (freelancerOpt.isEmpty()) {
            return "not_found";
        }

        Freelancer freelancer = freelancerOpt.get();

        if (Boolean.TRUE.equals(freelancer.isEmailConfirmed())) {
            return "email_confirmed";
        } else {
            return "email_not_confirmed";
        }
    }
    
}