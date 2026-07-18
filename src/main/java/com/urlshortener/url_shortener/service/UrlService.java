package com.urlshortener.url_shortener.service;


import com.urlshortener.url_shortener.dto.ShortenRequest;
import com.urlshortener.url_shortener.dto.ShortenResponse;
import com.urlshortener.url_shortener.entity.Url;
import com.urlshortener.url_shortener.repository.UrlRepository;
import com.urlshortener.url_shortener.util.Base62Util;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

import com.urlshortener.url_shortener.exception.AliasAlreadyExistsException;
import com.urlshortener.url_shortener.exception.UrlExpiredException;
import com.urlshortener.url_shortener.exception.UrlNotFoundException;
@Service
@RequiredArgsConstructor
@Slf4j

public class UrlService {
    private final UrlRepository urlRepository;
    private final Base62Util base62Util;
    private final CacheService cacheService;

    @Value("${app.base-url:http://localhost:8080/}")
    private String baseUrl;

    @Transactional
    public ShortenResponse shortenUrl(ShortenRequest request)
    {
        //2.Handle custom alias
        if(request.getCustomAlias() != null && !request.getCustomAlias().isBlank())
        {
            if(urlRepository.existsByShortCode(request.getCustomAlias()))
            {
                throw new AliasAlreadyExistsException("Alias '" + request.getCustomAlias() + "' is already taken.");
            }
            return saveAndResponse(request.getOriginalUrl(), request.getCustomAlias());
        }

        //1.Check if this URl is already Shortened - return existing shortCode if they exists
         Optional<Url> existing = urlRepository.findByOriginalUrl(request.getOriginalUrl());


        if(existing.isPresent())
        {
            log.info("URL already exists,hence returning short code");
            return buildResponse(existing.get());
        }



        //3.Save first with empty shortCode to get the auto generated-ID
        Url url = Url.builder().originalUrl(request.getOriginalUrl()).shortCode("temp").clickCount(0L).build();
        Url saved = urlRepository.save(url);

        //4.Use the ID to generate BASE62 Short Code
        String shortCode = base62Util.encode((saved.getId()));
        saved.setShortCode(shortCode);
        urlRepository.save(saved);

        log.info("Created Shortened URl : {} for Original : {}",shortCode,request.getOriginalUrl());
        //Need to save this new shortCode and URL Combo in our redis as well.
        cacheService.cacheUrl(saved.getShortCode(), saved.getOriginalUrl());
        return buildResponse(saved);

    }
    @Transactional
    public String getOriginalUrl(String shortCode)
    {
        // STEP 1: Check Redis cache first
        String cachedUrl = cacheService.getCachedUrl(shortCode);
        if (cachedUrl != null) {
            // Cache hit — return immediately, no DB query needed
            return cachedUrl;
        }
        //In Case of cache Miss
        Url url = urlRepository.findByShortCode(shortCode).orElseThrow(() -> new UrlNotFoundException("No URL found for short code: " + shortCode));
        //Check if URL has expired or not
        if(url.getExpiresAt() != null && url.getExpiresAt().isBefore(LocalDateTime.now()))
        {
            // Evict from cache if somehow it got cached
            cacheService.evictUrl(shortCode);
            throw new UrlExpiredException("The short URL '" + shortCode + "' has expired.");

        }


        //Cache this ShortCode
        cacheService.cacheUrl(shortCode,url.getOriginalUrl());

        //Increment Click Count
        url.setClickCount(url.getClickCount()+1);
        urlRepository.save(url);

        return url.getOriginalUrl();
    }

    private ShortenResponse saveAndResponse(String originalUrl,String shortCode)
    {
        Url url= Url.builder().originalUrl(originalUrl).shortCode(shortCode).clickCount(0L).build();
        Url saved = urlRepository.save(url);
        // Cache immediately
        cacheService.cacheUrl(shortCode, originalUrl);
        return buildResponse(saved);
    }

    private ShortenResponse buildResponse(Url url)
    {
        return ShortenResponse.builder()
                .shortCode(url.getShortCode())
                .shortUrl(baseUrl+url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .expiresAt(url.getExpiresAt())
                .clickCount(url.getClickCount())
                .build();
    }

}
