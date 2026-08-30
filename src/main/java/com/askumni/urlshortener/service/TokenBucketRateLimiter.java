package com.askumni.urlshortener.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TOKEN BUCKET ALGORITHM — explained simply:
 *
 * Imagine each client (identified by IP or API key) has a bucket that holds
 * a fixed number of tokens (e.g. 10). Every request costs 1 token.
 * - Bucket refills at a fixed rate (e.g. 10 tokens every 60 seconds).
 * - If the bucket is empty, the request is rejected (HTTP 429).
 * - This allows short bursts (use all 10 tokens at once) while still
 *   enforcing an average rate over time — better UX than a hard "1 req/6s" rule.
 *
 * WHY THIS OVER OTHER ALGORITHMS (mention this trade-off in interviews):
 * - Fixed Window: simple but allows 2x burst at window boundaries
 * - Sliding Window Log: accurate but memory-heavy (stores every timestamp)
 * - Token Bucket: good balance of burst tolerance + memory efficiency <- chosen
 *
 * NOTE: This in-memory version works for a single instance. For a real
 * distributed system (multiple app instances), you'd move bucket state to
 * Redis (using INCR + EXPIRE or a Lua script) so all instances share state.
 * That's a great follow-up point to mention proactively in an interview.
 */
@Component
public class TokenBucketRateLimiter {

    @Value("${app.ratelimit.capacity:10}")
    private int capacity;

    @Value("${app.ratelimit.refill-tokens:10}")
    private int refillTokens;

    @Value("${app.ratelimit.refill-duration-seconds:60}")
    private int refillDurationSeconds;

    // key = client identifier (IP address), value = that client's bucket
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryConsume(String clientKey) {
        Bucket bucket = buckets.computeIfAbsent(clientKey,
                k -> new Bucket(capacity, System.currentTimeMillis()));
        return bucket.tryConsume(capacity, refillTokens, refillDurationSeconds * 1000L);
    }

    /**
     * Represents one client's token bucket.
     * Uses a synchronized method for simplicity; in production at scale you'd
     * want a lock-free approach or push this logic into Redis.
     */
    private static class Bucket {
        private double tokens;
        private long lastRefillTimestamp;

        Bucket(double initialTokens, long now) {
            this.tokens = initialTokens;
            this.lastRefillTimestamp = now;
        }

        synchronized boolean tryConsume(int capacity, int refillTokens, long refillDurationMillis) {
            refill(capacity, refillTokens, refillDurationMillis);
            if (tokens >= 1) {
                tokens -= 1;
                return true;
            }
            return false;
        }

        private void refill(int capacity, int refillTokens, long refillDurationMillis) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTimestamp;
            if (elapsed <= 0) return;

            double tokensToAdd = (elapsed / (double) refillDurationMillis) * refillTokens;
            if (tokensToAdd > 0) {
                tokens = Math.min(capacity, tokens + tokensToAdd);
                lastRefillTimestamp = now;
            }
        }
    }
}
