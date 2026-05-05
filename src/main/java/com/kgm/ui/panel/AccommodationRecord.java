package com.kgm.ui.panel;

import java.util.ArrayList;
import java.util.List;

public class AccommodationRecord {
    private String name;
    private String category;
    private int capacity;
    private String status;
    private String assignedStaff;
    private final List<String> amenities;

    public AccommodationRecord(
            String name,
            String category,
            int capacity,
            String status,
            String assignedStaff,
            List<String> amenities
    ) {
        this.name = name;
        this.category = category;
        this.capacity = capacity;
        this.status = status;
        this.assignedStaff = assignedStaff;
        this.amenities = new ArrayList<>(amenities);
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

    public String getAssignedStaff() {
        return assignedStaff;
    }

    public List<String> getAmenities() {
        return new ArrayList<>(amenities);
    }

    public String getAmenitiesText() {
        return String.join(", ", amenities);
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

    public void setAssignedStaff(String assignedStaff) {
        this.assignedStaff = assignedStaff;
    }
}
