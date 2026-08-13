package com.aegis.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.List;

@Service
public class AegisGroqEngine {

    @Value("${groq.api.key}")
    private String apiKey;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generatePythonCode(String userPrompt) throws Exception {
        // System prompt instructs Groq to return ONLY clean, raw python code
        String systemInstruction = "You are an AI coding assistant. Generate ONLY pure executable Python code based on the user request. " +
                "Do NOT include markdown blocks like ```python or explanations. Just return the raw code lines.";

        // Build the OpenAI-compatible JSON payload for Groq
        Map<String, Object> payload = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(
                        Map.of("role", "system", "content", systemInstruction),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.1 // Low temperature ensures deterministic, safe code structures
        );

        String jsonRequestBody = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonNode rootNode = objectMapper.readTree(response.body());
            // Extract the generated code content from the JSON response tree
            return rootNode.path("choices").get(0).path("message").path("content").asText();
        } else {
            throw new RuntimeException("Groq API failed with Status Code: " + response.statusCode() + " | Error: " + response.body());
        }
    }
}