package com.aegis.gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@Service
public class AegisRedisPublisher {
    @Autowired
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // The channel name your Tier 2 Python worker will listen to
    private static final String REDIS_CHANNEL = "aegis-tasks";

    public void publishTask(String prompt, String verifiedCode) {
        try {
            // Build a clean structured envelope for Tier 2 execution
            Map<String, String> messageEnvelope = Map.of(
                    "status", "VERIFIED",
                    "prompt", prompt,
                    "code", verifiedCode
            );

            String jsonMessage = objectMapper.writeValueAsString(messageEnvelope);

            System.out.println("🚀 [REDIS] Publishing verified workload to channel: " + REDIS_CHANNEL);
            redisTemplate.convertAndSend(REDIS_CHANNEL, jsonMessage);

        } catch (Exception e) {
            System.err.println("❌ [REDIS ERROR] Failed to queue task payload: " + e.getMessage());
            throw new RuntimeException("Redis Enqueue Failure: " + e.getMessage());
        }
    }
}

