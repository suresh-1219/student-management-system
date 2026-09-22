# Student Management System

A secure and containerized Student Management System built using Spring Boot, Spring Data JPA, MySQL, JWT Authentication, Swagger/OpenAPI, and Docker.

## 📌 Project Overview

The Student Management System is a backend application designed to manage student information through RESTful APIs. The application provides secure, role-based authentication using JWT and supports full student data management through protected REST APIs.

## 🚀 Features

- JWT-based authentication with stateless session management
- Role-based access control (USER, ADMIN)
- Student management with full CRUD operations
- Search, sorting, and pagination support
- MySQL database integration
- Spring Data JPA and Hibernate
- Clean DTO layer decoupling API contracts from JPA entities
- Global exception handling with custom exceptions (401 / 404 / 400 / 409 responses)
- Standardized API responses
- Swagger/OpenAPI API documentation
- Docker containerization
- Layered Spring Boot architecture

## ✅ Testing

- 77 unit, controller, and integration tests written using JUnit 5 and Mockito
- 97% instruction coverage, verified via JaCoCo
- Covers the service layer, controllers, JWT filter/util, DTOs, entities, and exception handling

## 🛠️ Tech Stack

| Technology | Usage |
|---|---|
| Java 21 | Programming Language |
| Spring Boot 3.5.4 | Backend Framework |
| Spring Data JPA | Data Access |
| Hibernate | ORM |
| MySQL 8 | Database |
| Flyway | Versioned database migrations |
| JWT (jjwt) | Authentication |
| Spring Security | Authorization |
| ModelMapper | Entity-DTO conversion |
| Swagger / OpenAPI | API Documentation |
| JUnit 5 + Mockito | Testing |
| JaCoCo | Test coverage reporting |
| Maven | Build Tool |
| Docker | Containerization |
| Git & GitHub | Version Control |

## 🏗️ System Architecture

The application follows a layered architecture:

```text
Client
   │
   ▼
REST Controller (DTOs)
   │
   ▼
Service Layer (DTO ↔ Entity mapping)
   │
   ▼
Repository Layer
   │
   ▼
MySQL Database
```

### Architecture Diagram

![Student Management System Architecture](student-management-system/docs/student-management-systemdocsarchitecture.png)
## 🔐 Authentication

All endpoints except `/auth/login`, `/users/register`, and Swagger routes require a valid JWT.

1. Register a user via `POST /users/register` (body: `username`, `email`, `password`). Public registration **always creates a `USER`**; any `role` sent by the client is ignored.
2. Log in via `POST /auth/login` to receive a JWT
3. Pass the token on subsequent requests as a header:

```
Authorization: Bearer <token>
```

## 📚 API Endpoints

### Auth

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/auth/login` | Public | Authenticate and receive a JWT |

### Users

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/users/register` | Public | Register a new user (always role `USER`) |

