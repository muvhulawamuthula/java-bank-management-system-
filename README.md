# Bank Africa Management System

A full-stack banking application that allows users to create accounts, make deposits, and withdrawals through a modern web interface.

![Bank Africa](https://img.shields.io/badge/Bank-Africa-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)
![MySQL](https://img.shields.io/badge/MySQL-8.x-orange)

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [System Architecture](#system-architecture)
- [Database Schema](#database-schema)
- [API Documentation](#api-documentation)
- [Setup Instructions](#setup-instructions)
- [Usage Examples](#usage-examples)

## 🔍 Overview

Bank Africa Management System is a comprehensive banking solution that provides essential banking services through a user-friendly web interface. The system allows users to register accounts, log in securely, view account details, make deposits, and withdraw funds.

## ✨ Features

- **User Authentication**
  - User registration with personal details
  - Secure login with email and password
  - User profile management

- **Account Management**
  - Account creation with initial deposit (minimum R100)
  - Unique account number generation
  - Account balance display

- **Banking Operations**
  - Deposit funds
  - Withdraw funds (with balance validation)
  - View transaction history
  - Real-time balance updates

- **Security Features**
  - Input validation
  - Error handling
  - Session management

## 🛠️ Technology Stack

### Backend
- **Java 17**
- **Spring Boot**: Web application framework
- **Spring Data JPA**: Data access and persistence
- **Hibernate**: ORM for database operations
- **MySQL**: Relational database

### Frontend
- **HTML5/CSS3**: Structure and styling
- **JavaScript**: Client-side functionality
- **Fetch API**: AJAX requests to backend
- **Responsive Design**: Mobile-friendly interface

## 🏗️ System Architecture

The application follows a standard 3-tier architecture:

1. **Presentation Layer**: HTML/CSS/JavaScript frontend
2. **Business Logic Layer**: Spring Boot controllers and services
3. **Data Access Layer**: Spring Data JPA repositories and entities

### Component Diagram

```
┌─────────────┐     ┌─────────────────────────────────┐     ┌─────────────┐
│             │     │                                 │     │             │
│   Frontend  │◄───►│   Backend (Spring Boot REST)    │◄───►│   Database  │
│  (HTML/JS)  │     │                                 │     │   (MySQL)   │
│             │     │                                 │     │             │
└─────────────┘     └─────────────────────────────────┘     └─────────────┘
```

## 📊 Database Schema

The application uses two main entities:

### User Entity
- `id`: Primary key
- `first_name`: User's first name
- `last_name`: User's last name
- `email`: Unique email for login
- `id_number`: Unique national ID
- `phone_number`: Contact information
- `password`: Authentication credential
- `created_at`: Account creation timestamp
- One-to-one relationship with BankAccount

### BankAccount Entity
- `id`: Primary key
- `account_holder_name`: Name of account holder
- `balance`: Current account balance
- `created_at`: Account creation timestamp
- `account_number`: Unique 10-digit account number

## 📡 API Documentation

### Authentication Endpoints

#### Register User
- **URL**: `/api/auth/register`
- **Method**: `POST`
- **Request Body**:
  ```json
  {
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "idNumber": "9001015000000",
    "phoneNumber": "0712345678",
    "password": "securepassword",
    "initialDeposit": 500.00
  }
  ```
- **Success Response**: 200 OK with user ID and email

#### Login User
- **URL**: `/api/auth/login`
- **Method**: `POST`
- **Request Body**:
  ```json
  {
    "email": "john@example.com",
    "password": "securepassword"
  }
  ```
- **Success Response**: 200 OK with user details and account information

#### Get User Profile
- **URL**: `/api/auth/profile/{userId}`
- **Method**: `GET`
- **Success Response**: 200 OK with user profile and account details

### Banking Endpoints

#### Get Account Details
- **URL**: `/api/accounts/{accountId}`
- **Method**: `GET`
- **Success Response**: 200 OK with account details

#### Deposit Funds
- **URL**: `/api/deposit`
- **Method**: `POST`
- **Request Body**:
  ```json
  {
    "accountId": 1,
    "amount": 100.00
  }
  ```
- **Success Response**: 200 OK with new balance

#### Withdraw Funds
- **URL**: `/api/withdraw`
- **Method**: `POST`
- **Request Body**:
  ```json
  {
    "accountId": 1,
    "amount": 50.00
  }
  ```
- **Success Response**: 200 OK with new balance

#### Create Account
- **URL**: `/api/accounts`
- **Method**: `POST`
- **Request Body**:
  ```json
  {
    "name": "John Doe",
    "initialBalance": 200.00
  }
  ```
- **Success Response**: 200 OK with account details

## 🚀 Setup Instructions

### Prerequisites
- Java 17 or higher
- MySQL 8.0 or higher
- Maven 3.6 or higher

### Database Setup
1. Create a MySQL database named `bank_africa_db`
2. Create a user `bank_user` with password `bank_password`
3. Grant all privileges on `bank_africa_db` to `bank_user`

```sql
CREATE DATABASE bank_africa_db;
CREATE USER 'bank_user'@'localhost' IDENTIFIED BY 'bank_password';
GRANT ALL PRIVILEGES ON bank_africa_db.* TO 'bank_user'@'localhost';
FLUSH PRIVILEGES;
```

### Application Setup
1. Clone the repository
   ```bash
   git clone https://github.com/yourusername/Bank_Management_system.git
   cd Bank_Management_system
   ```

2. Navigate to the bankapp directory
   ```bash
   cd bankapp
   ```

3. Build the application
   ```bash
   ./mvnw clean package
   ```

4. Run the application
   ```bash
   ./mvnw spring-boot:run
   ```

5. Access the application at http://localhost:8080

## 📝 Usage Examples

### Registration Process
1. Navigate to the homepage
2. Click "Register here" to access the registration form
3. Fill in your personal details
4. Enter an initial deposit (minimum R100)
5. Click "Create Account"

### Login Process
1. Navigate to the homepage
2. Enter your email and password
3. Click "Sign In"

### Making a Deposit
1. Log in to your account
2. In the dashboard, find the "Deposit Funds" section
3. Enter the amount to deposit
4. Click "Deposit"

### Making a Withdrawal
1. Log in to your account
2. In the dashboard, find the "Withdraw Funds" section
3. Enter the amount to withdraw
4. Click "Withdraw"



## 👥 Contributors

- muthula muvhulawa
