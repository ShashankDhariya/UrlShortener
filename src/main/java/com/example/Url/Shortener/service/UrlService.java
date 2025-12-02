package com.example.Url.Shortener.service;

import com.example.Url.Shortener.dto.UrlAnalyticsDTO;
import com.example.Url.Shortener.dto.UrlRequestDTO;
import com.example.Url.Shortener.dto.UrlResponseDTO;
import com.example.Url.Shortener.entity.UrlEntity;
import com.example.Url.Shortener.repository.UrlRepository;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.util.InvalidUrlException;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
public class UrlService {
    private final UrlRepository urlRepository;

    UrlService(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    public UrlResponseDTO createShortUrl(UrlRequestDTO request) {
        String originalUrl = sanitise(request.getUrl());
        blockInternalUrls(originalUrl);
        validateUrl(originalUrl);

        String finalShort;

        if(request.getCustom() != null && !request.getCustom().isBlank()) {
            if(urlRepository.existsByShortUrl(request.getCustom())) {
                throw new RuntimeException("Custom URL already exists.");
            }

            finalShort = request.getCustom();
        }

        else {
            finalShort = generateRandomCode();
        }


        UrlEntity urlEntity = UrlEntity.builder()
                .originalUrl(request.getUrl())
                .shortUrl(finalShort)
                .clicks(0L)
                .createdAt(LocalDateTime.now())
                .expiryAt(LocalDateTime.now().plusDays(7))
                .build();

        urlRepository.save(urlEntity);

        log.info("Created short URL: {} -> {} (expires in 7 days)", finalShort, request.getUrl());

        return new UrlResponseDTO(
                "http://local:8080/" + finalShort,
                request.getUrl(),
                0L,
                urlEntity.getExpiryAt()
        );
    }

    private void blockInternalUrls(String url) {
        if (url.contains("127.0.0.1") || url.contains("localhost") || url.contains("192.168.")) {
            throw new InvalidUrlException("Internal/Private urls not allowed.");
        }
    }

    private String sanitise(String url) {
        return url.replaceAll("[<>\"{}]", "");
    }

    public String generateRandomCode() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();

        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(characters.charAt(random.nextInt(characters.length())));
        }

        return code.toString();
    }

    private void validateUrl(String url) {
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid URL: " + url);
        }
    }

    public String getOriginalUrl(String  shortUrl) {
        UrlEntity urlEntity = urlRepository.findByShortUrl(shortUrl);

        if(urlEntity == null)
            return null;

        if(urlEntity.getExpiryAt().isBefore(LocalDateTime.now())) {
            log.warn("Url Expired: {}", shortUrl);
            return "Expired";
        }

        urlEntity.setClicks(urlEntity.getClicks() + 1);

        urlRepository.save(urlEntity);

        return urlEntity.getOriginalUrl();
    }

    public List<UrlEntity> getAll() {
        return urlRepository.findAll();
    }

    public List<UrlEntity> getTop10() {
        return urlRepository.findTop5ByOrderByClicksDesc();
    }

    public UrlAnalyticsDTO getAnalytics(String shortUrl) {
        UrlEntity urlEntity = urlRepository.findByShortUrl(shortUrl);

        if(urlEntity == null) {
            return null;
        }

        UrlAnalyticsDTO analytics = new UrlAnalyticsDTO();
        analytics.setOriginalUrl(urlEntity.getOriginalUrl());
        analytics.setShortUrl(urlEntity.getShortUrl());
        analytics.setClicks(urlEntity.getClicks());
        analytics.setCreatedAt(urlEntity.getCreatedAt());
        analytics.setExpiryAt(urlEntity.getExpiryAt());

        boolean isExpired = urlEntity.getExpiryAt().isBefore(LocalDateTime.now());
        analytics.setExpired(isExpired);

        return analytics;
    }

    public UrlAnalyticsDTO updateExpiry(String shortUrl, int days) {
        UrlEntity urlEntity = urlRepository.findByShortUrl(shortUrl);

        if(urlEntity == null) {
            return null;
        }

        urlEntity.setExpiryAt(urlEntity.getExpiryAt().plusDays(days));
        urlRepository.save(urlEntity);

        return getAnalytics(shortUrl);
    }

    public Boolean deleteShortUrl(String shortUrl) {
        UrlEntity urlEntity = urlRepository.findByShortUrl(shortUrl);

        if(urlEntity == null) {
            return false;
        }

        urlRepository.delete(urlEntity);
        return true;
    }
}
