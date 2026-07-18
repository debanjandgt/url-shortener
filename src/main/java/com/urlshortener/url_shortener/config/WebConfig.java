package com.urlshortener.url_shortener.config;

import com.urlshortener.url_shortener.interceptor.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final RateLimitInterceptor rateLimitInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                // Apply rate limiting to these endpoints only
                .addPathPatterns("/api/v1/**", "/{shortCode}")
                // Exclude health check if you add one later
                .excludePathPatterns("/actuator/**");
    }
}
