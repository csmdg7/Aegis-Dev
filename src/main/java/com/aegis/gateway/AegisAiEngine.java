package com.aegis.gateway;

import ai.onnxruntime.*;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.huggingface.tokenizers.Encoding;

import java.util.HashMap;
import java.util.Map;

public class AegisAiEngine {

    private static OrtEnvironment env;
    private static OrtSession session;
    private static HuggingFaceTokenizer tokenizer;

    // 1. HARDWARE INIT: Boot up Tokenizer and NPU Session
    public static void initializeAI(String modelPath) {
        try {
            tokenizer = HuggingFaceTokenizer.builder()
                    .optTokenizerName("distilbert-base-uncased")
                    .build();

            env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();

            // options.addDirectML(0); // Uncomment ONLY if using Windows DirectML NPU

            session = env.createSession(modelPath, options);
            System.out.println("✅ SUCCESS: Tokenizer and DistilBERT model loaded!");

        } catch (Exception e) {
            System.err.println("CRITICAL: Failed to initialize AI environment.");
            e.printStackTrace();
        }
    }

    // 2. INFERENCE PIPELINE: Returns boolean for GatewayController
    public static boolean isPromptMalicious(String prompt) {
        if (session == null || tokenizer == null) {
            System.err.println("ERROR: AI not initialized.");
            return true;
        }

        try {
            Encoding encoding = tokenizer.encode(prompt);
            long[] rawIds = encoding.getIds();
            long[] rawMask = encoding.getAttentionMask();

            long[][] inputIds2D = { rawIds };
            long[][] attentionMask2D = { rawMask };

            try (OnnxTensor inputIdsTensor = OnnxTensor.createTensor(env, inputIds2D);
                 OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(env, attentionMask2D)) {

                Map<String, OnnxTensor> inputs = new HashMap<>();
                inputs.put("input_ids", inputIdsTensor);
                inputs.put("attention_mask", attentionMaskTensor);

                try (OrtSession.Result results = session.run(inputs)) {

                    float[][] rawOutput = (float[][]) results.get(0).getValue();

                    // CORRECTED MODEL MAPPING for protectai/fmops-distilbert
                    float maliciousScore = rawOutput[0][0]; // Index 0 = Injection
                    float safeScore = rawOutput[0][1];      // Index 1 = Legitimate

                    // 1. Define the strict mathematical threshold dynamically
                    final double THRESHOLD = 0.7f;

                    // 2. Apply the Softmax Algorithm
                    double expMalicious = Math.exp(maliciousScore);
                    double expSafe = Math.exp(safeScore);
                    double totalExp = expMalicious + expSafe;
                    double maliciousProbability = expMalicious / totalExp;

                    // LIVE DEMO DEBUG TRACE
                    System.out.println("\n--- 🧠 AI INFERENCE METRICS ---");
                    System.out.println("Prompt: [" + prompt + "]");
                    System.out.println("Malicious Score Probability (Idx 0): " + String.format("%.5f", maliciousProbability));
                    System.out.println("Safe Score Probability (Idx 1): " + safeScore);
                    System.out.println("Active Enclave Threshold: " + THRESHOLD);
                    System.out.println("-----------------------------\n");

                    return maliciousScore > THRESHOLD;
                }
            }
        } catch (Exception e) {
            System.err.println("Inference execution error. Failing secure.");
            e.printStackTrace();
            return true;
        }
    }
}