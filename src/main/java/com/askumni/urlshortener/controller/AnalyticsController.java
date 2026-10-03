package com.askumni.urlshortener.controller;

import com.askumni.urlshortener.dto.AnalyticsResponse;
import com.askumni.urlshortener.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/{shortCode}")
    public ResponseEntity<AnalyticsResponse> getAnalytics(
            @PathVariable String shortCode) {

        AnalyticsResponse response =
                analyticsService.getAnalytics(shortCode);

        return ResponseEntity.ok(response);
    }
}