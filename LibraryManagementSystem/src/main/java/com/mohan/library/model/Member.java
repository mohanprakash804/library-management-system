package com.mohan.library.model;

import java.time.LocalDate;

public class Member {
    private int memberId;
    private String name;
    private String email;
    private String phone;
    private LocalDate joinedOn;

    public Member(int memberId, String name, String email, String phone, LocalDate joinedOn) {
        this.memberId = memberId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.joinedOn = joinedOn;
    }

    /** Convenience constructor for a new member that is not saved yet. */
    public Member(String name, String email, String phone) {
        this(0, name, email, phone, null);
    }

    public int getMemberId() { return memberId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public LocalDate getJoinedOn() { return joinedOn; }
}
