package com.urlshortener.url_shortener.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.url_shortener.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.util.RateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.print.attribute.standard.Media;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception
    {
        //Get Client IP Address
        String ipAddress = getClientIp(request);

        //Check if request is allowed
        if(!rateLimiterService.isAllowed(ipAddress)){
            log.warn("Rate Limit Exceeded for IP : {}",ipAddress);

            //Build Error Response Manually
            Map<String,Object> errorResponse = new HashMap<>();
            errorResponse.put("status",429);
            errorResponse.put("error","Too Many Requests");
            errorResponse.put("message","Rate Limit Exceeded. Max 10 Requests per minute.Please slow down");
            errorResponse.put("timestamp", LocalDateTime.now().toString());
            errorResponse.put("path", request.getRequestURI());

            //Add rate limit headers
            response.setHeader("x-RateLimit-Limit", String.valueOf(10));
            response.setHeader("X-RateLimit-Remaining",
                    String.valueOf(rateLimiterService.getRemainingRequests(ipAddress)));
            response.setHeader("Retry-After", "60");

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));

            //return false and we won't reach to controller
            return false;
        }
        // Add remaining requests header on every response
        response.setHeader("X-RateLimit-Limit", "10");
        response.setHeader("X-RateLimit-Remaining",
                String.valueOf(rateLimiterService.getRemainingRequests(ipAddress)));

        // Return true — request proceeds to controller
        return true;
    }

    //Extract Real IP - handles proxies and load balancers
    private String getClientIp(HttpServletRequest request) {
        // In a pure server setup, this is 100% secure and cannot be spoofed
        return request.getRemoteAddr();
    }
}
