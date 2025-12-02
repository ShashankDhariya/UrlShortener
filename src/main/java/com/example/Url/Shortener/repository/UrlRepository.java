package com.example.Url.Shortener.repository;

import com.example.Url.Shortener.entity.UrlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<UrlEntity, Long> {
    @Query("Select u from UrlEntity u where u.shortUrl = :shortUrl")
    UrlEntity findByShortUrl(@Param("shortUrl") String shortUrl);

    boolean existsByShortUrl(String shortUrl);

    void deleteByExpiryAtBefore(LocalDateTime time);

    List<UrlEntity> findTop5ByOrderByClicksDesc();
}
