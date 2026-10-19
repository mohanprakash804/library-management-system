package com.mohan.library.dao;

import com.mohan.library.LibraryException;
import com.mohan.library.model.Loan;
import com.mohan.library.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Issue / return logic and reports.
 * Issuing and returning change two tables (loans + books), so each one runs inside a
 * database TRANSACTION: either both changes are saved (commit) or neither (rollback).
 */
public class LoanDAO {

    public static final int LOAN_DAYS = 14;
    public static final int MAX_ACTIVE_LOANS = 3;
    public static final BigDecimal FINE_PER_DAY = new BigDecimal("2.00");

    /** Issues a book to a member. Returns the new loan id. */
    public int issueBook(int bookId, int memberId, LocalDate today) throws SQLException, LibraryException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                // 1. Lock the book row so two people cannot take the last copy at the same time.
                int available;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT available_copies FROM books WHERE book_id = ? FOR UPDATE")) {
                    ps.setInt(1, bookId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new LibraryException("Book ID " + bookId + " not found.");
                        }
                        available = rs.getInt(1);
                    }
                }
                if (available <= 0) {
                    throw new LibraryException("No copies of this book are available right now.");
                }

                // 2. Check the member and the borrowing rules in one query.
                String check = "SELECT "
                        + "(SELECT COUNT(*) FROM members WHERE member_id = ?) AS member_exists, "
                        + "(SELECT COUNT(*) FROM loans WHERE member_id = ? AND return_date IS NULL) AS active_loans, "
                        + "(SELECT COUNT(*) FROM loans WHERE member_id = ? AND book_id = ? AND return_date IS NULL) AS same_book";
                try (PreparedStatement ps = con.prepareStatement(check)) {
                    ps.setInt(1, memberId);
                    ps.setInt(2, memberId);
                    ps.setInt(3, memberId);
                    ps.setInt(4, bookId);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        if (rs.getInt("member_exists") == 0) {
                            throw new LibraryException("Member ID " + memberId + " not found.");
                        }
                        if (rs.getInt("active_loans") >= MAX_ACTIVE_LOANS) {
                            throw new LibraryException("Member already has " + MAX_ACTIVE_LOANS + " books issued (limit reached).");
                        }
                        if (rs.getInt("same_book") > 0) {
                            throw new LibraryException("Member already has a copy of this book.");
                        }
                    }
                }

                // 3. Create the loan.
                int loanId;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO loans (book_id, member_id, issue_date, due_date) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, bookId);
                    ps.setInt(2, memberId);
                    ps.setDate(3, Date.valueOf(today));
                    ps.setDate(4, Date.valueOf(today.plusDays(LOAN_DAYS)));
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        loanId = keys.getInt(1);
                    }
                }

                // 4. Reduce available copies.
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE books SET available_copies = available_copies - 1 WHERE book_id = ?")) {
                    ps.setInt(1, bookId);
                    ps.executeUpdate();
                }

                con.commit();
                return loanId;
            } catch (SQLException | LibraryException | RuntimeException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    /** Returns a book. The fine (if the book is late) is calculated and returned. */
    public BigDecimal returnBook(int loanId, LocalDate today) throws SQLException, LibraryException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int bookId;
                LocalDate dueDate;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT book_id, due_date, return_date FROM loans WHERE loan_id = ? FOR UPDATE")) {
                    ps.setInt(1, loanId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new LibraryException("Loan ID " + loanId + " not found.");
                        }
                        if (rs.getDate("return_date") != null) {
                            throw new LibraryException("This book was already returned.");
                        }
                        bookId = rs.getInt("book_id");
                        dueDate = rs.getDate("due_date").toLocalDate();
                    }
                }

                BigDecimal fine = BigDecimal.ZERO.setScale(2);
                if (today.isAfter(dueDate)) {
                    long daysLate = ChronoUnit.DAYS.between(dueDate, today);
                    fine = FINE_PER_DAY.multiply(BigDecimal.valueOf(daysLate));
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE loans SET return_date = ?, fine_amount = ? WHERE loan_id = ?")) {
                    ps.setDate(1, Date.valueOf(today));
                    ps.setBigDecimal(2, fine);
                    ps.setInt(3, loanId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE books SET available_copies = available_copies + 1 WHERE book_id = ?")) {
                    ps.setInt(1, bookId);
                    ps.executeUpdate();
                }

                con.commit();
                return fine;
            } catch (SQLException | LibraryException | RuntimeException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    private static final String ACTIVE_LOANS_SQL =
            "SELECT l.loan_id, b.title, m.name, l.issue_date, l.due_date "
          + "FROM loans l "
          + "JOIN books b   ON b.book_id = l.book_id "
          + "JOIN members m ON m.member_id = l.member_id "
          + "WHERE l.return_date IS NULL ";

    /** All books that are currently issued (not yet returned). */
    public List<Loan> findActiveLoans() throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(ACTIVE_LOANS_SQL + "ORDER BY l.due_date")) {
            return readLoans(ps);
        }
    }

    /** Issued books whose due date is already over. */
    public List<Loan> findOverdueLoans(LocalDate today) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(ACTIVE_LOANS_SQL + "AND l.due_date < ? ORDER BY l.due_date")) {
            ps.setDate(1, Date.valueOf(today));
            return readLoans(ps);
        }
    }

    /** Report: most borrowed books (JOIN + GROUP BY + ORDER BY + LIMIT). */
    public Map<String, Integer> mostBorrowedBooks(int limit) throws SQLException {
        String sql = "SELECT b.title, COUNT(*) AS times_borrowed "
                   + "FROM loans l JOIN books b ON b.book_id = l.book_id "
                   + "GROUP BY b.book_id, b.title "
                   + "ORDER BY times_borrowed DESC, b.title "
                   + "LIMIT ?";
        return readCounts(sql, limit);
    }

    /** Report: number of loans per book category (JOIN + GROUP BY). */
    public Map<String, Integer> loansByCategory() throws SQLException {
        String sql = "SELECT b.category, COUNT(*) AS total_loans "
                   + "FROM loans l JOIN books b ON b.book_id = l.book_id "
                   + "GROUP BY b.category "
                   + "ORDER BY total_loans DESC, b.category";
        return readCounts(sql, null);
    }

    private Map<String, Integer> readCounts(String sql, Integer limit) throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (limit != null) {
                ps.setInt(1, limit);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString(1), rs.getInt(2));
                }
            }
        }
        return result;
    }

    private List<Loan> readLoans(PreparedStatement ps) throws SQLException {
        List<Loan> loans = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                loans.add(new Loan(
                        rs.getInt("loan_id"),
                        rs.getString("title"),
                        rs.getString("name"),
                        rs.getDate("issue_date").toLocalDate(),
                        rs.getDate("due_date").toLocalDate()));
            }
        }
        return loans;
    }
}
