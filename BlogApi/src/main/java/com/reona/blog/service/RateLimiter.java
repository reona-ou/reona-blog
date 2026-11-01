package com.reona.blog.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiter {

    private static final long CLEANUP_IDLE_MS = 60 * 60 * 1000L;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key, int maxRequests, long windowMs) {
        long now = System.currentTimeMillis();
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket());
        synchronized (bucket) {
            bucket.lastAccess = now;
            prune(bucket, now, windowMs);
            if (bucket.times.size() >= maxRequests) {
                return false;
            }
            bucket.times.addLast(now);
            return true;
        }
    }

    public boolean isBlocked(String key, int maxRequests, long windowMs) {
        Bucket bucket = buckets.get(key);
        if (bucket == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        synchronized (bucket) {
            bucket.lastAccess = now;
            prune(bucket, now, windowMs);
            if (bucket.times.isEmpty()) {
                buckets.remove(key, bucket);
                return false;
            }
            return bucket.times.size() >= maxRequests;
        }
    }

    public void recordFailure(String key, long windowMs) {
        long now = System.currentTimeMillis();
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket());
        synchronized (bucket) {
            bucket.lastAccess = now;
            prune(bucket, now, windowMs);
            bucket.times.addLast(now);
        }
    }

    public void reset(String key) {
        buckets.remove(key);
    }

    private void prune(Bucket bucket, long now, long windowMs) {
        while (!bucket.times.isEmpty() && now - bucket.times.peekFirst() > windowMs) {
            bucket.times.pollFirst();
        }
    }

    @Scheduled(fixedDelay = 10 * 60 * 1000L)
    public void cleanup() {
        long cutoff = System.currentTimeMillis() - CLEANUP_IDLE_MS;
        buckets.entrySet().removeIf(entry -> entry.getValue().lastAccess < cutoff);
    }

    private static final class Bucket {
        private final Deque<Long> times = new ArrayDeque<>();
        private long lastAccess = System.currentTimeMillis();
    }
}
