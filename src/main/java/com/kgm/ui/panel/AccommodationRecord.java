package com.kgm.ui.panel;

import java.util.ArrayList;
import java.util.List;

public class AccommodationRecord {
    private long id;
    private String name;
    private String category;
    private int capacity;
    private int availableSeats;
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
        this(0, name, category, capacity, capacity, status, assignedStaff, amenities);
    }

    public AccommodationRecord(
            long id,
            String name,
            String category,
            int capacity,
            String status,
            String assignedStaff,
            List<String> amenities
    ) {
        this(id, name, category, capacity, capacity, status, assignedStaff, amenities);
    }

    public AccommodationRecord(
            long id,
            String name,
            String category,
            int capacity,
            int availableSeats,
            String status,
            String assignedStaff,
            List<String> amenities
    ) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.capacity = capacity;
        this.availableSeats = availableSeats;
        this.status = status;
        this.assignedStaff = assignedStaff;
        this.amenities = new ArrayList<>(amenities);
    }

    public long getId() {
        return id;
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

    public int getAvailableSeats() {
        return availableSeats;
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

    public void setId(long id) {
        this.id = id;
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

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setAssignedStaff(String assignedStaff) {
        this.assignedStaff = assignedStaff;
    }
}
