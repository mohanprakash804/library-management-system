package com.mohan.library;

import com.mohan.library.dao.BookDAO;
import com.mohan.library.dao.LoanDAO;
import com.mohan.library.dao.MemberDAO;
import com.mohan.library.model.Book;
import com.mohan.library.model.Loan;
import com.mohan.library.model.Member;
import com.mohan.library.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/** Console menu. It only talks to the user; all database work is done by the DAO classes. */
public class Main {

    private static final Scanner IN = new Scanner(System.in);
    private static final BookDAO BOOKS = new BookDAO();
    private static final MemberDAO MEMBERS = new MemberDAO();
    private static final LoanDAO LOANS = new LoanDAO();

    public static void main(String[] args) {
        System.out.println("=== Library Management System ===");
        try (Connection ignored = DBConnection.getConnection()) {
            System.out.println("Connected to the database.");
        } catch (SQLException e) {
            System.out.println("[!] Could not connect to the database: " + e.getMessage());
            System.out.println("    Check db.properties and make sure MySQL is running.");
            return;
        }

        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Choose an option: ");
            try {
                switch (choice) {
                    case 1 -> addBook();
                    case 2 -> showBooks(BOOKS.findAll());
                    case 3 -> searchBooks();
                    case 4 -> updateCopies();
                    case 5 -> deleteBook();
                    case 6 -> addMember();
                    case 7 -> showMembers();
                    case 8 -> issueBook();
                    case 9 -> returnBook();
                    case 10 -> showLoans(LOANS.findActiveLoans(), "Currently issued books");
                    case 11 -> showLoans(LOANS.findOverdueLoans(LocalDate.now()), "Overdue books");
                    case 12 -> showReports();
                    case 0 -> running = false;
                    default -> System.out.println("[!] Please choose a number from the menu.");
                }
            } catch (LibraryException e) {
                System.out.println("[!] " + e.getMessage());
            } catch (SQLException e) {
                handleSql(e);
            }
        }
        System.out.println("Goodbye!");
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("---------------- MENU ----------------");
        System.out.println(" 1. Add book            7. View members");
        System.out.println(" 2. View all books      8. Issue book");
        System.out.println(" 3. Search books        9. Return book");
        System.out.println(" 4. Update copies      10. Issued books");
        System.out.println(" 5. Delete book        11. Overdue books");
        System.out.println(" 6. Add member         12. Reports");
        System.out.println(" 0. Exit");
        System.out.println("--------------------------------------");
    }

    // ---------- books ----------

    private static void addBook() throws SQLException {
        String title = readText("Title: ");
        String author = readText("Author: ");
        String category = readText("Category: ");
        int copies = readPositiveInt("Number of copies: ");
        int id = BOOKS.addBook(new Book(title, author, category, copies));
        System.out.println("Book added with ID " + id + ".");
    }

    private static void searchBooks() throws SQLException {
        String keyword = readText("Search (title / author / category): ");
        showBooks(BOOKS.search(keyword));
    }

    private static void updateCopies() throws SQLException {
        int id = readInt("Book ID: ");
        int total = readInt("New total number of copies: ");
        if (total < 0) {
            System.out.println("[!] Copies cannot be negative.");
        } else if (BOOKS.updateTotalCopies(id, total)) {
            System.out.println("Copies updated.");
        } else {
            System.out.println("[!] Book not found, or the new total is less than the copies currently issued.");
        }
    }

    private static void deleteBook() throws SQLException {
        int id = readInt("Book ID to delete: ");
        System.out.println(BOOKS.deleteBook(id) ? "Book deleted." : "[!] Book not found.");
    }

    private static void showBooks(List<Book> books) {
        if (books.isEmpty()) {
            System.out.println("No books found.");
            return;
        }
        System.out.printf("%-4s %-32s %-24s %-12s %5s %5s%n", "ID", "Title", "Author", "Category", "Total", "Avail");
        for (Book b : books) {
            System.out.printf("%-4d %-32s %-24s %-12s %5d %5d%n", b.getBookId(), cut(b.getTitle(), 32),
                    cut(b.getAuthor(), 24), cut(b.getCategory(), 12), b.getTotalCopies(), b.getAvailableCopies());
        }
    }

    // ---------- members ----------

    private static void addMember() throws SQLException {
        String name = readText("Name: ");
        String email = readText("Email: ");
        String phone = readText("Phone: ");
        int id = MEMBERS.addMember(new Member(name, email, phone));
        System.out.println("Member added with ID " + id + ".");
    }

    private static void showMembers() throws SQLException {
        List<Member> members = MEMBERS.findAll();
        if (members.isEmpty()) {
            System.out.println("No members yet.");
            return;
        }
        System.out.printf("%-4s %-22s %-28s %-12s %-10s%n", "ID", "Name", "Email", "Phone", "Joined");
        for (Member m : members) {
            System.out.printf("%-4d %-22s %-28s %-12s %-10s%n", m.getMemberId(), cut(m.getName(), 22),
                    cut(m.getEmail(), 28), m.getPhone() == null ? "" : m.getPhone(), m.getJoinedOn());
        }
    }

    // ---------- loans ----------

    private static void issueBook() throws SQLException, LibraryException {
        int bookId = readInt("Book ID: ");
        int memberId = readInt("Member ID: ");
        LocalDate today = LocalDate.now();
        int loanId = LOANS.issueBook(bookId, memberId, today);
        System.out.println("Book issued. Loan ID " + loanId + ", due on " + today.plusDays(LoanDAO.LOAN_DAYS) + ".");
    }

    private static void returnBook() throws SQLException, LibraryException {
        int loanId = readInt("Loan ID: ");
        BigDecimal fine = LOANS.returnBook(loanId, LocalDate.now());
        System.out.println("Book returned.");
        if (fine.signum() > 0) {
            System.out.println("Late fine to collect: Rs. " + fine);
        } else {
            System.out.println("Returned on time. No fine.");
        }
    }

    private static void showLoans(List<Loan> loans, String heading) {
        System.out.println("\n" + heading + ":");
        if (loans.isEmpty()) {
            System.out.println("None.");
            return;
        }
        LocalDate today = LocalDate.now();
        System.out.printf("%-6s %-30s %-20s %-11s %-11s %s%n", "Loan", "Book", "Member", "Issued", "Due", "Overdue");
        for (Loan l : loans) {
            long late = l.daysOverdue(today);
            System.out.printf("%-6d %-30s %-20s %-11s %-11s %s%n", l.getLoanId(), cut(l.getBookTitle(), 30),
                    cut(l.getMemberName(), 20), l.getIssueDate(), l.getDueDate(), late > 0 ? late + " day(s)" : "-");
        }
    }

    private static void showReports() throws SQLException {
        System.out.println("\nTop 5 most borrowed books:");
        printCounts(LOANS.mostBorrowedBooks(5), "times");
        System.out.println("\nLoans per category:");
        printCounts(LOANS.loansByCategory(), "loans");
    }

    private static void printCounts(Map<String, Integer> counts, String unit) {
        if (counts.isEmpty()) {
            System.out.println("No loan data yet.");
            return;
        }
        counts.forEach((name, n) -> System.out.printf("  %-32s %d %s%n", cut(name, 32), n, unit));
    }

    // ---------- helpers ----------

    private static void handleSql(SQLException e) {
        if (e instanceof SQLIntegrityConstraintViolationException) {
            System.out.println("[!] Not allowed: duplicate value, or this record is linked to other data (for example a book that has loan history).");
        } else {
            System.out.println("[!] Database error: " + e.getMessage());
        }
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        if (!IN.hasNextLine()) {      // input closed (Ctrl+D / end of piped input)
            System.out.println();
            System.exit(0);
        }
        return IN.nextLine().trim();
    }

    private static String readText(String prompt) {
        String value = readLine(prompt);
        while (value.isEmpty()) {
            System.out.println("[!] This field cannot be empty.");
            value = readLine(prompt);
        }
        return value;
    }

    private static int readInt(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(readLine(prompt));
            } catch (NumberFormatException e) {
                System.out.println("[!] Please enter a valid number.");
            }
        }
    }

    private static int readPositiveInt(String prompt) {
        int value = readInt(prompt);
        while (value <= 0) {
            System.out.println("[!] Enter a number greater than 0.");
            value = readInt(prompt);
        }
        return value;
    }

    private static String cut(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}
