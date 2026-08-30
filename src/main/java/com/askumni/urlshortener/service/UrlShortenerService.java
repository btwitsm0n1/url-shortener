package com.askumni.urlshortener.service;

import com.askumni.urlshortener.dto.ShortenRequest;
import com.askumni.urlshortener.dto.ShortenResponse;
import com.askumni.urlshortener.entity.UrlMapping;
import com.askumni.urlshortener.exception.AliasAlreadyExistsException;
import com.askumni.urlshortener.exception.UrlNotFoundException;
import com.askumni.urlshortener.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UrlShortenerService {

    private final UrlMappingRepository repository;
    private final Base62Encoder base62Encoder;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.default-expiry-days}")
    private int defaultExpiryDays;

    /**
     * Creates a short URL.
     * Flow:
     *  1. If user gave a custom alias -> validate it's free, use it directly.
     *  2. Otherwise -> save row first (id auto-generated), then Base62-encode
     *     the id to get the short code, then update the row with that code.
     *     (Two-step because we need the DB-generated ID before we can encode it.)
     */
    public ShortenResponse shortenUrl(ShortenRequest request) {
        int expiryDays = request.getExpiryDays() != null ? request.getExpiryDays() : defaultExpiryDays;
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(expiryDays);

        UrlMapping mapping;

        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            if (repository.existsByShortCode(request.getCustomAlias())) {
                throw new AliasAlreadyExistsException(request.getCustomAlias());
            }
            mapping = new UrlMapping();
            mapping.setOriginalUrl(request.getOriginalUrl());
            mapping.setShortCode(request.getCustomAlias());
            mapping.setCustomAlias(true);
            mapping.setExpiresAt(expiresAt);
            mapping.setClickCount(0L);
            repository.save(mapping);
        } else {
            mapping = new UrlMapping();
            mapping.setOriginalUrl(request.getOriginalUrl());
            mapping.setShortCode("PENDING"); // placeholder, unique temp value not required since we update right after
            mapping.setExpiresAt(expiresAt);
            mapping.setClickCount(0L);
            mapping = repository.save(mapping); // id gets generated here

            String shortCode = base62Encoder.encode(mapping.getId());
            mapping.setShortCode(shortCode);
            repository.save(mapping);
        }

        return new ShortenResponse(baseUrl + mapping.getShortCode(), mapping.getOriginalUrl(), mapping.getExpiresAt());
    }

    /**
     * Resolves a short code to its original URL.
     * Cached in Redis under key "shortUrl::<code>" so repeat hits on popular
     * links skip the database entirely — this is the #1 optimization
     * interviewers expect you to mention for a high-read system like this.
     */
    @Cacheable(value = "shortUrl", key = "#shortCode")
    public String resolveOriginalUrl(String shortCode) {
        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        if (mapping.getExpiresAt() != null && mapping.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UrlNotFoundException(shortCode); // treat expired as not found
        }

        return mapping.getOriginalUrl();
    }

    /**
     * Click count is updated separately (not inside the cached read path)
     * so that cache hits don't need to hit the DB just to increment a counter.
     * In a high-traffic system you'd batch/async this (e.g. via a message queue)
     * instead of writing on every single click.
     */
    public void incrementClickCount(String shortCode) {
        repository.findByShortCode(shortCode).ifPresent(mapping -> {
            mapping.setClickCount(mapping.getClickCount() + 1);
            repository.save(mapping);
        });
    }

    @CacheEvict(value = "shortUrl", key = "#shortCode")
    public void evictCache(String shortCode) {
        // call this if a mapping is deleted/updated, to keep cache consistent
    }
}
