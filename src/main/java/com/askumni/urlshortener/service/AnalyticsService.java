package com.askumni.urlshortener.service;

import com.askumni.urlshortener.dto.AnalyticsResponse;
import com.askumni.urlshortener.entity.UrlMapping;
import com.askumni.urlshortener.exception.UrlNotFoundException;
import com.askumni.urlshortener.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UrlMappingRepository repository;

    public AnalyticsResponse getAnalytics(String shortCode) {

        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        if (mapping.getExpiresAt() != null
                && mapping.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UrlNotFoundException(shortCode);
        }

        return new AnalyticsResponse(
                mapping.getShortCode(),
                mapping.getOriginalUrl(),
                mapping.getClickCount(),
                mapping.getCreatedAt(),
                mapping.getExpiresAt()
        );
    }
}