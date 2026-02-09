# Retail Management System

A full-stack Retail Management System built with **Java Spring Boot**, **PostgreSQL**, and **Thymeleaf**.

## 🚀 Features
-   **Product Management**: Create, Read, Update, Delete (CRUD) products.
-   **Inventory Tracking**: Real-time stock levels.
-   **Order Processing**: ACID-compliant transactions (Stock reduces automatically when an order is placed).
-   **Authentication**: Role-based access control (Admin vs. Customer) using Spring Security.

## 🛠️ Tech Stack
-   **Backend**: Java 17, Spring Boot 3.5.0
-   **Database**: PostgreSQL
-   **Frontend**: Thymeleaf, Bootstrap 5
-   **Build Tool**: Maven

---

## 🐛 Development Journal: Errors & Resolutions

During the construction of this project, we encountered and solved several critical issues. Here is a log of those challenges to help future developers.

### 1. The "Lombok" Compilation Failure
**Error**: `cannot find symbol: method getId()`
**Cause**: The Lombok library (`@Data`) was not processing annotations during the Maven build, meaning Getters and Setters were missing at compile time.
**Resolution**: 
-   Removed the `lombok` dependency.
-   Manually generated **Getters and Setters** for all Entity classes (`Product`, `Inventory`, `Order`, `User`).

### 2. Missing "Main" Class
**Error**: The application built successfully but exited immediately with "Hello World".
**Cause**: The project was missing the `@SpringBootApplication` entry point.
**Resolution**: 
-   Created `RetailManagerApplication.java` with `SpringApplication.run(...)`.

### 3. Database Connection Refusal
**Error**: `org.postgresql.util.PSQLException: FATAL: database "retail_db" does not exist`
**Cause**: The Spring Boot application was trying to connect to a database that hadn't been created in PostgreSQL yet.
**Resolution**: 
-   Manually created the database `retail_db` using pgAdmin.

### 4. Authentication Role Mismatch
**Error**: Customers could not access the "Place Order" page (403 Forbidden).
**Cause**: 
-   In `SecurityConfig`, the user was assigned reference `.roles("Customer")` (Lowercase).
-   The rule required `.hasAnyRole("CUSTOMER")` (Uppercase).
-   Spring Security is case-sensitive and expects roles to be capitalized.
**Resolution**: Changed the user definition to `.roles("CUSTOMER")`.

### 5. Missing Login Page
**Error**: Circular redirection or 404 on `/login`.
**Cause**: We defined a custom login page in Security Config but didn't create the Controller to serve it.
**Resolution**: Created `LoginController.java` to map the `/login` URL to the `login.html` view.
