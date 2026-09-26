package com.ubs.model;

/**
 * Represents a pricing plan ("service") attached to a resource, e.g.
 * "Hourly usage" for a meeting room: first hour costs X, every additional
 * hour (or part thereof, rounded up) costs Y.
 */
public class ServicePlan {
    private int id;
    private int resourceId;
    private String resourceName; // convenience field for display, not always populated
    private String serviceName;
    private double firstHourPrice;
    private double additionalHourPrice;

    public ServicePlan() {
    }

    public ServicePlan(int id, int resourceId, String serviceName,
                        double firstHourPrice, double additionalHourPrice) {
        this.id = id;
        this.resourceId = resourceId;
        this.serviceName = serviceName;
        this.firstHourPrice = firstHourPrice;
        this.additionalHourPrice = additionalHourPrice;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getResourceId() { return resourceId; }
    public void setResourceId(int resourceId) { this.resourceId = resourceId; }

    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public double getFirstHourPrice() { return firstHourPrice; }
    public void setFirstHourPrice(double firstHourPrice) { this.firstHourPrice = firstHourPrice; }

    public double getAdditionalHourPrice() { return additionalHourPrice; }
    public void setAdditionalHourPrice(double additionalHourPrice) { this.additionalHourPrice = additionalHourPrice; }

    @Override
    public String toString() {
        return serviceName + " (1st hr \u20B9" + firstHourPrice + ", +hr \u20B9" + additionalHourPrice + ")";
    }
}
