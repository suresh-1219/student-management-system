## System Architecture

The Student Management System follows a layered architecture using Spring Boot.

The application is divided into multiple layers to maintain separation of concerns, improve maintainability, and simplify testing.

### Architecture Layers

1. **Client Layer**
   - Users interact with the application through Swagger UI or a REST API client.

2. **Controller Layer**
   - Handles HTTP requests and responses.
   - Exposes RESTful endpoints for student and authentication operations.

3. **Security Layer**
   - Uses Spring Security and JWT authentication.
   - Validates JWT tokens before allowing access to protected endpoints.
   - Implements role-based authorization for USER and ADMIN roles.

4. **Service Layer**
   - Contains the application's business logic.
   - Processes student operations and user authentication.
   - Communicates with the repository layer.

5. **Repository Layer**
   - Uses Spring Data JPA for database operations.
   - Performs CRUD operations on student and user data.

6. **Database Layer**
   - MySQL is used for persistent data storage.
   - Stores student and user information.

### Architecture Diagram

![Student Management System Architecture](docs/architecture.png)
