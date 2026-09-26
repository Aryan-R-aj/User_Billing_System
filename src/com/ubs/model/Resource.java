package com.ubs.model;


public class Resource {
    private int id;
    private String name;
    private String category;
    private int capacity;
    private String createdAt;

    // Not persisted directly - computed at read time for convenience in the UI.
    private int activeCount;

    public Resource() {
    }

    public Resource(int id, String name, String category, int capacity, String createdAt) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.capacity = capacity;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public int getActiveCount() { return activeCount; }
    public void setActiveCount(int activeCount) { this.activeCount = activeCount; }

    public int getAvailableSlots() { return capacity - activeCount; }

    public boolean isFull() { return activeCount >= capacity; }

    @Override
    public String toString() {
        return name + " (" + activeCount + "/" + capacity + " in use)";
    }
}
