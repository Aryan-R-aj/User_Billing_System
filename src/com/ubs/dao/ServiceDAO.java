package com.ubs.dao;

import com.ubs.model.ServicePlan;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class ServiceDAO {

    public ServicePlan insert(int resourceId, String serviceName, double firstHourPrice, double additionalHourPrice) throws SQLException {
        String sql = "INSERT INTO services (resource_id, service_name, first_hour_price, additional_hour_price) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, resourceId);
            ps.setString(2, serviceName);
            ps.setDouble(3, firstHourPrice);
            ps.setDouble(4, additionalHourPrice);
            ps.executeUpdate();
            int id = DatabaseManager.lastInsertId(conn);
            return new ServicePlan(id, resourceId, serviceName, firstHourPrice, additionalHourPrice);
        }
    }

    public List<ServicePlan> findAll() throws SQLException {
        String sql =
            "SELECT s.id, s.resource_id, s.service_name, s.first_hour_price, s.additional_hour_price, r.name AS resource_name " +
            "FROM services s JOIN resources r ON r.id = s.resource_id ORDER BY r.name, s.service_name";
        List<ServicePlan> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                ServicePlan sp = new ServicePlan(rs.getInt("id"), rs.getInt("resource_id"),
                        rs.getString("service_name"), rs.getDouble("first_hour_price"),
                        rs.getDouble("additional_hour_price"));
                sp.setResourceName(rs.getString("resource_name"));
                list.add(sp);
            }
        }
        return list;
    }

    public List<ServicePlan> findByResourceId(int resourceId) throws SQLException {
        String sql = "SELECT id, resource_id, service_name, first_hour_price, additional_hour_price " +
                "FROM services WHERE resource_id = ? ORDER BY service_name";
        List<ServicePlan> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ServicePlan(rs.getInt("id"), rs.getInt("resource_id"),
                            rs.getString("service_name"), rs.getDouble("first_hour_price"),
                            rs.getDouble("additional_hour_price")));
                }
            }
        }
        return list;
    }

    public ServicePlan findById(int id) throws SQLException {
        String sql = "SELECT id, resource_id, service_name, first_hour_price, additional_hour_price FROM services WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ServicePlan(rs.getInt("id"), rs.getInt("resource_id"),
                            rs.getString("service_name"), rs.getDouble("first_hour_price"),
                            rs.getDouble("additional_hour_price"));
                }
            }
        }
        return null;
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM services WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
