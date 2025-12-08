-- Initial schema for the Reference Service

-- Banks table
CREATE TABLE banks (
    bank_code VARCHAR(10) PRIMARY KEY,
    bank_name VARCHAR(100) NOT NULL,
    logo_url VARCHAR(200),
    status VARCHAR(10),
    created_at TIMESTAMP
);

-- Branches table
CREATE TABLE branches (
    branch_id SERIAL PRIMARY KEY,
    bank_code VARCHAR(10) NOT NULL,
    branch_name VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    city VARCHAR(50),
    status VARCHAR(20),
    created_at TIMESTAMP,
    CONSTRAINT fk_branches_bank FOREIGN KEY (bank_code) REFERENCES banks(bank_code)
);

-- Product catalog table
CREATE TABLE product_catalog (
    product_id SERIAL PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    status VARCHAR(20),
    created_at TIMESTAMP
);