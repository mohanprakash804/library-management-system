package com.mohan.library.dao;

import com.mohan.library.model.Book;
import com.mohan.library.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Data Access Object: all SQL for the books table lives here. */
public class BookDAO {

    public int addBook(Book book) throws SQLException {
        String sql = "INSERT INTO books (title, author, category, total_copies, available_copies) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setString(3, book.getCategory());
            ps.setInt(4, book.getTotalCopies());
            ps.setInt(5, book.getTotalCopies());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    public List<Book> findAll() throws SQLException {
        String sql = "SELECT * FROM books ORDER BY title";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            return readBooks(ps);
        }
    }

    public List<Book> search(String keyword) throws SQLException {
        String sql = "SELECT * FROM books WHERE title LIKE ? OR author LIKE ? OR category LIKE ? ORDER BY title";
        String like = "%" + keyword + "%";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            return readBooks(ps);
        }
    }

    /**
     * Changes the total number of copies. Available copies move by the same difference,
     * and the update is rejected if it would make available copies negative
     * (i.e. fewer copies than are currently issued).
     */
    public boolean updateTotalCopies(int bookId, int newTotal) throws SQLException {
        String sql = "UPDATE books SET available_copies = available_copies + (? - total_copies), total_copies = ? "
                   + "WHERE book_id = ? AND available_copies + (? - total_copies) >= 0";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, newTotal);
            ps.setInt(2, newTotal);
            ps.setInt(3, bookId);
            ps.setInt(4, newTotal);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteBook(int bookId) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM books WHERE book_id = ?")) {
            ps.setInt(1, bookId);
            return ps.executeUpdate() > 0;
        }
    }

    private List<Book> readBooks(PreparedStatement ps) throws SQLException {
        List<Book> books = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                books.add(new Book(
                        rs.getInt("book_id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("category"),
                        rs.getInt("total_copies"),
                        rs.getInt("available_copies")));
            }
        }
        return books;
    }
}
