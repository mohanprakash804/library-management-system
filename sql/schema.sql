-- Library Management System : database schema (MySQL 8+)
-- Run:  mysql -u root -p < sql/schema.sql

CREATE DATABASE IF NOT EXISTS library_db;
USE library_db;

CREATE TABLE IF NOT EXISTS books (
    book_id          INT AUTO_INCREMENT PRIMARY KEY,
    title            VARCHAR(150) NOT NULL,
    author           VARCHAR(100) NOT NULL,
    category         VARCHAR(50)  NOT NULL DEFAULT 'General',
    total_copies     INT NOT NULL DEFAULT 1,
    available_copies INT NOT NULL DEFAULT 1,
    CONSTRAINT chk_copies CHECK (total_copies >= 0
                                 AND available_copies >= 0
                                 AND available_copies <= total_copies),
    INDEX idx_books_title (title),
    INDEX idx_books_author (author)
);

CREATE TABLE IF NOT EXISTS members (
    member_id INT AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(100) NOT NULL,
    email     VARCHAR(120) NOT NULL UNIQUE,
    phone     VARCHAR(15),
    joined_on DATE NOT NULL DEFAULT (CURRENT_DATE)
);

CREATE TABLE IF NOT EXISTS loans (
    loan_id     INT AUTO_INCREMENT PRIMARY KEY,
    book_id     INT NOT NULL,
    member_id   INT NOT NULL,
    issue_date  DATE NOT NULL,
    due_date    DATE NOT NULL,
    return_date DATE NULL,
    fine_amount DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_loans_book   FOREIGN KEY (book_id)   REFERENCES books(book_id),
    CONSTRAINT fk_loans_member FOREIGN KEY (member_id) REFERENCES members(member_id),
    INDEX idx_loans_member (member_id),
    INDEX idx_loans_book_return (book_id, return_date)
);
