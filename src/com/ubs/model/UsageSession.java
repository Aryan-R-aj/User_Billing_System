package com.ubs.model;

import java.time.LocalDateTime;

/**
 * Represents one instance of a user occupying a resource, from start to
 * (eventually) stop. While status is ACTIVE the session occupies one slot
 * of the resource's capacity.
 */
public class UsageSession {
    public static final String ACTIVE = "ACTIVE";
    public static final String COMPLETED = "COMPLETED";

    private int id;
    private int resourceId;
    private int serviceId;
    private String userName;
    private LocalDateTime startTime;
    private LocalDateTime endTime; // null while active
    private String status;

    // Convenience display fields, populated by joined queries.
    private String resourceName;
    private String serviceName;

    public UsageSession() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getResourceId() { return resourceId; }
    public void setResourceId(int resourceId) { this.resourceId = resourceId; }

    public int getServiceId() { return serviceId; }
    public void setServiceId(int serviceId) { this.serviceId = serviceId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public boolean isActive() { return ACTIVE.equals(status); }
}
