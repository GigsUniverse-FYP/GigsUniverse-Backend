package com.giguniverse.backend.Auth.Controller;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.giguniverse.backend.Auth.DTO.ForgotPasswordRequest;
import com.giguniverse.backend.Auth.DTO.FreelancerRegisterRequest;
import com.giguniverse.backend.Auth.DTO.ResetPasswordRequest;
import com.giguniverse.backend.Auth.DTO.VerifyCodeRequest;
import com.giguniverse.backend.Auth.JWT.JwtConfig;
import com.giguniverse.backend.Auth.Service.FreelancerAuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth/freelancer")
public class FreelancerAuthController {

    @Autowired
    private FreelancerAuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody FreelancerRegisterRequest request) {
        return authService.register(request);
    }

    @GetMapping("/confirm-email")
    public ResponseEntity<?> confirmEmail(@RequestParam String token) {
        return authService.confirmEmail(token);
    }

    @GetMapping("/check-id")
    public ResponseEntity<?> checkIdAvailability(@RequestParam("id") String freelancerId) {
        return authService.checkIdAvailability(freelancerId);
    }

    @PostMapping("/resend-confirmation")
    public ResponseEntity<?> resendEmail(@RequestBody Map<String, String> body) {
        try {
            authService.resendConfirmationEmail(body.get("email"));
            return ResponseEntity.ok("Confirmation email sent.");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to resend.");
        }
    }

    @GetMapping("/oauth2-init")
    public void initOAuth(@RequestParam("id") String id, HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        session = request.getSession(true);
        session.setAttribute("pendingFreelancerId", id);
        response.sendRedirect("/oauth2/authorization/google-freelancer");
    }

    // password reset section
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgot(@RequestBody ForgotPasswordRequest r) {
        authService.forgotPassword(r.email);
        return ResponseEntity.ok("If valid, a code was sent.");
    }

    @PostMapping("/verify-reset-code")
    public ResponseEntity<?> verify(@RequestBody VerifyCodeRequest r) {
        System.out.println("Verifying reset code for email: " + r.email + ", code: " + r.code);

        boolean valid = authService.verifyCode(r.email.trim(), r.code.trim());

        if (valid) {
            return ResponseEntity.ok("Valid code");
        } else {
            return ResponseEntity.status(400).body("Invalid code");
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> reset(@RequestBody ResetPasswordRequest r) {
        try {
        authService.resetPassword(r.email, r.code, r.newPassword);
        return ResponseEntity.ok("Password reset successful");
        } catch (IllegalArgumentException e) {
        return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @PostMapping("/resend-reset-code")
    public ResponseEntity<?> resendResetCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Email is required.");
        }

        try {
            authService.resendResetCode(email);
            return ResponseEntity.ok("Reset code resent successfully.");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An unexpected error occurred.");
        }
    }

    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmailExists(@RequestParam("email") String email) {
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Email is required.");
        }

        boolean exists = authService.emailExists(email);
        if (exists) {
            return ResponseEntity.ok("Email exists.");
        } else {
            return ResponseEntity.status(404).body("Email not found.");
        }
    }

    @GetMapping("/check-provider")
    public ResponseEntity<?> checkProvider(@RequestParam("email") String email) {
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Email is required.");
        }

        try {
            String provider = authService.getProviderByEmail(email);

            if ("email".equalsIgnoreCase(provider)) {
                return ResponseEntity.ok().body("Email provider");
            } else if ("google".equalsIgnoreCase(provider)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Account registered with Google. Please use Google login.");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Unknown provider: " + provider);
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @Autowired
    private JwtConfig jwtConfig;

    @PostMapping("/email-login")
    public ResponseEntity<?> emailLogin(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Email and password are required.");
        }

        try {
            String jwt = authService.emailProviderLogin(email, password);

            if (jwt == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password.");
            }

            // Create secure HTTP-only cookie for JWT
            ResponseCookie cookie = ResponseCookie.from("jwt", jwt)
                    .httpOnly(true)
                    .secure(true) // Set to 'false' if testing over HTTP (not HTTPS)
                    .path("/")
                    .maxAge(Duration.ofMillis(jwtConfig.getExpiration()))
                    .sameSite("None")
                    .domain(".gigsuniverse.studio")
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(Map.of("message", "Login successful as Freelancer"));

        } catch (Exception e) {
            e.printStackTrace(); 
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Login failed.");
        }
    }
    
    @GetMapping("/check-registration-status")
    public ResponseEntity<String> checkRegistrationStatus(@RequestParam String email) {
        String status = authService.checkAccountRegistrationStatus(email);

        switch (status) {
            case "email_confirmed":
                return ResponseEntity.ok("Email is confirmed.");
            case "email_not_confirmed":
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Email is not confirmed.");
            default:
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No account found.");
        }
    }

}
