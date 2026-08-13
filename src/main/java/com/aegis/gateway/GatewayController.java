package com.aegis.gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/gateway")
public class GatewayController {

    private static final String SECRET_KEY = "YOUR_HMAC_SECRET_KEY_HERE";

    @Autowired
    private AegisGroqEngine groqEngine;

    @Autowired
    private AegisRedisPublisher redisPublisher;

    @PostMapping("/process")
    public ResponseEntity<String> processRequest(@RequestBody Map<String, String> payload) {
        if (payload == null || !payload.containsKey("prompt") || !payload.containsKey("signature")) {
            return ResponseEntity.badRequest().body("Error: Missing prompt or signature.");
        }

        // REAL-TIME SANITIZER: Strips invisible terminal/OS payload characters
        String prompt = String.valueOf(payload.get("prompt")).replaceAll("[^\\x20-\\x7e]", "");
        String incomingSignature = String.valueOf(payload.get("signature")).replaceAll("[^\\x20-\\x7e]", "");

        try {
            // Calculate expected signature
            String calculatedSignature = AegisSecurityEngine.generateSignature(prompt, SECRET_KEY);

            // =========================================================================
            // PHASE 1: Real-Time Cryptographic Verification
            // =========================================================================
            if (!calculatedSignature.equals(incomingSignature)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Security Check Failed: Invalid Cryptographic Signature.\n");
            }

            // =========================================================================
            // PHASE 1.5: Deterministic Keyword Blacklisting (Heuristic Defense)
            // =========================================================================
            String normalizedPrompt = prompt.toLowerCase();
            String[] maliciousKeywords = {
                    "delete", "rm -rf", "drop table", "format", "mkfs", "rmdir",
                    "chmod", "chown", "wget", "curl", "nc -", "bash -i", "reverse shell",
                    "os.system", "subprocess", "root", "sudo", "ignore all previous"
            };

            for (String keyword : maliciousKeywords) {
                if (normalizedPrompt.contains(keyword)) {
                    System.out.println("⚠️ HEURISTIC TRIGGER: Blacklisted syntax detected -> " + keyword);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("SECURITY_BREACH: Heuristic filter detected destructive keyword. Payload blocked.\n");
                }
            }

            // =========================================================================
            // PHASE 2: Input Semantic AI Filtering (Prompt Scan)
            // =========================================================================
            boolean isInputAttack = AegisAiEngine.isPromptMalicious(prompt);
            if (isInputAttack) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("SECURITY_BREACH: AI detected Malicious Intent in Input Prompt. Payload blocked.\n");
            }

            // =========================================================================
            // PHASE 3: High-Speed LLM Code Generation via Groq LPU
            // =========================================================================
            System.out.println("🤖 Input Safe. Contacting Groq LPU for Code Generation...");
            String generatedPythonCode = groqEngine.generatePythonCode(prompt);
            System.out.println("--- GENERATED PYTHON CODE FROM LLM ---\n" + generatedPythonCode + "\n-------------------------------------");

            // =========================================================================
            // PHASE 4: Secondary Output Semantic AI Scan (The Zero-Trust Anchor)
            // =========================================================================
            boolean isOutputAttack = AegisAiEngine.isPromptMalicious(generatedPythonCode);
            if (isOutputAttack) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("SECURITY_BREACH: AI detected Malicious Code structure in LLM output. Generation blocked.\n");
            }

            // =========================================================================
            // PHASE 5: Asynchronous Hand-off to Tier 2 Sandbox Pipeline
            // =========================================================================
            redisPublisher.publishTask(prompt, generatedPythonCode);

            return ResponseEntity.ok("SUCCESS: Enclave verified payload. Workload pushed asynchronously to Redis Queue.\n");

        } catch (Exception e) {
            // ADD THIS LINE TO FORCE INTELLIJ TO PRINT THE CRASH LOG
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing pipeline: " + e.getMessage());
        }
    }
}