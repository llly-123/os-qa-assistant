package com.xidian.osqa.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 接口限流器（基于令牌桶算法的简化实现）
 * 支持按用户ID和IP地址限流
 */
@Component
public class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    // 限流配置
    private static final int CHAT_MAX_REQUESTS = 20;      // 聊天接口：每分钟最多20次
    private static final int CHAT_WINDOW_SECONDS = 60;
    private static final int LOGIN_MAX_REQUESTS = 5;       // 登录接口：每分钟最多5次
    private static final int LOGIN_WINDOW_SECONDS = 60;
    private static final int DEFAULT_MAX_REQUESTS = 30;    // 默认：每分钟30次
    private static final int DEFAULT_WINDOW_SECONDS = 60;

    // 限流记录：<key, RateLimitEntry>
    private final Map<String, RateLimitEntry> rateLimitMap = new ConcurrentHashMap<>();

    // 定期清理过期记录（每5分钟）
    private volatile long lastCleanupTime = System.currentTimeMillis();
    private static final long CLEANUP_INTERVAL = 5 * 60 * 1000;

    /**
     * 检查聊天接口限流
     */
    public boolean allowChatRequest(Long userId, String ip) {
        cleanupIfNeeded();
        String userKey = "chat:user:" + userId;
        String ipKey = "chat:ip:" + ip;
        boolean userAllowed = checkAndRecord(userKey, CHAT_MAX_REQUESTS, CHAT_WINDOW_SECONDS);
        boolean ipAllowed = checkAndRecord(ipKey, CHAT_MAX_REQUESTS * 3, CHAT_WINDOW_SECONDS);
        if (!userAllowed) {
            log.warn("用户聊天限流触发: userId={}", userId);
        }
        if (!ipAllowed) {
            log.warn("IP聊天限流触发: ip={}", ip);
        }
        return userAllowed && ipAllowed;
    }

    /**
     * 检查登录接口限流
     */
    public boolean allowLoginRequest(String ip) {
        cleanupIfNeeded();
        String ipKey = "login:ip:" + ip;
        boolean allowed = checkAndRecord(ipKey, LOGIN_MAX_REQUESTS, LOGIN_WINDOW_SECONDS);
        if (!allowed) {
            log.warn("登录限流触发: ip={}", ip);
        }
        return allowed;
    }

    /**
     * 检查默认接口限流
     */
    public boolean allowRequest(String key) {
        cleanupIfNeeded();
        return checkAndRecord("default:" + key, DEFAULT_MAX_REQUESTS, DEFAULT_WINDOW_SECONDS);
    }

    /**
     * 核心限流检查逻辑（滑动窗口计数器）
     */
    private boolean checkAndRecord(String key, int maxRequests, int windowSeconds) {
        long now = System.currentTimeMillis();
        long windowStart = now - windowSeconds * 1000L;

        RateLimitEntry entry = rateLimitMap.computeIfAbsent(key, k -> new RateLimitEntry());

        synchronized (entry) {
            // 如果窗口已过期，重置
            if (entry.windowStart < windowStart) {
                entry.windowStart = now;
                entry.count.set(0);
            }

            int current = entry.count.incrementAndGet();
            return current <= maxRequests;
        }
    }

    /**
     * 定期清理过期记录，防止内存泄漏
     */
    private void cleanupIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime < CLEANUP_INTERVAL) return;
        lastCleanupTime = now;

        long threshold = now - 10 * 60 * 1000; // 清理10分钟前的记录
        rateLimitMap.entrySet().removeIf(e -> e.getValue().windowStart < threshold);
        log.debug("限流记录清理完成，当前记录数: {}", rateLimitMap.size());
    }

    /**
     * 限流记录条目
     */
    private static class RateLimitEntry {
        long windowStart = System.currentTimeMillis();
        AtomicInteger count = new AtomicInteger(0);
    }
}
