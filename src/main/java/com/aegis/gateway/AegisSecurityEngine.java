package com.aegis.gateway;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AegisSecurityEngine {
    private static final String ALGORITHM = "HmacSHA256";

    public static String generateSignature(String data, String secretKey) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(secretKeySpec);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(rawHmac);
    }

    public static void main(String[] args) {
        try {
            String myPrompt = "BTS update 2026";
            String mySecret = "Aegis_Private_Key_2026";
            String signature = generateSignature(myPrompt, mySecret);

            System.out.println("--- AEGIS GATEWAY SECURITY CHECK ---");
            System.out.println("Prompt: " + myPrompt);
            System.out.println("HMAC Signature: " + signature);
            System.out.println("-------------------------------------");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
