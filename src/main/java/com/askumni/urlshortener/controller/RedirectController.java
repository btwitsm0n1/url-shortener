package com.askumni.urlshortener.controller;

import com.askumni.urlshortener.service.UrlShortenerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final UrlShortenerService urlShortenerService;

    /**
     * GET /{shortCode} -> 302 redirect to the original URL.
     * This is the hottest path in the whole system (every click hits this),
     * which is why resolveOriginalUrl() is cached in Redis.
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String originalUrl = urlShortenerService.resolveOriginalUrl(shortCode);

        // fire-and-forget style click tracking; doesn't block the redirect
        urlShortenerService.incrementClickCount(shortCode);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, originalUrl);
        return new ResponseEntity<>(headers, HttpStatus.FOUND); // 302
    }
}
