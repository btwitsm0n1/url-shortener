
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

    public ShortenResponse shortenUrl(ShortenRequest request) {

        int expiryDays = request.getExpiryDays() != null
                ? request.getExpiryDays()
                : defaultExpiryDays;

        LocalDateTime expiresAt =
                LocalDateTime.now().plusDays(expiryDays);

        UrlMapping mapping;

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

            mapping = new UrlMapping();
            mapping.setOriginalUrl(request.getOriginalUrl());
            mapping.setShortCode("PENDING");
            mapping.setExpiresAt(expiresAt);
            mapping.setClickCount(0L);

            mapping = repository.save(mapping);

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

    @Cacheable(value = "shortUrl", key = "#shortCode")
    public String resolveOriginalUrl(String shortCode) {

        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() ->
                        new UrlNotFoundException(shortCode));

        if (mapping.getExpiresAt() != null
                && mapping.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new UrlNotFoundException(shortCode);
        }

        return mapping.getOriginalUrl();
    }

    public void incrementClickCount(String shortCode) {

        repository.findByShortCode(shortCode)
                .ifPresent(mapping -> {

                    mapping.setClickCount(
                            mapping.getClickCount() + 1
                    );

                    repository.save(mapping);
                });
    }

    @CacheEvict(value = "shortUrl", key = "#shortCode")
    public void evictCache(String shortCode) {
        // Removes the cached URL for this short code.
    }

    public void deleteUrl(String shortCode) {

        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() ->
                        new UrlNotFoundException(shortCode));

        repository.delete(mapping);

        evictCache(shortCode);
    }
}