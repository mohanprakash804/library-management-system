package com.mohan.library.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** A loan row joined with the book title and member name, used for reports. */
public class Loan {
    private final int loanId;
    private final String bookTitle;
    private final String memberName;
    private final LocalDate issueDate;
    private final LocalDate dueDate;

    public Loan(int loanId, String bookTitle, String memberName, LocalDate issueDate, LocalDate dueDate) {
        this.loanId = loanId;
        this.bookTitle = bookTitle;
        this.memberName = memberName;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
    }

    public int getLoanId() { return loanId; }
    public String getBookTitle() { return bookTitle; }
    public String getMemberName() { return memberName; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getDueDate() { return dueDate; }

    public long daysOverdue(LocalDate today) {
        return Math.max(0, ChronoUnit.DAYS.between(dueDate, today));
    }
}
