package com.askumni.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ShortenRequest {

    @NotBlank(message = "originalUrl must not be empty")
    @Pattern(
            regexp = "^(https?://)[\\w.-]+(\\.[\\w.-]+)+[/#?]?.*$",
            message = "originalUrl must be a valid http/https URL"
    )
    private String originalUrl;

    // Optional: user can request a custom alias like "my-resume"
    private String customAlias;

    // Optional: how many days until this link expires
    private Integer expiryDays;
}
