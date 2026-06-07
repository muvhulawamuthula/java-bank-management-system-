package com.bankafrica.bankingapp.security;

import com.bankafrica.bankingapp.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-client-IP throttle on the unauthenticated credential endpoints
 * ({@code POST /api/auth/login} and {@code /register}). A classic token bucket caps how
 * many attempts an IP can make per window, blunting brute-force password guessing and
 * account-enumeration sweeps. When the bucket is empty the request is rejected with
 * {@code 429 Too Many Requests} in the same {@link ApiError} envelope as every other error,
 * plus a {@code Retry-After} header.
 *
 * <p>The limiter is in-memory and per-instance — adequate for a single node; a clustered
 * deployment would back the buckets with Redis. It runs before authentication so rejected
 * requests never reach the database.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    /** Defensive cap so a flood of distinct source IPs can't grow the map without bound. */
    private static final int MAX_TRACKED_CLIENTS = 100_000;

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final int capacity;
    private final long refillPeriodNanos;

    public RateLimitingFilter(ObjectMapper objectMapper,
                              @Value("${app.ratelimit.auth.capacity:10}") int capacity,
                              @Value("${app.ratelimit.auth.refill-seconds:60}") long refillSeconds) {
        this.objectMapper = objectMapper;
        this.capacity = capacity;
        this.refillPeriodNanos = refillSeconds * 1_000_000_000L;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        return !("/api/auth/login".equals(uri) || "/api/auth/register".equals(uri));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientKey = clientIp(request);
        if (buckets.size() >= MAX_TRACKED_CLIENTS) {
            buckets.clear(); // crude pressure valve; resets everyone's allowance, never leaks memory
        }
        TokenBucket bucket = buckets.computeIfAbsent(clientKey,
                k -> new TokenBucket(capacity, refillPeriodNanos));

        if (bucket.tryConsume()) {
            filterChain.doFilter(request, response);
        } else {
            rejectTooManyRequests(request, response);
        }
    }

    private void rejectTooManyRequests(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(refillPeriodNanos / 1_000_000_000L));
        ApiError error = ApiError.of(status.value(), status.getReasonPhrase(),
                "Too many attempts. Please try again later.", request.getRequestURI());
        objectMapper.writeValue(response.getWriter(), error);
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim(); // first hop = original client
        }
        return request.getRemoteAddr();
    }

    /** Test/diagnostics hook: drop all tracked buckets. */
    public void reset() {
        buckets.clear();
    }

    /**
     * A lazily-refilled token bucket. Starts full; each consumed token is regenerated at a
     * steady rate of {@code capacity} tokens per refill period, so the long-run allowance is
     * {@code capacity} attempts per window while still permitting a short initial burst.
     */
    private static final class TokenBucket {
        private final int capacity;
        private final double tokensPerNano;
        private double tokens;
        private long lastRefillNanos;

        TokenBucket(int capacity, long refillPeriodNanos) {
            this.capacity = capacity;
            this.tokensPerNano = (double) capacity / refillPeriodNanos;
            this.tokens = capacity;
            this.lastRefillNanos = System.nanoTime();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            long elapsed = now - lastRefillNanos;
            if (elapsed > 0) {
                tokens = Math.min(capacity, tokens + elapsed * tokensPerNano);
                lastRefillNanos = now;
            }
        }
    }
}
