<img width="1200" height="630" alt="url-shortener-banner" src="https://github.com/user-attachments/assets/8077561a-2fb9-44ac-831a-9e36e9d278b2" />




A Spring Boot based URL shortening service with Base62 encoding, Redis caching,
and a custom Token Bucket rate limiter — built to demonstrate core system
design concepts (hashing/encoding, caching, rate limiting) in a small,
interview-friendly project.

## Features

- Shorten any long URL into a compact code (`POST /api/shorten`)
- Redirect short codes to original URLs (`GET /{shortCode}`)
- Optional custom alias (e.g. `/my-resume`)
- Optional expiry date for links
- Click count tracking
- Redis caching for fast redirects on popular links
- Custom Token Bucket rate limiter (no external library) to prevent abuse

## Tech Stack

- Java 17, Spring Boot 3.3
- MySQL (persistence)
- Redis (caching)
- Maven

## Getting Started

### Prerequisites
- Java 17+
- Maven
- MySQL running locally
- Redis running locally (`redis-server`)

### Setup

1. Create a MySQL database (or let the app auto-create it):
   ```sql
   CREATE DATABASE urlshortener_db;
   ```

2. Update `src/main/resources/application.properties` with your MySQL
   username/password.

3. Run Redis (default port 6379):
   ```bash
   redis-server
   ```

4. Build and run:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

5. App runs at `http://localhost:8080`

## API Usage

### Shorten a URL
```bash
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://example.com/some/very/long/path"}'
```

Response:
```json
{
  "shortUrl": "http://localhost:8080/1B",
  "originalUrl": "https://example.com/some/very/long/path",
  "expiresAt": "2026-09-28T10:00:00"
}
```

### With custom alias and expiry
```bash
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://example.com", "customAlias": "my-link", "expiryDays": 7}'
```

### Use the short URL
Just open `http://localhost:8080/1B` in a browser — it 302-redirects to the
original URL.

## Architecture Notes (for interview discussion)

- **Short code generation**: DB auto-increment ID -> Base62 encoded. O(1),
  no collision checks needed. Trade-off: sequential IDs are predictable —
  the alternative (random hash + collision check) trades that away for
  extra DB round trips.
- **Caching**: `resolveOriginalUrl()` is `@Cacheable` in Redis, so repeat
  redirects for popular links skip the database.
- **Rate limiting**: Custom Token Bucket implementation (see
  `TokenBucketRateLimiter.java`) applied via a `HandlerInterceptor` to every
  request. Currently in-memory per app instance — in a real distributed
  deployment this would move to Redis so all instances share bucket state.
- **Click tracking**: Updated outside the cached read path so a cache hit
  doesn't force a DB write on every click.

## Possible Extensions

- QR code generation for short URLs
- Analytics dashboard (clicks by day/browser)
- Move rate limiter state to Redis for multi-instance deployments
- React frontend for a shareable UI
