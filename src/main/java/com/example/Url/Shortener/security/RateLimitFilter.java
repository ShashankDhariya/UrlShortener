package com.example.Url.Shortener.security;

import io.github.bucket4j.*;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter implements Filter {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket createNewBuckets() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(
                        50,
                        Refill.intervally(50, Duration.ofMinutes(1))
                ))
                .build();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String ip = req.getRemoteAddr();

        Bucket bucket = buckets.computeIfAbsent(ip, k -> createNewBuckets());

        if(bucket.tryConsume(1)) {
             chain.doFilter(request, response);
        }

        else {
            HttpServletResponse res = (HttpServletResponse) response;
            res.setStatus(429);
            res.getWriter().write("Rate Limit Exceeded. Try again later.");
        }
    }
}
