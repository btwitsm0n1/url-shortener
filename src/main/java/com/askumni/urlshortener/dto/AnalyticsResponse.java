package com.askumni.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnalyticsResponse {

    private String shortCode;
    private String originalUrl;
    private Long clickCount;
}