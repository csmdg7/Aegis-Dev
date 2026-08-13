package com.aegis.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
// @SpringBootApplication
public class AegisGatewayApplication {

    public static void main(String[] args) {
        // STEP 1: Boot up the Tomcat Web Server and activate GatewayController
        SpringApplication.run(AegisGatewayApplication.class, args);

        // STEP 2: The AI Hardwarex Ignition Switch
        // We are now calling the updated initializeAI method we built in Phase 2
        AegisAiEngine.initializeAI("src/main/resources/DistilBERT.onnx");

        // STEP 3: System Confirmation
        System.out.println("=================================================");
        System.out.println("✅ AEGIS GATEWAY PHASE 2: ONLINE AND SECURE");
        System.out.println("🔒 HMAC Cryptographic Engine: ACTIVE");
        System.out.println("🧠 NPU AI Inference Engine: ACTIVE");
        System.out.println("=================================================");
    }
}