### Students

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/students/search` | USER, ADMIN | **Search, filter, sort and page students in one call (see below)** |
| GET | `/students` | USER, ADMIN | Get all students *(deprecated, use `/students/search`)* |
| GET | `/students/{id}` | USER, ADMIN | Get a student by ID |
| GET | `/students/search/{name}` | USER, ADMIN | Search students by name *(deprecated)* |
| GET | `/students/page?page=&size=` | USER, ADMIN | Get students with pagination *(deprecated)* |
| GET | `/students/sort/{field}` | USER, ADMIN | Get students sorted ascending by field *(deprecated)* |
| GET | `/students/sortDesc/{field}` | USER, ADMIN | Get students sorted descending by field *(deprecated)* |
| GET | `/students/pageSort?page=&size=&field=` | USER, ADMIN | Get students with pagination + sorting *(deprecated)* |
| POST | `/students` | ADMIN | Create a student |
| PUT | `/students/{id}` | ADMIN | Update a student |
| DELETE | `/students/{id}` | ADMIN | Delete a student |

#### Searching: `GET /students/search`

All parameters are optional and can be combined:

| Parameter | Meaning | Default |
|---|---|---|
| `name` | Partial, case-insensitive match on the name | (no filter) |
| `course` | Exact, case-insensitive match on the course | (no filter) |
| `minFee`, `maxFee` | Inclusive fee range | (no filter) |
| `sort` | `id`, `name`, `email`, `course` or `fee` | `id` |
| `direction` | `asc` or `desc` | `asc` |
| `page`, `size` | Page number (from 0) and page size (1-100) | `0`, `10` |

Example: `GET /students/search?name=sur&course=MCA&minFee=1000&sort=fee&direction=desc&size=5`

The result is wrapped in the usual `data` field and contains `content`, `page`, `size`, `totalElements`, `totalPages`, `first` and `last`. `minFee` greater than `maxFee` returns `400`.

**Fees** are exact decimals (never floating point): at most 10 digits before and 2 after the decimal point, and never negative.

**Limits:** `size` must be between 1 and 100 and `page` must not be negative. Sorting is allowed only by `id`, `name`, `email`, `course` or `fee`; any other field returns `400`.

**Unique e-mail:** a student's e-mail must be unique. Creating a student, or changing an e-mail, to one that another student already uses returns `409 Conflict`.

### Error responses

Every error (including 401 and 403) uses the same JSON shape:

```json
{
  "timestamp": "2026-09-21T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": { "fee": "Fee cannot be negative" }
}
```

`errors` is present only for validation failures. Unexpected server errors return a generic `500` message; details are logged on the server and never sent to the client.

Full interactive API documentation is available via Swagger once the app is running, at `/swagger-ui.html`.

## ⚙️ Getting Started

### Prerequisites

- Java 21
- Maven
- MySQL 8 (or Docker, to run it in a container)

### Environment Variables

The application reads the following from the environment:

| Variable | Description |
|---|---|
| `DB_URL` | MySQL JDBC URL (defaults to `jdbc:mysql://localhost:3307/student_db`) |
| `DB_USERNAME` | MySQL username (defaults to `root`) |
| `DB_PASSWORD` | MySQL password |
| `JWT_SECRET` | Secret key used to sign JWTs (at least 32 characters) |
| `ADMIN_PASSWORD` | *Optional.* If set, an `ADMIN` account is created on startup (skipped if it already exists) |
| `ADMIN_USERNAME` | Admin username (defaults to `admin`) |
| `ADMIN_EMAIL` | Admin email (defaults to `admin@example.com`) |

### Creating the first admin

Admins cannot be created through the public API. Set `ADMIN_PASSWORD` (and optionally `ADMIN_USERNAME` / `ADMIN_EMAIL`) before starting the app and it will seed the account on startup. Then log in via `/auth/login` to get an admin token.

### Database migrations (Flyway)

The database schema is managed by [Flyway](https://flywaydb.org/), not by Hibernate (`spring.jpa.hibernate.ddl-auto=none`). Migration scripts live in `src/main/resources/db/migration` and run automatically on startup:

| Script | What it does |
|---|---|
| `V1__baseline_schema.sql` | Creates `users` and `students` on a brand-new database |
| `V2__student_email_unique_and_audit.sql` | Unique student e-mail, `created_at` / `updated_at` timestamps, `version` column for optimistic locking |
| `V3__student_fee_decimal.sql` | Fee stored as `DECIMAL(12,2)` instead of `DOUBLE` |

- **Existing database** (created before Flyway was added): Flyway records it as already being at version 1 and applies only V2 onwards.
- **Never edit a migration that has already been applied.** Add a new file instead (`V3__...sql`, `V4__...sql`, ...).
- If V2 fails with `Duplicate entry ... for key 'uk_students_email'`, two students share an e-mail. Remove or change the duplicate, then start the app again; the failed `ALTER TABLE` changes nothing.

### Run locally

```bash
git clone https://github.com/suresh-1219/student-management-system.git
cd student-management-system

export DB_PASSWORD=your_db_password
export JWT_SECRET=your_jwt_secret

mvn spring-boot:run
```

The application runs on `http://localhost:9090`.

### Run with Docker

```bash
mvn clean package -DskipTests
docker build -t student-management-system .
docker run -p 9090:9090 \
  -e DB_URL=jdbc:mysql://host.docker.internal:3307/student_db \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=your_db_password \
  -e JWT_SECRET=your_jwt_secret \
  student-management-system
```

### Run tests + coverage report

```bash
mvn test
```

The JaCoCo coverage report is generated at `target/site/jacoco/index.html`.

## 📄 License

This project was built as a personal portfolio project.
