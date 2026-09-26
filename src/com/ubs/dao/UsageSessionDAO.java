package com.ubs.dao;

import com.ubs.model.UsageSession;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


public class UsageSessionDAO {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public int countActiveForResource(Connection conn, int resourceId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM usage_sessions WHERE resource_id = ? AND status = 'ACTIVE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public UsageSession insertActive(Connection conn, int resourceId, int serviceId, String userName, LocalDateTime startTime) throws SQLException {
        String sql = "INSERT INTO usage_sessions (resource_id, service_id, user_name, start_time, status) VALUES (?, ?, ?, ?, 'ACTIVE')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, resourceId);
            ps.setInt(2, serviceId);
            ps.setString(3, userName);
            ps.setString(4, startTime.format(FMT));
            ps.executeUpdate();
            int id = DatabaseManager.lastInsertId(conn);
            UsageSession s = new UsageSession();
            s.setId(id);
            s.setResourceId(resourceId);
            s.setServiceId(serviceId);
            s.setUserName(userName);
            s.setStartTime(startTime);
            s.setStatus(UsageSession.ACTIVE);
            return s;
        }
    }

    public UsageSession findById(Connection conn, int id) throws SQLException {
        String sql = "SELECT id, resource_id, service_id, user_name, start_time, end_time, status FROM usage_sessions WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    public void markCompleted(Connection conn, int sessionId, LocalDateTime endTime) throws SQLException {
        String sql = "UPDATE usage_sessions SET end_time = ?, status = 'COMPLETED' WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, endTime.format(FMT));
            ps.setInt(2, sessionId);
            ps.executeUpdate();
        }
    }

    /** Lists all currently active sessions, joined with resource/service names for display. */
    public List<UsageSession> findAllActive() throws SQLException {
        String sql =
            "SELECT u.id, u.resource_id, u.service_id, u.user_name, u.start_time, u.end_time, u.status, " +
            "       r.name AS resource_name, sv.service_name " +
            "FROM usage_sessions u " +
            "JOIN resources r ON r.id = u.resource_id " +
            "JOIN services sv ON sv.id = u.service_id " +
            "WHERE u.status = 'ACTIVE' ORDER BY u.start_time";
        List<UsageSession> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                UsageSession s = map(rs);
                s.setResourceName(rs.getString("resource_name"));
                s.setServiceName(rs.getString("service_name"));
                list.add(s);
            }
        }
        return list;
    }

    private UsageSession map(ResultSet rs) throws SQLException {
        UsageSession s = new UsageSession();
        s.setId(rs.getInt("id"));
        s.setResourceId(rs.getInt("resource_id"));
        s.setServiceId(rs.getInt("service_id"));
        s.setUserName(rs.getString("user_name"));
        s.setStartTime(LocalDateTime.parse(rs.getString("start_time"), FMT));
        String end = rs.getString("end_time");
        if (end != null) {
            s.setEndTime(LocalDateTime.parse(end, FMT));
        }
        s.setStatus(rs.getString("status"));
        return s;
    }
}
