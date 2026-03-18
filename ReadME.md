# AI-Powered Retail Management Platform

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.11-brightgreen)
![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0-teal)
![pgvector](https://img.shields.io/badge/pgvector-1536d-blue)
![Java 17](https://img.shields.io/badge/Java-17-orange)

A full-stack retail management framework with Spring Security authentication, automated external product synchronization, and semantic search powered by OpenAI `text-embedding-ada-002` and `pgvector`.

## Architecture Overview

Retail Manager natively integrates strict secure user management, resilient data synchronization, and state-of-the-art AI features into a cohesive backend sequence.

- **Security & Access Control (`SecurityConfig.java`):** The application relies on a robust Spring Security filter chain for authentication and role-based access control. Sensitive passwords are encrypted using `BCryptPasswordEncoder`. Public routes handle user registration, standard routes require authenticated customer sessions, and management operations are strictly guarded behind admin-only privileges.
- **Live Product Synchronization (`ProductSyncService.java`):** Administrators can orchestrate automated product ingestion. Driven by Spring WebFlux's reactive `WebClient`, catalog data is securely fetched from the DummyJSON API and intelligently mapped to database entities using Spring Data JPA.
- **AI-Powered Semantic Search (`AIService.java`):** Deeply integrated directly into the product sync workflow, the system uses Spring AI calling OpenAI's `text-embedding-ada-002` model to transparently generate and attach 1536-dimensional vector embeddings for each new product. These high-dimensional vectors are stored and indexed within PostgreSQL using the `pgvector` extension taking advantage of an `HNSW` index. When users perform a search, their literal queries are embedded in real-time and rapidly evaluated against the catalog using Cosine Distance, surfacing mathematically relevant semantic matches that exceed traditional keyword capabilities.

## Tech Stack

- **Framework:** Spring Boot 3.5.11
- **Security:** Spring Security 6
- **Frontend:** Thymeleaf + Bootstrap 5.3
- **Database:** PostgreSQL + pgvector
- **HTTP Client:** Spring WebFlux WebClient
- **AI / Embeddings:** Spring AI 1.0.0
- **Embedding Model:** OpenAI `ada-002` (1536 dims)
- **ORM:** Spring Data JPA / Hibernate

## Getting Started

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

> **Configuration Note:** Check your `application.properties` and make sure environment variables follow Spring's standard syntax (e.g. `${DB_USERNAME}`) rather than `{$DB_USERNAME}` to prevent connection failures.

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

## Reference

### Data Model

| Table         | Column          | Type            | Notes                                   |
| ------------- | --------------- | --------------- | --------------------------------------- |
| **products**  | `id`            | `BIGINT`        | Auto-generated PK                       |
|               | `name`          | `VARCHAR`       | Not null                                |
|               | `description`   | `VARCHAR`       | Nullable                                |
|               | `price`         | `NUMERIC`       | Not null                                |
|               | `category`      | `VARCHAR`       | Nullable                                |
|               | `thumbnail_url` | `VARCHAR(1000)` | Nullable                                |
|               | `embedding`     | `vector(1536)`  | Null until sync runs; requires pgvector |
| **users**     | `id`            | `BIGINT`        | Auto-generated PK                       |
|               | `username`      | `VARCHAR`       | Unique, not null                        |
|               | `password`      | `VARCHAR`       | BCrypt-encoded, not null                |
|               | `role`          | `VARCHAR`       | `ADMIN` or `CUSTOMER`                   |
| **inventory** | `id`            | `BIGINT`        | Auto-generated PK                       |
|               | `product_id`    | `BIGINT`        | FK → `products.id` (OneToOne)           |
|               | `quantity`      | `INT`           | Not null; decremented on order          |
| **orders**    | `id`            | `BIGINT`        | Auto-generated PK                       |
|               | `user_id`       | `BIGINT`        | FK → `users.id`                         |
|               | `product_id`    | `BIGINT`        | FK → `products.id`                      |
|               | `quantity`      | `INT`           | Not null                                |
|               | `created_at`    | `TIMESTAMP`     | Set via `@PrePersist`                   |

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
