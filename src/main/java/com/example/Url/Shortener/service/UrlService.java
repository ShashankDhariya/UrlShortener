package com.example.Url.Shortener.service;

import com.example.Url.Shortener.dto.UrlAnalyticsDTO;
import com.example.Url.Shortener.dto.UrlRequestDTO;
import com.example.Url.Shortener.dto.UrlResponseDTO;
import com.example.Url.Shortener.entity.UrlEntity;
import com.example.Url.Shortener.repository.UrlRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.config.FixedRateTask;
import org.springframework.stereotype.Service;
import org.springframework.web.util.InvalidUrlException;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Service
@Slf4j
public class UrlService {
    private final UrlRepository urlRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    UrlService(UrlRepository urlRepository, RedisTemplate<String, Object> redisTemplate) {
        this.urlRepository = urlRepository;
        this.redisTemplate = redisTemplate;
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
        redisTemplate.opsForValue().set("url" + finalShort, originalUrl, Duration.ofHours(12));
        redisTemplate.opsForValue().setIfAbsent("clicks" + finalShort, 0L);

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
        String urlKey = "url" + shortUrl;
        String clickKey = "clicks" + shortUrl;

        String cachedUrl =  (String) redisTemplate.opsForValue().get(urlKey);
        if(cachedUrl != null) {
            redisTemplate.opsForValue().increment(clickKey);
            return cachedUrl;
        }

        UrlEntity urlEntity = urlRepository.findByShortUrl(shortUrl);

        if(urlEntity == null)
            return null;

        if(urlEntity.getExpiryAt().isBefore(LocalDateTime.now())) {
            log.warn("Url Expired: {}", shortUrl);
            return "Expired";
        }

        redisTemplate.opsForValue().set(urlKey, urlEntity.getOriginalUrl(), Duration.ofHours(12));
        redisTemplate.opsForValue().increment(clickKey);

        return urlEntity.getOriginalUrl();
    }

    @Scheduled(fixedRate = 30000)
    private void syncClicksToDB() {
        log.info("Syncing clicks to DB");

        Set<String> keys = redisTemplate.keys("clicks*");
        if(keys.isEmpty()) {
            return;
        }

        for(String key : keys) {
            String shortUrl = key.replace("clicks", "");

            Long redisClicks = (Long) redisTemplate.opsForValue().get(key);
            if(redisClicks == null) {
                continue;
            }

            UrlEntity urlEntity = urlRepository.findByShortUrl(shortUrl);
            if(urlEntity == null) {
                continue;
            }

            urlEntity.setClicks(redisClicks);
            urlRepository.save(urlEntity);
        }

        log.info("Synced Successfully");
    }

    public List<UrlEntity> getAll() {
        return urlRepository.findAll();
    }

    public List<UrlEntity> getTop3() {
        return urlRepository.findTop3ByOrderByClicksDesc();
    }

    public List<UrlEntity> createdInLastXDays(int days) {
        return urlRepository.findByCreatedAtAfter(LocalDateTime.now().minusDays(days));
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
        redisTemplate.delete("url" + shortUrl);
        redisTemplate.delete("clicks" + shortUrl);
        return true;
    }

    public Long getClicks(String shortUrl) {
        UrlEntity urlEntity = urlRepository.findByShortUrl(shortUrl);

        if(urlEntity == null) {
            throw new RuntimeException("Url Not Found");
        }

        return urlEntity.getClicks();
    }

    public List<UrlEntity> getMostClicked() {
        return urlRepository.findAll()
                .stream()
                .sorted((a,b) -> Long.compare(b.getClicks(), a.getClicks()))
                .toList();
    }

    public Page<UrlEntity> getPaginated(int page, int size) {
        PageRequest request = PageRequest.of(page, size);
        return urlRepository.findAll(request);
    }
}
