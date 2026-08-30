package com.askumni.urlshortener.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Core entity: maps a short code to the original long URL.
 * This is the table that both the "shorten" and "redirect" flows depend on.
 */
@Entity
@Table(name = "url_mapping", indexes = {
        // This index is what makes GET /{shortCode} fast even at scale
        @Index(name = "idx_short_code", columnList = "shortCode", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2048)
    private String originalUrl;

    @Column(nullable = false, unique = true, length = 20)
    private String shortCode;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Long clickCount = 0L;

    // true if the user picked their own alias instead of an auto-generated code
    @Column(nullable = false)
    private boolean customAlias = false;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.clickCount == null) {
            this.clickCount = 0L;
        }
    }
}
