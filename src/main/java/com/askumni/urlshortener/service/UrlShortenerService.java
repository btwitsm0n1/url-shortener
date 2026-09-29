package com.askumni.urlshortener.service;

import com.askumni.urlshortener.dto.AnalyticsResponse;
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
     * 1. If user gives a custom alias, check whether it already exists.
     * 2. If no custom alias is given, save the mapping first to get the DB ID.
     * 3. Convert the generated ID into a Base62 short code.
     * 4. Save the final short code.
     */
    public ShortenResponse shortenUrl(ShortenRequest request) {

        int expiryDays = request.getExpiryDays() != null
                ? request.getExpiryDays()
                : defaultExpiryDays;

        LocalDateTime expiresAt =
                LocalDateTime.now().plusDays(expiryDays);

        UrlMapping mapping;

        // Custom alias
        if (request.getCustomAlias() != null
                && !request.getCustomAlias().isBlank()) {

            if (repository.existsByShortCode(request.getCustomAlias())) {
                throw new AliasAlreadyExistsException(
                        request.getCustomAlias()
                );
            }

            mapping = new UrlMapping();

            mapping.setOriginalUrl(request.getOriginalUrl());
            mapping.setShortCode(request.getCustomAlias());
            mapping.setCustomAlias(true);
            mapping.setExpiresAt(expiresAt);
            mapping.setClickCount(0L);

            repository.save(mapping);

        } else {

            // Auto-generated short code
            mapping = new UrlMapping();

            mapping.setOriginalUrl(request.getOriginalUrl());
            mapping.setShortCode("PENDING");
            mapping.setExpiresAt(expiresAt);
            mapping.setClickCount(0L);

            // Save first so that database generates the ID
            mapping = repository.save(mapping);

            // Convert ID to Base62
            String shortCode =
                    base62Encoder.encode(mapping.getId());

            mapping.setShortCode(shortCode);

            repository.save(mapping);
        }

        return new ShortenResponse(
                baseUrl + mapping.getShortCode(),
                mapping.getOriginalUrl(),
                mapping.getExpiresAt()
        );
    }

    /**
     * Resolves a short code to the original URL.
     *
     * Redis caches the result so repeated requests
     * can avoid hitting the database.
     */
    @Cacheable(value = "shortUrl", key = "#shortCode")
    public String resolveOriginalUrl(String shortCode) {

        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() ->
                        new UrlNotFoundException(shortCode)
                );

        // Check expiry
        if (mapping.getExpiresAt() != null
                && mapping.getExpiresAt().isBefore(LocalDateTime.now())) {

            throw new UrlNotFoundException(shortCode);
        }

        return mapping.getOriginalUrl();
    }

    /**
     * Increments the click count for a short URL.
     */
    public void incrementClickCount(String shortCode) {

        repository.findByShortCode(shortCode)
                .ifPresent(mapping -> {

                    mapping.setClickCount(
                            mapping.getClickCount() + 1
                    );

                    repository.save(mapping);
                });
    }

    /**
     * Returns analytics information for a short URL.
     */
    public AnalyticsResponse getAnalytics(String shortCode) {

        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() ->
                        new UrlNotFoundException(shortCode)
                );

        return new AnalyticsResponse(
                mapping.getShortCode(),
                mapping.getOriginalUrl(),
                mapping.getClickCount()
        );
    }

    /**
     * Removes a short URL from Redis cache.
     * Useful when a mapping is updated or deleted.
     */
    @CacheEvict(value = "shortUrl", key = "#shortCode")
    public void evictCache(String shortCode) {

        // Cache eviction happens automatically.
    }
}