# Student Management System

A secure and containerized Student Management System built using Spring Boot, Spring Data JPA, MySQL, JWT Authentication, Swagger/OpenAPI, and Docker.

## 📌 Project Overview

The Student Management System is a backend application designed to manage student information through RESTful APIs.

The application provides secure authentication using JWT and supports student data management through protected REST APIs.

## 🚀 Features

- JWT-based authentication
- Secure REST APIs
- Student management
- MySQL database integration
- Spring Data JPA and Hibernate
- Global exception handling
- Standardized API responses
- Swagger/OpenAPI API documentation
- Docker containerization
- Layered Spring Boot architecture

## 🛠️ Tech Stack

| Technology | Usage |
|---|---|
| Java 21 | Programming Language |
| Spring Boot 3.5.4 | Backend Framework |
| Spring Data JPA | Data Access |
| Hibernate | ORM |
| MySQL 8 | Database |
| JWT | Authentication |
| Swagger / OpenAPI | API Documentation |
| Maven | Build Tool |
| Docker | Containerization |
| Git & GitHub | Version Control |

## 🏗️ System Architecture

The application follows a layered architecture:

```text
Client
   │
   ▼
REST Controller
   │
   ▼
Service Layer
   │
   ▼
Repository Layer
   │
   ▼
MySQL Database
MySQL Database
