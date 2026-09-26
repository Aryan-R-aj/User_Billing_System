package com.ubs.service;

import com.ubs.dao.BillDAO;
import com.ubs.dao.DatabaseManager;
import com.ubs.dao.ResourceDAO;
import com.ubs.dao.ServiceDAO;
import com.ubs.dao.UsageSessionDAO;
import com.ubs.model.Bill;
import com.ubs.model.Resource;
import com.ubs.model.ServicePlan;
import com.ubs.model.UsageSession;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class BillingService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ResourceDAO resourceDAO = new ResourceDAO();
    private final ServiceDAO serviceDAO = new ServiceDAO();
    private final UsageSessionDAO usageSessionDAO = new UsageSessionDAO();
    private final BillDAO billDAO = new BillDAO();


    public synchronized UsageSession startUsage(int resourceId, int serviceId, String userName) throws SQLException, BillingException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Resource resource = fetchResourceForUpdate(conn, resourceId);
                if (resource == null) {
                    throw new BillingException("Selected resource no longer exists.");
                }
                int activeCount = usageSessionDAO.countActiveForResource(conn, resourceId);
                if (activeCount >= resource.getCapacity()) {
                    throw new BillingException(
                        "\"" + resource.getName() + "\" is at full capacity (" +
                        activeCount + "/" + resource.getCapacity() + "). Usage request rejected.");
                }
                UsageSession session = usageSessionDAO.insertActive(conn, resourceId, serviceId, userName, LocalDateTime.now());
                conn.commit();
                return session;
            } catch (BillingException be) {
                conn.rollback();
                throw be;
            } catch (SQLException se) {
                conn.rollback();
                throw se;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Stops the given active usage session: records the end time,
     * calculates the bill according to the pricing rules, persists the
     * bill, and frees the resource slot (by marking the session COMPLETED).
     */
    public synchronized Bill stopUsage(int sessionId) throws SQLException, BillingException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                UsageSession session = usageSessionDAO.findById(conn, sessionId);
                if (session == null || !session.isActive()) {
                    throw new BillingException("This usage session is not currently active.");
                }
                LocalDateTime endTime = LocalDateTime.now();
                usageSessionDAO.markCompleted(conn, sessionId, endTime);

                Resource resource = fetchResourceForUpdate(conn, session.getResourceId());
                ServicePlan plan = serviceDAO.findById(session.getServiceId());

                Bill bill = calculateBill(session, resource, plan, endTime);
                billDAO.insert(conn, bill);

                conn.commit();
                return bill;
            } catch (BillingException be) {
                conn.rollback();
                throw be;
            } catch (SQLException se) {
                conn.rollback();
                throw se;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }


    Bill calculateBill(UsageSession session, Resource resource, ServicePlan plan, LocalDateTime endTime) {
        Duration duration = Duration.between(session.getStartTime(), endTime);
        long totalMinutes = Math.max(duration.toMinutes(), 0);
        long totalSeconds = Math.max(duration.getSeconds(), 0);

        int billedHours;
        if (totalSeconds == 0) {
            billedHours = 1; // minimum one hour billed for any recorded usage
        } else {
            billedHours = (int) Math.ceil(totalSeconds / 3600.0);
        }

        double amount = plan.getFirstHourPrice();
        if (billedHours > 1) {
            amount += (billedHours - 1) * plan.getAdditionalHourPrice();
        }

        Bill bill = new Bill();
        bill.setSessionId(session.getId());
        bill.setResourceName(resource != null ? resource.getName() : session.getResourceName());
        bill.setServiceName(plan != null ? plan.getServiceName() : session.getServiceName());
        bill.setUserName(session.getUserName());
        bill.setStartTime(session.getStartTime().format(FMT));
        bill.setEndTime(endTime.format(FMT));
        bill.setDurationMinutes(totalMinutes);
        bill.setBilledHours(billedHours);
        bill.setAmount(round2(amount));
        bill.setGeneratedAt(LocalDateTime.now().format(FMT));
        return bill;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private Resource fetchResourceForUpdate(Connection conn, int resourceId) throws SQLException {
        // ResourceDAO opens its own connection for simplicity elsewhere; for the
        // transactional path we only need capacity + name, fetched via the same
        // connection to stay consistent within the transaction boundary.
        String sql = "SELECT id, name, category, capacity, created_at FROM resources WHERE id = ?";
        try (var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, resourceId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Resource(rs.getInt("id"), rs.getString("name"),
                            rs.getString("category"), rs.getInt("capacity"), rs.getString("created_at"));
                }
            }
        }
        return null;
    }
}
