package com.urlshortener.url_shortener.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {
    private final RedisTemplate<String,String> redisTemplate;

    //Cache Key Prefix - avoids collision with other Keys in Redis
    private static final String URL_CACHE_PREFIX = "url:";

    //How Long to keep a URL in chache -24 hours
    private static final long CACHE_TTL_EXPIRE = 24;

    //Stores  a shortCode - > originalUrl mapping in Redis
    public void cacheUrl(String shortCode,String originalUrl)
    {
        String key= URL_CACHE_PREFIX + shortCode;
        redisTemplate.opsForValue().set(key,originalUrl,CACHE_TTL_EXPIRE, TimeUnit.HOURS);
        log.info("Cached URL for ShortCode: {}",shortCode);
    }

    //Retrieve original URL from Redis
    public String getCachedUrl(String shortCode)
    {
        String key= URL_CACHE_PREFIX + shortCode;
        String cachedUrl = redisTemplate.opsForValue().get(key);

        if(cachedUrl!=null){
            log.info("Cache Hit for ShortCode: {}",shortCode);
        }
        else{
            log.info("Cache Miss for shortCode : {}", shortCode);
        }
        return cachedUrl;
    }

    //Remove a URL from Cache when URL expires
    public void evictUrl(String shortCode)
    {
        String key= URL_CACHE_PREFIX + shortCode;
        redisTemplate.delete(key);
        log.info("Evicted Cache for shortCode: {}",shortCode);
    }

    //Check if a shortCode exists in Cache
    public boolean isCached(String shortCode)
    {
        String key=URL_CACHE_PREFIX + shortCode;
        return redisTemplate.hasKey(key);
    }
}
