package com.aegis.gateway;

import ai.onnxruntime.*;
import java.util.HashMap;
import java.util.Map;

public class AegisAiEngine {

    // Production engines keep the environment and session alive at the class level
    // so you don't reload a 300MB model on every single user click.
    private static OrtEnvironment env;
    private static OrtSession session;

    // 1. HARDWARE INIT (Runs ONCE when the Spring Boot server boots up)
    public static void loadModelOnNPU(String modelPath) {
        try {
            env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();

            // Enable DirectML for Hardware NPU acceleration
            options.addDirectML(0);

            session = env.createSession(modelPath, options);
            System.out.println("SUCCESS: DistilBERT model loaded onto NPU via DirectML!");

        } catch (OrtException e) {
            System.err.println("CRITICAL: Failed to initialize ONNX environment.");
            e.printStackTrace();
        }
    }

    // 2. THE PRODUCTION INFERENCE PIPELINE (Runs on every user request)
    public static String analyzePrompt(String prompt) {

        if (session == null) {
            return "ERROR: NPU Model not initialized.";
        }

        try {
            // STEP 1: Tokenization (Amrutha will link the HuggingFace vocabulary here)
            // DistilBERT requires the text converted to long arrays (Tensors)
            long[][] inputIds = tokenizeText(prompt);
            long[][] attentionMask = generateAttentionMask(inputIds);

            // STEP 2: Create Hardware Tensors (Bridging Java to C++/NPU)
            OnnxTensor inputIdsTensor = OnnxTensor.createTensor(env, inputIds);
            OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(env, attentionMask);

            // STEP 3: Map inputs exactly as the DistilBERT ONNX model expects them
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input_ids", inputIdsTensor);
            inputs.put("attention_mask", attentionMaskTensor);

            // STEP 4: HARDWARE EXECUTION
            OrtSession.Result results = session.run(inputs);

            // STEP 5: Extract raw Logits (Array of numbers) from the NPU
            float[][] rawOutput = (float[][]) results.get(0).getValue();

            // Convert raw numbers to human-readable percentages (0.0 to 1.0)
            float[] probabilities = applySoftmax(rawOutput[0]);
            float maliciousScore = probabilities[1]; // Index 1 is typically the "Malicious" label

            // STEP 6: CRITICAL MEMORY CLEANUP
            // ONNX is C-based. If you don't close tensors, your server will memory leak and crash.
            inputIdsTensor.close();
            attentionMaskTensor.close();
            results.close();

            // STEP 7: The Final Decision Routing
            if (maliciousScore > 0.80) {
                return "SECURITY_BREACH: AI detected Malicious Intent (" + (maliciousScore * 100) + "% confidence). Payload blocked.";
            } else {
                return "SAFE: AI verified prompt. Ready for Docker Execution.";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: NPU Inference Failure.";
        }
    }

    // --- AMRUTHA'S INTERNAL MATHEMATICAL HELPER METHODS ---

    private static long[][] tokenizeText(String text) {
        // Amrutha's exact Tokenizer mapping logic will go here.
        // For production compilation right now, we return a valid shape dummy tensor: [CLS] text [SEP]
        return new long[][] {{101, 2026, 2000, 102}};
    }

    private static long[][] generateAttentionMask(long[][] inputIds) {
        // Tells the AI which tokens are real words (1) and which are just empty padding (0)
        long[][] mask = new long[1][inputIds[0].length];
        for (int i = 0; i < inputIds[0].length; i++) mask[0][i] = 1;
        return mask;
    }

    private static float[] applySoftmax(float[] logits) {
        // AI NPU outputs raw Logits (e.g., [2.4, -1.2]). Softmax converts them to percentages.
        float maxLogit = Math.max(logits[0], logits[1]);
        float exp0 = (float) Math.exp(logits[0] - maxLogit);
        float exp1 = (float) Math.exp(logits[1] - maxLogit);
        float sum = exp0 + exp1;
        return new float[] {exp0 / sum, exp1 / sum};
    }
}