package com.aegis.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/gateway")
public class GatewayController {

    // In production, this goes in a secure vault.
    private static final String SECRET_KEY = "Aegis_Private_Key_2026";

    @PostMapping("/process")
    public ResponseEntity<String> processRequest(@RequestBody Map<String, String> payload) {
        String prompt = payload.get("prompt");
        String incomingSignature = payload.get("signature");

        if (prompt == null || incomingSignature == null) {
            return ResponseEntity.badRequest().body("Error: Missing prompt or signature.");
        }

        try {
            // PHASE 1: Cryptographic Verification (Chetana's Code)
            String calculatedSignature = AegisSecurityEngine.generateSignature(prompt, SECRET_KEY);

            if (calculatedSignature.equals(incomingSignature)) {

                // PHASE 2: AI Intent Verification (Amrutha's Code)
                // We physically pass the verified string into the NPU pipeline
                String aiResponse = AegisAiEngine.analyzePrompt(prompt);

                // PHASE 3: The Final Decision Routing
                if (aiResponse.startsWith("SECURITY_BREACH")) {
                    // The AI caught a malicious prompt. Block the network request!
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(aiResponse);
                } else {
                    // The AI said it is safe. Pass it through!
                    return ResponseEntity.ok(aiResponse);
                }

            } else {
                // The Cryptography failed. Hacker tampered with the payload.
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Security Check Failed: Invalid Cryptographic Signature.");
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing security pipeline: " + e.getMessage());
        }
    }
}