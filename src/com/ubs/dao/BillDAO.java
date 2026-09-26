package com.ubs.dao;

import com.ubs.model.Bill;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


public class BillDAO {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public Bill insert(Connection conn, Bill bill) throws SQLException {
        String sql = "INSERT INTO bills (session_id, resource_name, service_name, user_name, start_time, end_time, " +
                "duration_minutes, billed_hours, amount, generated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bill.getSessionId());
            ps.setString(2, bill.getResourceName());
            ps.setString(3, bill.getServiceName());
            ps.setString(4, bill.getUserName());
            ps.setString(5, bill.getStartTime());
            ps.setString(6, bill.getEndTime());
            ps.setLong(7, bill.getDurationMinutes());
            ps.setInt(8, bill.getBilledHours());
            ps.setDouble(9, bill.getAmount());
            ps.setString(10, bill.getGeneratedAt());
            ps.executeUpdate();
            bill.setId(DatabaseManager.lastInsertId(conn));
            return bill;
        }
    }

    public List<Bill> findAll() throws SQLException {
        String sql = "SELECT * FROM bills ORDER BY generated_at DESC";
        List<Bill> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    private Bill map(ResultSet rs) throws SQLException {
        Bill b = new Bill();
        b.setId(rs.getInt("id"));
        b.setSessionId(rs.getInt("session_id"));
        b.setResourceName(rs.getString("resource_name"));
        b.setServiceName(rs.getString("service_name"));
        b.setUserName(rs.getString("user_name"));
        b.setStartTime(rs.getString("start_time"));
        b.setEndTime(rs.getString("end_time"));
        b.setDurationMinutes(rs.getLong("duration_minutes"));
        b.setBilledHours(rs.getInt("billed_hours"));
        b.setAmount(rs.getDouble("amount"));
        b.setGeneratedAt(rs.getString("generated_at"));
        return b;
    }
}
