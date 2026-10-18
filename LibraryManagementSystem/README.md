# Library Management System (Java + JDBC + MySQL)

A menu-driven console application to manage a small library: books, members, issuing and returning
books, late fines and reports. It is built with **Core Java**, **JDBC** and **MySQL**, using a layered
design (model → DAO → menu).

## Features
- Add, list, search, update (copies) and delete books
- Add and list members (unique email)
- Issue a book (14-day loan, max 3 books per member, no duplicate copy for the same member)
- Return a book with automatic late fine (Rs. 2 per day after the due date)
- Reports: currently issued books, overdue books, most borrowed books, loans per category
- Friendly error messages for business rules (no copies left, member not found, already returned...)

## Tech stack
Java 17 • JDBC • MySQL 8 • Maven (optional)

## Project structure
```
LibraryManagementSystem/
├── pom.xml
├── sql/
│   ├── schema.sql              # tables, keys, constraints, indexes
│   └── sample_data.sql         # optional demo data
└── src/main/
    ├── resources/db.properties.example
    └── java/com/mohan/library/
        ├── Main.java           # console menu (talks to the user only)
        ├── LibraryException.java   # business-rule errors
        ├── model/              # Book, Member, Loan  (plain Java classes)
        ├── dao/                # BookDAO, MemberDAO, LoanDAO  (all SQL lives here)
        └── util/DBConnection.java  # creates JDBC connections
```

## Database design
```
books (book_id PK, title, author, category, total_copies, available_copies)
members (member_id PK, name, email UNIQUE, phone, joined_on)
loans (loan_id PK, book_id FK→books, member_id FK→members,
       issue_date, due_date, return_date NULL, fine_amount)
```
One book → many loans, one member → many loans. A loan with `return_date IS NULL` is still issued.

## Setup
1. Install **JDK 17+** and **MySQL 8+** (Maven is optional).
2. Create the database and tables:
   ```
   mysql -u root -p < sql/schema.sql
   mysql -u root -p < sql/sample_data.sql      # optional demo data
   ```
3. Configure the connection:
   ```
   cp src/main/resources/db.properties.example src/main/resources/db.properties
   ```
   Open `db.properties` and put your MySQL username and password.
   (This file is in `.gitignore`, so your password is not uploaded to GitHub.)
   You can also use environment variables `DB_URL`, `DB_USER`, `DB_PASSWORD`.

## Run
**With Maven**
```
mvn compile exec:java
```
**With an IDE (IntelliJ / Eclipse / VS Code):** open the folder as a Maven project and run `Main`.

**Without Maven:** download the MySQL driver `mysql-connector-j-8.x.jar`, then
```
javac -d out $(find src -name "*.java")
cp src/main/resources/db.properties out/
java -cp out:mysql-connector-j-8.x.jar com.mohan.library.Main      # use ; instead of : on Windows
```

## Sample session
```
Choose an option: 8
Book ID: 1
Member ID: 1
Book issued. Loan ID 1, due on 2026-11-01.

Choose an option: 9
Loan ID: 2
Book returned.
Late fine to collect: Rs. 10.00        (5 days late x Rs. 2)
```

## How it works (read this before an interview)
- **DAO pattern:** `Main` never writes SQL. Each DAO class owns the SQL of one table, so changing the
  database logic does not touch the menu.
- **PreparedStatement** is used everywhere (`?` placeholders). This prevents SQL injection.
- **try-with-resources** closes Connection, Statement and ResultSet automatically.
- **Transactions:** issuing a book inserts a loan row *and* reduces `available_copies`. Both happen inside
  one transaction (`setAutoCommit(false)` → `commit()` / `rollback()`), so the data never ends up half-updated.
- **Row locking:** `SELECT ... FOR UPDATE` locks the book row so two users cannot take the last copy together.
- **Constraints in the database:** `PRIMARY KEY`, `FOREIGN KEY`, `UNIQUE (email)` and
  `CHECK (available_copies <= total_copies)` protect the data even if the Java code has a bug.
- **Indexes:** on `books.title`, `books.author`, `loans.member_id` and `loans(book_id, return_date)` for faster search and joins.
- **SQL concepts used:** INNER JOIN (3 tables), GROUP BY + COUNT, ORDER BY, LIMIT, LIKE, subqueries,
  `IS NULL`, `UPDATE` with arithmetic.
- **OOP concepts used:** encapsulation (private fields + getters), custom checked exception, separation of concerns.

## Interview questions you should be able to answer
1. Why PreparedStatement instead of Statement? *(SQL injection, precompiled, easier parameters)*
2. What is a transaction and why is it needed in `issueBook`? *(two tables must change together)*
3. What does `FOR UPDATE` do? *(locks the row until commit/rollback)*
4. What is the DAO pattern? *(separates data-access code from business/UI code)*
5. Difference between `INNER JOIN` and `LEFT JOIN`? Which did you use and why?
6. Why `ON DELETE` is not cascade here? *(a book with loan history cannot be deleted; history must stay)*
7. What is a foreign key / composite index? Where are they used in this project?
8. What happens if the connection fails halfway through `returnBook`? *(rollback)*

## Ideas to extend it
- Use a connection pool (HikariCP) instead of opening a connection per call
- Add a Spring Boot REST API on top of the same tables
- Add login for librarian and members, unit tests with JUnit, and a Swing / web UI
