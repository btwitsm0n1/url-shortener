package com.askumni.urlshortener.controller;

import com.askumni.urlshortener.dto.AnalyticsResponse;
import com.askumni.urlshortener.dto.ShortenRequest;
import com.askumni.urlshortener.dto.ShortenResponse;
import com.askumni.urlshortener.service.UrlShortenerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;

    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shortenUrl(
            @Valid @RequestBody ShortenRequest request) {

        ShortenResponse response =
                urlShortenerService.shortenUrl(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/analytics/{shortCode}")
    public ResponseEntity<AnalyticsResponse> getAnalytics(
            @PathVariable String shortCode) {

        AnalyticsResponse response =
                urlShortenerService.getAnalytics(shortCode);

        return ResponseEntity.ok(response);
    }
}