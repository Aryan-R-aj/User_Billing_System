package com.ubs.dao;

import com.ubs.model.Resource;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


public class ResourceDAO {

    public Resource insert(String name, String category, int capacity) throws SQLException {
        String sql = "INSERT INTO resources (name, category, capacity, created_at) VALUES (?, ?, ?, ?)";
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setInt(3, capacity);
            ps.setString(4, now);
            ps.executeUpdate();
            int id = DatabaseManager.lastInsertId(conn);
            return new Resource(id, name, category, capacity, now);
        }
    }

    public List<Resource> findAll() throws SQLException {
        String sql =
            "SELECT r.id, r.name, r.category, r.capacity, r.created_at, " +
            "       (SELECT COUNT(*) FROM usage_sessions s WHERE s.resource_id = r.id AND s.status = 'ACTIVE') AS active_count " +
            "FROM resources r ORDER BY r.name";
        List<Resource> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Resource r = new Resource(rs.getInt("id"), rs.getString("name"),
                        rs.getString("category"), rs.getInt("capacity"), rs.getString("created_at"));
                r.setActiveCount(rs.getInt("active_count"));
                list.add(r);
            }
        }
        return list;
    }

    public Resource findById(int id) throws SQLException {
        String sql =
            "SELECT r.id, r.name, r.category, r.capacity, r.created_at, " +
            "       (SELECT COUNT(*) FROM usage_sessions s WHERE s.resource_id = r.id AND s.status = 'ACTIVE') AS active_count " +
            "FROM resources r WHERE r.id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Resource r = new Resource(rs.getInt("id"), rs.getString("name"),
                            rs.getString("category"), rs.getInt("capacity"), rs.getString("created_at"));
                    r.setActiveCount(rs.getInt("active_count"));
                    return r;
                }
            }
        }
        return null;
    }

    public boolean existsByName(String name) throws SQLException {
        String sql = "SELECT 1 FROM resources WHERE LOWER(name) = LOWER(?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM resources WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
