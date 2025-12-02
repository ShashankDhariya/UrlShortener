package com.example.Url.Shortener.controller;

import com.example.Url.Shortener.dto.UpdateExpiryDTO;
import com.example.Url.Shortener.dto.UrlAnalyticsDTO;
import com.example.Url.Shortener.dto.UrlRequestDTO;
import com.example.Url.Shortener.dto.UrlResponseDTO;
import com.example.Url.Shortener.entity.UrlEntity;
import com.example.Url.Shortener.service.UrlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class UrlController {
    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @PostMapping("/shorten")
    public ResponseEntity<UrlResponseDTO> shorten(@RequestBody UrlRequestDTO requestDTO) {
        UrlResponseDTO response = urlService.createShortUrl(requestDTO);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{shortUrl}")
    public ResponseEntity<?> redirectToOriginal(@PathVariable String shortUrl) {
        String originalUrl = urlService.getOriginalUrl(shortUrl);

        if (originalUrl == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Url Not Found");
        }

        if(originalUrl.equals("Expired")) {
            return ResponseEntity.status(HttpStatus.GONE).body("Url Expired");
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .build();
    }

    @GetMapping("/analytics/all")
    public ResponseEntity<?> getAllUrls() {
        List<UrlEntity> urlList = urlService.getAll();
        return ResponseEntity.ok(urlList);
    }

    @GetMapping("/analytics/top")
    public ResponseEntity<?> getTopUrls() {
        List<UrlEntity> topUrl = urlService.getTop10();
        return ResponseEntity.ok(topUrl);
    }

    @GetMapping("/analytics/{shortUrl}")
    public ResponseEntity<?> getAnalytics(@PathVariable String shortUrl) {
        UrlAnalyticsDTO analyticsDTO = urlService.getAnalytics(shortUrl);

        if(analyticsDTO == null) {
            return null;
        }

        return ResponseEntity.ok(analyticsDTO);
    }

    @PatchMapping("/update-expiry/{shortUrl}")
    public ResponseEntity<?> updateExpiry(@PathVariable String shortUrl, @RequestBody UpdateExpiryDTO requestDTO) {
        UrlAnalyticsDTO urlAnalyticsDTO = urlService.updateExpiry(shortUrl, requestDTO.getDays());

        if(urlAnalyticsDTO == null) {
            return null;
        }

        return ResponseEntity.ok(urlAnalyticsDTO);
    }

    @DeleteMapping("/delete/{shortUrl}")
    public ResponseEntity<?> deleteUrl(@PathVariable String shortUrl) {
        Boolean deleted = urlService.deleteShortUrl(shortUrl);

        if(!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("URL Not Found");
        }

        return ResponseEntity.ok("URL deleted successfully");
    }
}
