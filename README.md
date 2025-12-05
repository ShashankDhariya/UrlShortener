# 🔗 URL Shortener

A production-grade **URL Shortening Service** built with **Spring Boot**, **Redis**, and **MySQL**, supporting custom aliases, analytics, expiry updates, and automatic cleanup of expired URLs.

---

## 🚀 Features

### 🔗 URL Shortening

* Generate random short URLs
* Support for **custom aliases**
* Store URL metadata in MySQL
* Set optional expiration time

### 📊 Analytics

* Track click counts using Redis
* Retrieve analytics via API
* Fast in-memory counters with Redis key pattern:

  ```
  clicks:<shortUrl>
  ```

### ♻️ Background Cleanup

* Scheduled cleanup of expired URLs
* Redis + MySQL consistency maintained

### 🔐 Security & Stability

* Rate limiting using custom **RateLimitFilter**
* Centralized error responses via **GlobalExceptionHandler**

### 🏥 Health Monitoring

* `/health` endpoint via **HealthController**

---

## 📁 Project Structure

```
com.example.Url.Shortener
│
├── config
│    └── RedisConfig              # Redis connection + template
│
├── controller
│    ├── HealthController         # Basic health-check endpoint
│    └── UrlController            # Core URL shorten / redirect / analytics APIs
│
├── dto
│    ├── UpdateExpiryDTO
│    ├── UrlAnalyticsDTO
│    ├── UrlRequestDTO
│    └── UrlResponseDTO
│
├── entity
│    └── UrlEntity                # JPA entity for storing URLs
│
├── exception
│    └── GlobalExceptionHandler   # Handles all API exceptions
│
├── repository
│    └── UrlRepository            # MySQL persistence
│
├── security
│    └── RateLimitFilter          # IP-based request throttling
│
├── service
│    ├── UrlCleanupService        # Scheduled cleanup for expired URLs
│    └── UrlService               # Main business logic
│
└── util
     └── (any helper utilities)
```

---

## 🔧 How It Works

### 1️⃣ Create Short URL

* Accepts long URL + optional custom short code
* Checks if custom alias already exists
* Saves metadata in MySQL
* Initializes Redis click counter

### 2️⃣ Redirect Logic

* Short URL is resolved from DB
* Counter incremented in Redis
* 302 redirect issued

### 3️⃣ Analytics

* Read clicks from Redis
* Read URL metadata from MySQL
* Return combined insights

### 4️⃣ Expiry & Cleanup

* A scheduled job checks expired URLs
* Deletes expired entries from DB

---

## ▶️ API Overview

### URL APIs

| Method | Endpoint                            | Description               |
| ------ | ----------------------------------- | --------------------------|
| POST   | `/api/shorten`                      | Create short URL          |
| GET    | `/api/{shortUrl}`                   | Redirect to original URL  |
| GET    | `/api/analytics/{shortUrl}`         | Get analytics             |
| GET    | `/api/analytics/all`                | All url analytics         |
| GET    | `/api/analytics/clicks`             | Click analytics for a Url |
| GET    | `/api/analytics/most-clicked`       | Top clicked Urls          |
| PATCH  | `/api/update-expiry/{shortUrl}`     | Update expiry             |
| DELETE | `/api/delete/{shortUrl}`            | Delete url                |

### Utility

| Method | Endpoint  | Description          |
| ------ | --------- | -------------------- |
| GET    | `/health` | Check service health |

---

## 🛠️ Running the Project

### Prerequisites

* JDK 17+
* MySQL
* Redis
* Maven

### Start Application

```bash
mvn clean install
mvn spring-boot:run
```

---

## 🧑‍💻 Author

**Shashank Dhariya**

---

## ⭐ Contributing

Pull requests and suggestions are welcome.

---

## 📜 License

MIT License
