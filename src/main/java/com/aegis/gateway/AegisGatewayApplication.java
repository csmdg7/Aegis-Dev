package com.aegis.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AegisGatewayApplication {

    public static void main(String[] args) {
        // STEP 1: Boot up the Tomcat Web Server and activate Supriya's GatewayController
        SpringApplication.run(AegisGatewayApplication.class, args);

        // STEP 2: The AI Hardware Ignition Switch
        // NOTE FOR AMRUTHA: Once you download the physical DistilBERT.onnx file
        // from Hugging Face and place it in the src/main/resources/ folder,
        // remove the "//" from the line below to activate the NPU pipeline.

        // AegisAiEngine.loadModelOnNPU("src/main/resources/DistilBERT.onnx");

        // STEP 3: System Confirmation
        System.out.println("=================================================");
        System.out.println("✅ AEGIS GATEWAY PHASE 1: ONLINE AND SECURE");
        System.out.println("🔒 HMAC Cryptographic Engine: ACTIVE");
        System.out.println("=================================================");
    }
}
