package com.giguniverse.backend.Auth.Session;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// This Section is for Debug Purposes 
@RestController
@RequestMapping("/api/auth")
public class JwtSessionController {

    @GetMapping("/current-id")
    public ResponseEntity<?> getCurrentUserId() {
        String userId = AuthUtil.getUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        return ResponseEntity.ok(Map.of("currentId", userId));
    }

    @GetMapping("/current-role")
    public ResponseEntity<?> getCurrentUserRole() {
        String role = AuthUtil.getUserRole();
        if (role == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        return ResponseEntity.ok(Map.of("currentRole", role));
    }

    @GetMapping("/current-email")
    public ResponseEntity<?> getCurrentUserEmail() {
        String email = AuthUtil.getUserEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        return ResponseEntity.ok(Map.of("currentEmail", email));
    }
}
