package com.mohan.library.dao;

import com.mohan.library.model.Member;
import com.mohan.library.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MemberDAO {

    /** Email is UNIQUE in the table, so a duplicate email throws SQLIntegrityConstraintViolationException. */
    public int addMember(Member member) throws SQLException {
        String sql = "INSERT INTO members (name, email, phone) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, member.getName());
            ps.setString(2, member.getEmail());
            ps.setString(3, member.getPhone());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    public List<Member> findAll() throws SQLException {
        List<Member> members = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM members ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Date joined = rs.getDate("joined_on");
                members.add(new Member(
                        rs.getInt("member_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        joined == null ? null : joined.toLocalDate()));
            }
        }
        return members;
    }
}
