-- Initial schema for the User Service

-- Users table
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(255),
    created_at TIMESTAMP,
    citizen_id VARCHAR(20) UNIQUE,
    dob DATE
);
