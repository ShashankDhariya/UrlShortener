package com.example.Url.Shortener.service;

import com.example.Url.Shortener.repository.UrlRepository;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@EnableScheduling
public class UrlCleanupService {
    UrlRepository urlRepository;
    public UrlCleanupService(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void deleteExpiredUrls() {
        urlRepository.deleteByExpiryAtBefore(LocalDateTime.now() );
    }
}
