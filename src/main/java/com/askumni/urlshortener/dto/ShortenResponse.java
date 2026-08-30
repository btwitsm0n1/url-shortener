package com.askumni.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShortenResponse {
    private String shortUrl;
    private String originalUrl;
    private LocalDateTime expiresAt;
}
