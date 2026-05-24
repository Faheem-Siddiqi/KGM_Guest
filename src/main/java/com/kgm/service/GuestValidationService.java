package com.kgm.service;

import com.kgm.dao.AccommodationDao;
import com.kgm.model.Guest;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GuestValidationService {
    public static final String LEGACY_REMARK = "Old record - validation bypassed";

    private static final String ALL_ROOMS_OCCUPIED = "All rooms occupied";
    private static final String NO_ROOMS_AVAILABLE = "No rooms available";

    private final AccommodationDao accommodationDao = new AccommodationDao();

    public ValidationResult validateStandardGuest(Guest guest) throws SQLException {
        return validate(guest, ValidationMode.STANDARD);
    }

    public ValidationResult validateLegacyImportGuest(Guest guest) throws SQLException {
        return validate(guest, ValidationMode.LEGACY);
    }

    private ValidationResult validate(Guest guest, ValidationMode mode) throws SQLException {
        List<String> missingFields = mode == ValidationMode.STANDARD
                ? missingStandardFields(guest)
                : missingLegacyFields(guest);
        List<String> fieldIssues = new ArrayList<>();
        String dateIssue = dateIssue(guest);
        String roomIssue = roomIssue(guest, mode);

        if (mode == ValidationMode.STANDARD
                && !containsField(missingFields, "Guest CNIC")
                && !text(guest.getCnic()).matches("\\d{13}")) {
            fieldIssues.add("Guest CNIC must contain exactly 13 digits.");
        }

        return new ValidationResult(missingFields, fieldIssues, dateIssue, roomIssue);
    }

    public void prepareLegacyGuest(Guest guest) {
        if (isBlank(guest.getGuestName())) {
            guest.setGuestName("N/A");
        }
        if (isBlank(guest.getCnic())) {
            guest.setCnic("N/A");
        }
        if (isBlank(guest.getNationality())) {
            guest.setNationality("N/A");
        }
        if (isBlank(guest.getGuestCategory())) {
            guest.setGuestCategory("N/A");
        }
        if (isBlank(guest.getCompanyName())) {
            guest.setCompanyName("N/A");
        }
        if (isBlank(guest.getVisitType())) {
            guest.setVisitType("Official Visit");
        }
        if (isBlank(guest.getAddress())) {
            guest.setAddress("N/A");
        }
        if (isBlank(guest.getRequestedBy())) {
            guest.setRequestedBy("N/A");
        }
        if (isBlank(guest.getRequestedDepartment())) {
            guest.setRequestedDepartment("N/A");
        }
        if (isBlank(guest.getApprovedBy())) {
            guest.setApprovedBy("N/A");
        }
        if (isBlank(guest.getAccommodatedBy())) {
            guest.setAccommodatedBy("N/A");
        }
        guest.setRemarks(legacyRemarks(guest.getRemarks()));
    }

    public static List<String> dialogSections(ValidationResult result) {
        List<String> sections = new ArrayList<>();
        if (!result.missingFields().isEmpty()) {
            sections.add(missingFieldsMessage(result.missingFields()));
        }
        if (!result.fieldIssues().isEmpty()) {
            sections.add("Check field format\n" + String.join("\n", result.fieldIssues()));
        }
        if (result.dateIssue() != null) {
            sections.add("Check stay dates\n" + result.dateIssue());
        }
        if (result.roomIssue() != null) {
            sections.add("Room availability\n" + result.roomIssue());
        }
        return sections;
    }

    public static String rowMessage(ValidationResult result) {
        List<String> issues = new ArrayList<>();
        if (!result.missingFields().isEmpty()) {
            issues.add("Missing required fields: " + String.join(", ", result.missingFields()) + ".");
        }
        issues.addAll(result.fieldIssues());
        if (result.dateIssue() != null) {
            issues.add(result.dateIssue());
        }
        if (result.roomIssue() != null) {
            issues.add(result.roomIssue());
        }
        return String.join(" ", issues);
    }

    public static boolean hasIssues(ValidationResult result) {
        return !result.missingFields().isEmpty()
                || !result.fieldIssues().isEmpty()
                || result.dateIssue() != null
                || result.roomIssue() != null;
    }

    public static String stayStatus(Date arrivalAt, Date departureAt) {
        if (arrivalAt == null || departureAt == null) {
            return "Unknown";
        }
        Date now = new Date();
        if (!departureAt.after(arrivalAt)) {
            return "Invalid Dates";
        }
        if (!departureAt.after(now)) {
            return "Departed";
        }
        if (arrivalAt.after(now)) {
            return "Upcoming";
        }
        return "Currently Staying";
    }

    private List<String> missingStandardFields(Guest guest) {
        List<String> missingFields = new ArrayList<>();
        addIfBlank(missingFields, "Guest Name", guest.getGuestName());
        addIfBlank(missingFields, "Guest CNIC", guest.getCnic());
        addIfBlank(missingFields, "Guest Nationality", guest.getNationality());
        addIfBlank(missingFields, "Guest Category", guest.getGuestCategory());
        addIfBlank(missingFields, "Guest Address", guest.getAddress());
        addIfBlank(missingFields, "Requested By", guest.getRequestedBy());
        addIfBlank(missingFields, "Requested Department", guest.getRequestedDepartment());
        addIfBlank(missingFields, "Approved By", guest.getApprovedBy());
        addIfBlank(missingFields, "Accommodated By", guest.getAccommodatedBy());
        addIfBlank(missingFields, "Accommodation Category", guest.getAccommodation());
        return missingFields;
    }

    private List<String> missingLegacyFields(Guest guest) {
        List<String> missingFields = new ArrayList<>();
        addIfBlank(missingFields, "Accommodation Category", guest.getAccommodation());
        addIfBlank(missingFields, "Room", guest.getRoomName());
        return missingFields;
    }

    private String dateIssue(Guest guest) {
        Date arrivalAt = guest.getArrivalAt();
        Date departureAt = guest.getDepartureAt();
        if (arrivalAt == null || departureAt == null) {
            return "Arrival and departure dates are required.";
        }
        if (!departureAt.after(arrivalAt)) {
            return "Departure date must be after arrival date.";
        }
        return null;
    }

    private String roomIssue(Guest guest, ValidationMode mode) throws SQLException {
        String accommodationCategory = text(guest.getAccommodation());
        String room = text(guest.getRoomName());
        if (accommodationCategory.isEmpty() || room.isEmpty() || isRoomPlaceholder(room)) {
            if (mode == ValidationMode.LEGACY) {
                return null;
            }
            return accommodationCategory.isEmpty()
                    ? null
                    : "No ready room is available for " + accommodationCategory
                    + ". Select another accommodation category or mark a room ready in Accommodation Management.";
        }

        Long accommodationId = mode == ValidationMode.LEGACY
                ? accommodationDao.findAnyIdByCategoryAndName(accommodationCategory, room)
                : accommodationDao.findReadyIdByCategoryAndName(accommodationCategory, room);
        if (accommodationId != null) {
            return null;
        }

        if (mode == ValidationMode.LEGACY) {
            return "Room not found in DB: Accommodation Category '" + accommodationCategory
                    + "', Room '" + room + "'";
        }
        return "No ready room is available for " + accommodationCategory
                + ". Select another accommodation category or mark a room ready in Accommodation Management.";
    }

    private static String missingFieldsMessage(List<String> missingFields) {
        if (missingFields.size() >= 8) {
            return """
                    Missing required information
                    Please complete the required Guest Details, Request Details, Approval Details, and Stay Details before saving.
                    """;
        }
        return "Missing required information\nComplete: " + String.join(", ", missingFields) + ".";
    }

    private static String legacyRemarks(String remarks) {
        String text = text(remarks);
        if (text.isEmpty() || "N/A".equalsIgnoreCase(text)) {
            return LEGACY_REMARK;
        }
        if (text.toLowerCase().contains(LEGACY_REMARK.toLowerCase())) {
            return text;
        }
        return text + "\n" + LEGACY_REMARK;
    }

    private static void addIfBlank(List<String> missingFields, String fieldName, String value) {
        if (isBlank(value)) {
            missingFields.add(fieldName);
        }
    }

    private static boolean containsField(List<String> fields, String fieldName) {
        for (String field : fields) {
            if (field.equalsIgnoreCase(fieldName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRoomPlaceholder(String value) {
        return ALL_ROOMS_OCCUPIED.equals(value) || NO_ROOMS_AVAILABLE.equals(value);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private enum ValidationMode {
        STANDARD,
        LEGACY
    }

    public record ValidationResult(
            List<String> missingFields,
            List<String> fieldIssues,
            String dateIssue,
            String roomIssue
    ) {
    }
}
