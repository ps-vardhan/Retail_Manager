# Retail Manager

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.11-brightgreen)
![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0-teal)
![pgvector](https://img.shields.io/badge/pgvector-1536d-blue)
![Java 17](https://img.shields.io/badge/Java-17-orange)

A full-stack retail management application with Spring Security authentication, DummyJSON-sourced product data, and semantic search powered by OpenAI `text-embedding-ada-002` and `pgvector`.

## 📑 Table of Contents
- [Architecture Overview](#-architecture-overview)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Configuration](#configuration)
  - [Running the Application](#running-the-application)
- [Core Pillars](#-core-pillars)
- [Reference](#-reference)
  - [Data Model](#data-model)
  - [Endpoints](#endpoints)
- [Known Issues](#-known-issues)

---

## 🏗 Architecture Overview

The application is structured around three sequentially coupled pillars:

1. **Pillar 01: Auth Gate**
   - Spring Security filter chain restricts all routes.
   - Core file: `SecurityConfig.java`
2. **Pillar 02: Product Sync**
   - `WebClient` fetches product data from DummyJSON, persists Products via JPA.
   - Core file: `ProductSyncService.java`
3. **Pillar 03: Semantic Search**
   - OpenAI `ada-002` embeddings are stored in a pgvector `HNSW` index.
   - Core file: `AIService.java`

> **Note:** Pillars 2 and 3 are tightly coupled. `EmbeddingService.generateAndStore()` is invoked inside `ProductSyncService.syncAllProducts()` immediately after each `productRepository.save()`, ensuring every synced product gets a `float[1536]` embedding vector before the sync loop continues.

## 💻 Tech Stack

- **Framework:** Spring Boot 3.5.11
- **Security:** Spring Security 6
- **Frontend:** Thymeleaf + Bootstrap 5.3
- **Database:** PostgreSQL + pgvector
- **HTTP Client:** Spring WebFlux WebClient
- **AI / Embeddings:** Spring AI 1.0.0
- **Embedding Model:** OpenAI `ada-002` (1536 dims)
- **ORM:** Spring Data JPA / Hibernate

## 📂 Project Structure

```text
main/src/main/java/com/retail/retailmanager/
├── config/
│   └── SecurityConfig.java          # Filter chain, BCrypt, AuthManager
├── controller/
│   ├── AuthController.java          # GET/POST /login /signup
│   ├── ProductController.java       # GET /products /products/search /{id}
│   ├── OrderController.java         # POST /orders — inventory decrement
│   └── AdminController.java         # /admin/** — sync, add, delete
├── model/
│   ├── Product.java                 # float[] embedding → vector(1536)
│   ├── User.java                    # username, BCrypt password, role
│   ├── Order.java                   # ManyToOne user + product
│   └── Inventory.java               # OneToOne product, quantity
├── repository/
│   ├── ProductRepository.java
│   ├── UserRepository.java          # findByUsername()
│   ├── OrderRepository.java
│   └── InventoryRepository.java     # findByProductId()
└── service/
    ├── UserService.java             # Implements UserDetailsService
    ├── ProductSyncService.java      # WebClient → persist → embed
    ├── EmbeddingService.java        # OpenAI embed → float[] → DB
    └── AIService.java               # VectorStore.similaritySearch()

main/src/main/resources/
├── application.properties           # DB, OpenAI, pgvector, port
├── data.sql                         # Admin seed row (idempotent)
└── templates/
    ├── login.html, signup.html
    ├── products/ list.html, detail.html
    └── admin/ dashboard.html
```

## 🚀 Getting Started

### Prerequisites

1. **Java 17**: Ensure Java 17 is installed. Verify with `java -version`.
2. **PostgreSQL on port 5435**: A database named `retail_db` must exist. The app connects via JDBC at startup and Hibernate auto-creates the schema (`ddl-auto=update`).
3. **pgvector extension**: Required before Hibernate maps the `vector(1536)` column type in `Product.java`.
   ```sql
   CREATE EXTENSION IF NOT EXISTS vector;
   ```
4. **OpenAI API key**: Requires access to `text-embedding-ada-002`. Set this as the `OPENAI_API` environment variable.

### Configuration

Runtime configurations are maintained in `src/main/resources/application.properties`. 

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5435/retail_db
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update

# Server
server.port=8081

# Product sync source
product.api.base-url=https://dummyjson.com

# OpenAI
spring.ai.openai.api-key=${OPENAI_API}
spring.ai.openai.embedding.model=text-embedding-ada-002

# pgvector
spring.ai.vectorstore.pgvector.dimensions=1536
spring.ai.vectorstore.pgvector.distance-type=COSINE_DISTANCE
spring.ai.vectorstore.pgvector.index-type=HNSW
```

> ⚠️ **Configuration Note:** Check your `application.properties` and make sure environment variables follow Spring's standard syntax (e.g. `${DB_USERNAME}`) rather than `{$DB_USERNAME}` to prevent connection failures.

### Running the Application

Navigate to the project directory and run:
```bash
cd main
./mvnw spring-boot:run
```
The application will start on `http://localhost:8081`. 

**Default Admin Credentials:**
On first startup, `data.sql` idempotently seeds a default admin account using a `WHERE NOT EXISTS` guard.
- **Username:** `admin`
- **Password:** `password`
- **Role:** `ADMIN`

## 🏛 Core Pillars

### Pillar 1 — Authentication Gate
File: `src/main/java/com/retail/retailmanager/config/SecurityConfig.java`

Spring Security's filter chain is fully configured. The endpoints `/login` and `/signup` are public (`permitAll()`). Routes starting with `/admin/**` require `ROLE_ADMIN`, and all other routes like `/products` require standard authentication (`anyRequest().authenticated()`). Passwords are encrypted using `BCryptPasswordEncoder`, and signup persistence handles creation of `CUSTOMER` role users.

### Pillar 2 — Product Sync
Service: `ProductSyncService.java` | Trigger: `POST /admin/sync`

`ProductSyncService` uses a `WebClient` to fetch products limit=100 from the DummyJSON API. Each record is mapped to a `Product` entity and persisted via `productRepository.save()`. Immediately after each save, `EmbeddingService.generateAndStore(product)` generates the OpenAI embedding vector.

### Pillar 3 — Semantic Search
Service: `AIService.java` / `EmbeddingService.java` | Endpoint: `GET /products/search?q=`

1. **Embedding generation:** Powered by OpenAI's `text-embedding-ada-002`. Plain text (name, description, category) is mapped into a `float[]` (1536 dims) and written to the JPA column mapping `vector(1536)`.
2. **Similarity search:** `AIService.semanticSearch(query)` embeds the search term and executes a `vectorStore.similaritySearch()` against pgvector using the `HNSW` index (Cosine Distance). It fetches IDs from result metadata and grabs `Product` objects from JPA. An empty query elegantly bypasses the embeddings entirely and performs a `productRepository.findAll()`.

## 📚 Reference

### Data Model

| Table | Column | Type | Notes |
|---|---|---|---|
| **products** | `id` | `BIGINT` | Auto-generated PK |
| | `name` | `VARCHAR` | Not null |
| | `description` | `VARCHAR` | Nullable |
| | `price` | `NUMERIC` | Not null |
| | `category` | `VARCHAR` | Nullable |
| | `thumbnail_url` | `VARCHAR(1000)` | Nullable |
| | `embedding` | `vector(1536)` | Null until sync runs; requires pgvector |
| **users** | `id` | `BIGINT` | Auto-generated PK |
| | `username` | `VARCHAR` | Unique, not null |
| | `password` | `VARCHAR` | BCrypt-encoded, not null |
| | `role` | `VARCHAR` | `ADMIN` or `CUSTOMER` |
| **inventory**| `id` | `BIGINT` | Auto-generated PK |
| | `product_id` | `BIGINT` | FK → `products.id` (OneToOne) |
| | `quantity` | `INT` | Not null; decremented on order |
| **orders** | `id` | `BIGINT` | Auto-generated PK |
| | `user_id` | `BIGINT` | FK → `users.id` |
| | `product_id` | `BIGINT` | FK → `products.id` |
| | `quantity` | `INT` | Not null |
| | `created_at` | `TIMESTAMP` | Set via `@PrePersist` |

### Endpoints

**Public**
- <span style="background-color:rgba(34,197,94,0.12); color:#22c55e; padding:3px 7px; border-radius:3px; font-family:monospace; font-weight:600; font-size:11px;">GET</span> `/login` — Login page
- <span style="background-color:rgba(79,142,247,0.12); color:#4f8ef7; padding:3px 7px; border-radius:3px; font-family:monospace; font-weight:600; font-size:11px;">POST</span> `/login` — Handled by Spring Security
- <span style="background-color:rgba(34,197,94,0.12); color:#22c55e; padding:3px 7px; border-radius:3px; font-family:monospace; font-weight:600; font-size:11px;">GET</span> `/signup` — Sign-up page
- <span style="background-color:rgba(79,142,247,0.12); color:#4f8ef7; padding:3px 7px; border-radius:3px; font-family:monospace; font-weight:600; font-size:11px;">POST</span> `/signup` — Creates a CUSTOMER user

**Authenticated (Any Role)**
- <span style="background-color:rgba(34,197,94,0.12); color:#22c55e; padding:3px 7px; border-radius:3px; font-family:monospace; font-weight:600; font-size:11px;">GET</span> `/products` — Full product listing
- <span style="background-color:rgba(34,197,94,0.12); color:#22c55e; padding:3px 7px; border-radius:3px; font-family:monospace; font-weight:600; font-size:11px;">GET</span> `/products/search?q=` — Semantic search via pgvector
- <span style="background-color:rgba(34,197,94,0.12); color:#22c55e; padding:3px 7px; border-radius:3px; font-family:monospace; font-weight:600; font-size:11px;">GET</span> `/products/{id}` — Product detail page
- <span style="background-color:rgba(79,142,247,0.12); color:#4f8ef7; padding:3px 7px; border-radius:3px; font-family:monospace; font-weight:600; font-size:11px;">POST</span> `/orders` — Place order; decrements inventory
- <span style="background-color:rgba(79,142,247,0.12); color:#4f8ef7; padding:3px 7px; border-radius:3px; font-family:monospace; font-weight:600; font-size:11px;">POST</span> `/logout` — Invalidates session; redirects to `/login`

**Admin Only (`ROLE_ADMIN`)**
- <span style="background-color:rgba(34,197,94,0.12); color:#22c55e; padding:3px 7px; border-radius:3px; font-family:monospace; font-weight:600; font-size:11px;">GET</span> `/admin/dashboard` — Products, orders, sync button
- <span style="background-color:rgba(79,142,247,0.12); color:#4f8ef7; padding:3px 7px; border-radius:3px; font-family:monospace; font-weight:600; font-size:11px;">POST</span> `/admin/sync` — Full DummyJSON sync + embed
- <span style="background-color:rgba(79,142,247,0.12); color:#4f8ef7; padding:3px 7px; border-radius:3px; font-family:monospace; font-weight:600; font-size:11px;">POST</span> `/admin/products/add` — Manual product + inventory row
- <span style="background-color:rgba(79,142,247,0.12); color:#4f8ef7; padding:3px 7px; border-radius:3px; font-family:monospace; font-weight:600; font-size:11px;">POST</span> `/admin/products/delete/{id}` — Deletes product + inventory

## 🚨 Known Issues

1. **Compile Error — Missing import (Document type unresolved):** `src/main/java/com/retail/retailmanager/service/AIService.java` (line 35) references `Document` directly without importing `org.springframework.ai.document.Document`.
2. **Runtime Bug — Embeddings never written to VectorStore:** In `EmbeddingService.java` (`generateAndStore()`), the vector is mapped to JPA `product.embedding` but `vectorStore.add()` is skipped. The pgvector `vector_store` table remains empty, causing similarity searches to fail.
3. **Config Bug — Property placeholder syntax incorrect:** In `application.properties`, the environment variables are configured as `{$VAR}` instead of Spring's standard `${VAR}`, which breaks database connection and OpenAI token parsing.
4. **Data Gap — No inventory seed rows:** `data.sql` populates the admin user but lacks defaults for `inventory`. A fresh `/orders` call will throw a `RuntimeException("Inventory not found")`.
