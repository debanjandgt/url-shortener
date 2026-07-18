package com.urlshortener.url_shortener.controller;

import com.urlshortener.url_shortener.dto.ShortenRequest;
import com.urlshortener.url_shortener.dto.ShortenResponse;
import com.urlshortener.url_shortener.service.UrlService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class UrlController {

    private final UrlService urlService;

    //POST /api/v1/shorten -- Creates a Short URl
    @PostMapping("/api/v1/shorten")
    public ResponseEntity<ShortenResponse> shortenUrl(
            @Valid @RequestBody ShortenRequest request) {

        log.info("Received Shorten Request for;{}",request.getOriginalUrl());
        ShortenResponse response = urlService.shortenUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    //GET /{shortCode} - redirect to originalURL
    @GetMapping("/{shortCode}")
    public void redirect(
            @PathVariable String shortCode,
            HttpServletResponse response) throws IOException {

        log.info("Redirect request for short code : {}",shortCode);
        String originalUrl = urlService.getOriginalUrl(shortCode);
        response.sendRedirect(originalUrl);
    }

    //GET /api/v1/info/{shortCode} - get info without redirecting
    @GetMapping("/api/v1/info/{shortCode}")
    public ResponseEntity<ShortenResponse> getInfo(
            @PathVariable String shortCode
    )
    {
        String originalUrl = urlService.getOriginalUrl(shortCode);
        return ResponseEntity.ok(
                ShortenResponse.builder().shortCode(shortCode).shortUrl("http://localhost:8080/"+shortCode).originalUrl(originalUrl).build()
        );
    }
}
