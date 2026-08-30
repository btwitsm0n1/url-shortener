package com.askumni.urlshortener.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Spring's cache abstraction (@Cacheable, @CacheEvict) backed by Redis.
 * Spring Boot auto-configures a RedisCacheManager as long as
 * spring-boot-starter-data-redis is on the classpath and Redis connection
 * properties are set in application.properties.
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {
}
