package com.kgm.ui.panel;

public class AccommodationRecord {
    private String name;
    private String category;
    private int capacity;
    private String status;
    private String location;
    private String description;

    public AccommodationRecord(String name, String category, int capacity, String status, String location, String description) {
        this.name = name;
        this.category = category;
        this.capacity = capacity;
        this.status = status;
        this.location = location;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getStatus() {
        return status;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
