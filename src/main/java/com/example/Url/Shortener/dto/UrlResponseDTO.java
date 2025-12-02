package com.example.Url.Shortener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UrlResponseDTO {
    private String shortUrl;
    private String originalUrl;
    private long clicks;
    private LocalDateTime expiryAt;
}
