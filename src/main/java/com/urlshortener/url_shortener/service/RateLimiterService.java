package com.urlshortener.url_shortener.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimiterService {

    private final RedisTemplate<String,String> redisTemplate;

    //Max requests allowed per window
    private static final int MAX_REQUESTS = 10;

    // Window size in minutes
    private static final long WINDOW_MINUTES = 1;

    // Key prefix
    private static final String RATE_LIMIT_PREFIX = "rate:";

    public boolean isAllowed(String ipAddress)
    {
        // Build key: rate:192.168.1.1:202504191030
        // (IP + current minute — changes every minute automatically)
        String currentMinute = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        String key = RATE_LIMIT_PREFIX + ipAddress + ":" + currentMinute;

        //Increment the counter for this IP in this minute
        long requestCount = redisTemplate.opsForValue().increment(key);

        //If this is my first request , then we need to set expiry of 1 minute
        //After 1 minute Redis auto-deletes the key window slides

        if(requestCount == 1){
            redisTemplate.expire(key,WINDOW_MINUTES, TimeUnit.MINUTES);
        }
        log.info("IP: {} | Requests this minute: {}/{}",
                ipAddress, requestCount, MAX_REQUESTS);

        // Return true if under limit, false if limit exceeded
        return requestCount <= MAX_REQUESTS;
    }

    //Get Remaining Requests
    public long getRemainingRequests(String ipAddress)
    {
        String currentMinute = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        String key = RATE_LIMIT_PREFIX + ipAddress+":"+currentMinute;

        String count = redisTemplate.opsForValue().get(key);
        if(count == null) return MAX_REQUESTS;

        long usedCount = Long.parseLong(count);

        return Math.max(0,MAX_REQUESTS - usedCount);
    }
}
