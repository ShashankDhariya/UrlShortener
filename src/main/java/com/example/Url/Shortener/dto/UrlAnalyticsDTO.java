package com.example.Url.Shortener.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UrlAnalyticsDTO {
    private String originalUrl;
    private String shortUrl;
    private Long clicks;
    private LocalDateTime createdAt;
    private LocalDateTime expiryAt;
    private Boolean expired;
}
