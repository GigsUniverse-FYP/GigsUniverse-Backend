package com.giguniverse.backend.Onboarding.Sumsub;

import java.util.Map;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.giguniverse.backend.Auth.Session.AuthUtil;



@RestController
@RequestMapping("/api/sumsub")
public class SumsubController {

    private static final Logger logger = Logger.getLogger(SumsubController.class.getName());

    @Autowired
    private SumsubService sumsubService;

    @GetMapping("/freelancer/permalink")
    public ResponseEntity<?> getFreelancerSumsubPermalink() {
        try {
            String userId = AuthUtil.getUserId();
            String email = AuthUtil.getUserEmail();

            logger.info("Incoming permalink request - userId: " + userId + ", email: " + email);

            if (userId == null || email == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "User session missing. Please login again."
                ));
            }

            String link = sumsubService.generateFreelancerWebSdkPermalink(userId, email);

            logger.info("Sumsub permalink generated: " + link);

            return ResponseEntity.ok(Map.of("url", link));

        } catch (Exception e) {
            logger.severe("Failed to generate Sumsub link: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to generate Sumsub permalink",
                "details", e.getMessage()
            ));
        }
    }

    @GetMapping("/employer/permalink")
    public ResponseEntity<?> getFreelancerEmployerPermalink() {
        try {
            String userId = AuthUtil.getUserId();
            String email = AuthUtil.getUserEmail();

            logger.info("Incoming permalink request - userId: " + userId + ", email: " + email);

            if (userId == null || email == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "User session missing. Please login again."
                ));
            }

            String link = sumsubService.generateEmployerWebSdkPermalink(userId, email);

            logger.info("Sumsub permalink generated: " + link);

            return ResponseEntity.ok(Map.of("url", link));

        } catch (Exception e) {
            logger.severe("Failed to generate Sumsub link: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to generate Sumsub permalink",
                "details", e.getMessage()
            ));
        }
    }
}
