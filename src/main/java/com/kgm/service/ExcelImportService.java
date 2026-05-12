package com.kgm.service;

import com.kgm.dao.AccommodationCategoryDao;
import com.kgm.dao.AccommodationDao;
import com.kgm.dao.GuestDao;
import com.kgm.model.Guest;
import com.kgm.ui.panel.AccommodationRecord;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service for importing guest data from Excel files.
 * For sample template generation, use {@link ExcelSampleGenerator}.
 */
public class ExcelImportService {
    private static final String GUEST_NAME = "Guest Name";
    private static final String CNIC = "CNIC";
    private static final String NATIONALITY = "Nationality";
    private static final String GUEST_CATEGORY = "Guest Category";
    private static final String ADDRESS = "Address";
    private static final String REQUESTED_BY = "Requested By";
    private static final String REQUESTED_DEPARTMENT = "Requested Department";
    private static final String APPROVED_BY = "Approved By";
    private static final String ACCOMMODATED_BY = "Accommodated By";
    private static final String ARRIVAL_DATE_TIME = "Arrival Date Time";
    private static final String DEPARTURE_DATE_TIME = "Departure Date Time";
    private static final String ACCOMMODATION_CATEGORY = "Accommodation Category";
    private static final String ROOM = "Room";
    private static final String REMARKS = "Remarks";
    private static final String ROOM_PREFIX = "Room-";
    private static final String ROOMS_PREFIX = "Rooms-";

    private static final List<String> STANDARD_REQUIRED_HEADERS = List.of(
            GUEST_NAME,
            CNIC,
            NATIONALITY,
            GUEST_CATEGORY,
            ADDRESS,
            REQUESTED_BY,
            REQUESTED_DEPARTMENT,
            APPROVED_BY,
            ACCOMMODATED_BY,
            ARRIVAL_DATE_TIME,
            DEPARTURE_DATE_TIME,
            ACCOMMODATION_CATEGORY,
            ROOM
    );
    private static final List<String> LEGACY_REQUIRED_HEADERS = List.of(
            ARRIVAL_DATE_TIME,
            DEPARTURE_DATE_TIME,
            ACCOMMODATION_CATEGORY,
            ROOM
    );
    private static final Map<String, String> HEADER_ALIASES = aliases();
    private static final List<DateTimeFormatter> DATE_TIME_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd H:mm"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy H:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy H:mm"),
            DateTimeFormatter.ofPattern("M/d/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("M/d/yyyy H:mm"),
            DateTimeFormatter.ofPattern("M/d/yy HH:mm"),
            DateTimeFormatter.ofPattern("M/d/yy H:mm")
    );
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yy")
    );

    private final GuestDao guestDao = new GuestDao();
    private final AccommodationDao accommodationDao = new AccommodationDao();
    private final AccommodationCategoryDao accommodationCategoryDao = new AccommodationCategoryDao();
    private final GuestValidationService guestValidationService = new GuestValidationService();
    private Map<String, String> accommodationCategoryLookup;
    private List<AccommodationRecord> importAccommodationRecords;

    public enum ImportType {
        STANDARD("Import New / Standard Data", GuestDao.SaveMode.STANDARD),
        LEGACY("Import Legacy / Historical Data", GuestDao.SaveMode.LEGACY);

        private final String label;
        private final GuestDao.SaveMode saveMode;

        ImportType(String label, GuestDao.SaveMode saveMode) {
            this.label = label;
            this.saveMode = saveMode;
        }

        public String label() {
            return label;
        }

        private GuestDao.SaveMode saveMode() {
            return saveMode;
        }
    }

    /**
     * Imports guests from an Excel file.
     *
     * @param file the Excel file to import
     * @return the result containing the number of imported guests and any skipped rows
     * @throws IOException if the file cannot be read
     */
    public ImportResult importGuests(File file) throws IOException {
        return importGuests(file, ImportType.STANDARD);
    }

    /**
     * Imports guests from an Excel file.
     *
     * @param file       the Excel file to import
     * @param importType the type of import to apply
     * @return the result containing the number of imported guests and any skipped rows
     * @throws IOException if the file cannot be read
     */
    public ImportResult importGuests(File file, ImportType importType) throws IOException {
        ImportType selectedImportType = importType == null ? ImportType.STANDARD : importType;
        List<String> skippedRows = new ArrayList<>();
        int imported = 0;
        accommodationCategoryLookup = null;
        importAccommodationRecords = null;

        try (Workbook workbook = openWorkbook(file)) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("Excel file has no sheet to import.");
            }

            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Map<String, Integer> headers = readHeaders(sheet, formatter, evaluator);
            validateHeaders(headers, selectedImportType);

            boolean hasGuestRows = false;
            Map<String, Integer> importedLegacyRowsByFingerprint = new LinkedHashMap<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isBlankRow(row, formatter, evaluator)) {
                    continue;
                }
                hasGuestRows = true;

                try {
                    Guest guest = guestFromRow(row, headers, formatter, evaluator);
                    if (!guest.getAccommodation().isBlank()) {
                        guest.setAccommodation(resolveAccommodationCategory(
                                guest.getAccommodation(),
                                guest.getRoomName()
                        ));
                    }
                    validateGuestForImport(guest, selectedImportType);
                    String legacyFingerprint = null;
                    if (selectedImportType == ImportType.LEGACY) {
                        legacyFingerprint = legacyFingerprint(guest);
                        Integer matchingRow = importedLegacyRowsByFingerprint.get(legacyFingerprint);
                        if (matchingRow != null) {
                            throw new RowImportException(
                                    "Row not added since it is exactly same to row: " + matchingRow
                            );
                        }
                    }
                    guestDao.save(guest, selectedImportType.saveMode());
                    if (legacyFingerprint != null) {
                        importedLegacyRowsByFingerprint.put(legacyFingerprint, rowIndex + 1);
                    }
                    imported++;
                } catch (RowImportException exception) {
                    addSkippedRow(skippedRows, rowIndex + 1, exception.getMessage());
                } catch (SQLException exception) {
                    addSkippedRow(skippedRows, rowIndex + 1, friendlySqlMessage(exception));
                }
            }
            if (!hasGuestRows) {
                throw new IllegalArgumentException("No guest rows found. Add guest records below the header row before importing.");
            }
        }

        return new ImportResult(imported, skippedRows);
    }

    private void validateGuestForImport(Guest guest, ImportType importType)
            throws SQLException, RowImportException {
        GuestValidationService.ValidationResult validationResult;
        if (importType == ImportType.LEGACY) {
            guestValidationService.prepareLegacyGuest(guest);
            validationResult = guestValidationService.validateLegacyImportGuest(guest);
        } else {
            validationResult = guestValidationService.validateStandardGuest(guest);
        }
        if (GuestValidationService.hasIssues(validationResult)) {
            throw new RowImportException(GuestValidationService.rowMessage(validationResult));
        }
        if (importType == ImportType.STANDARD
                && guestDao.existsByCnicOnArrivalDate(guest.getCnic(), guest.getArrivalAt())) {
            throw new RowImportException("Guest CNIC already exists for this arrival date: "
                    + guest.getCnic());
        }
    }

    private String legacyFingerprint(Guest guest) {
        StringBuilder fingerprint = new StringBuilder();
        appendFingerprintValue(fingerprint, guest.getGuestName());
        appendFingerprintValue(fingerprint, guest.getCnic());
        appendFingerprintValue(fingerprint, guest.getNationality());
        appendFingerprintValue(fingerprint, guest.getGuestCategory());
        appendFingerprintValue(fingerprint, guest.getAddress());
        appendFingerprintValue(fingerprint, guest.getRequestedBy());
        appendFingerprintValue(fingerprint, guest.getRequestedDepartment());
        appendFingerprintValue(fingerprint, guest.getApprovedBy());
        appendFingerprintValue(fingerprint, guest.getAccommodatedBy());
        appendFingerprintValue(fingerprint, guest.getArrivalAt());
        appendFingerprintValue(fingerprint, guest.getDepartureAt());
        appendFingerprintValue(fingerprint, guest.getAccommodation());
        appendFingerprintValue(fingerprint, guest.getRoomName());
        appendFingerprintValue(fingerprint, guest.getRemarks());
        return fingerprint.toString();
    }

    private void appendFingerprintValue(StringBuilder fingerprint, String value) {
        String text = value == null ? "" : value.trim();
        fingerprint.append(text.length()).append(':').append(text).append('|');
    }

    private void appendFingerprintValue(StringBuilder fingerprint, Date value) {
        fingerprint.append(value == null ? "" : value.getTime()).append('|');
    }

    private Workbook openWorkbook(File file) throws IOException {
        try {
            byte[] workbookBytes = Files.readAllBytes(file.toPath());
            return WorkbookFactory.create(new ByteArrayInputStream(workbookBytes));
        } catch (FileSystemException exception) {
            throw new IOException(
                    "Could not read the Excel file because it is open or locked by another process. "
                            + "Close it in Excel and try again: " + file.getName(),
                    exception
            );
        }
    }

    private Guest guestFromRow(
            Row row,
            Map<String, Integer> headers,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) throws RowImportException {
        String guestName = optionalText(row, headers, GUEST_NAME, formatter, evaluator);
        String cnic = cnicValue(rowCell(row, headers, CNIC), formatter, evaluator);
        String nationality = optionalText(row, headers, NATIONALITY, formatter, evaluator);
        String guestCategory = optionalText(row, headers, GUEST_CATEGORY, formatter, evaluator);
        String address = optionalText(row, headers, ADDRESS, formatter, evaluator);
        String requestedBy = optionalText(row, headers, REQUESTED_BY, formatter, evaluator);
        String requestedDepartment = optionalText(row, headers, REQUESTED_DEPARTMENT, formatter, evaluator);
        String approvedBy = optionalText(row, headers, APPROVED_BY, formatter, evaluator);
        String accommodatedBy = optionalText(row, headers, ACCOMMODATED_BY, formatter, evaluator);
        String accommodationCategory = accommodationCategoryValue(
                optionalText(row, headers, ACCOMMODATION_CATEGORY, formatter, evaluator)
        );
        String room = roomNameValue(optionalText(row, headers, ROOM, formatter, evaluator));
        String remarks = optionalText(row, headers, REMARKS, formatter, evaluator);
        Date arrivalAt = dateValue(rowCell(row, headers, ARRIVAL_DATE_TIME), formatter, evaluator);
        Date departureAt = dateValue(rowCell(row, headers, DEPARTURE_DATE_TIME), formatter, evaluator);

        List<String> issues = dateParseIssues(
                row,
                headers,
                formatter,
                evaluator,
                arrivalAt,
                departureAt
        );
        if (!issues.isEmpty()) {
            throw new RowImportException(String.join(" ", issues));
        }

        if (ROOM_PREFIX.equals(room)) {
            room = "";
        }

        Guest guest = new Guest();
        guest.setGuestName(guestName);
        guest.setCnic(cnic);
        guest.setNationality(nationality);
        guest.setGuestCategory(guestCategory);
        guest.setAddress(address);
        guest.setRequestedBy(requestedBy);
        guest.setRequestedDepartment(requestedDepartment);
        guest.setApprovedBy(approvedBy);
        guest.setAccommodatedBy(accommodatedBy);
        guest.setArrivalAt(arrivalAt);
        guest.setDepartureAt(departureAt);
        guest.setAccommodation(accommodationCategory);
        guest.setRoomName(room);
        guest.setRemarks(remarks.isEmpty() ? "N/A" : remarks);
        return guest;
    }

    private List<String> dateParseIssues(
            Row row,
            Map<String, Integer> headers,
            DataFormatter formatter,
            FormulaEvaluator evaluator,
            Date arrivalAt,
            Date departureAt
    ) {
        List<String> issues = new ArrayList<>();
        if (!optionalText(row, headers, ARRIVAL_DATE_TIME, formatter, evaluator).isEmpty() && arrivalAt == null) {
            issues.add(ARRIVAL_DATE_TIME + " must be a valid date/time.");
        }
        if (!optionalText(row, headers, DEPARTURE_DATE_TIME, formatter, evaluator).isEmpty() && departureAt == null) {
            issues.add(DEPARTURE_DATE_TIME + " must be a valid date/time.");
        }
        return issues;
    }

    private String resolveAccommodationCategory(String accommodationCategory, String room)
            throws SQLException, RowImportException {
        String text = accommodationCategoryValue(accommodationCategory);
        Map<String, String> lookup = accommodationCategoryLookup();
        for (String key : categoryLookupKeys(text)) {
            String resolved = lookup.get(key);
            if (resolved != null) {
                return resolved;
            }
        }
        throw new RowImportException("Accommodation Category not found in DB: "
                + accommodationRoomText(text, room)
                + ". Use a category from the sample file's Valid Values sheet.");
    }

    private String accommodationRoomText(String accommodationCategory, String room) {
        return "Accommodation Category '" + accommodationCategory + "', Room '" + room + "'";
    }

    private Map<String, String> accommodationCategoryLookup() throws SQLException {
        if (accommodationCategoryLookup != null) {
            return accommodationCategoryLookup;
        }

        Map<String, String> lookup = new LinkedHashMap<>();
        for (String category : accommodationCategoryDao.findActiveNames()) {
            lookup.putIfAbsent(normalizeLookupValue(category), category);
        }
        for (AccommodationRecord record : importAccommodationRecords()) {
            lookup.putIfAbsent(normalizeLookupValue(record.getCategory()), record.getCategory());
        }
        accommodationCategoryLookup = lookup;
        return accommodationCategoryLookup;
    }

    private List<AccommodationRecord> importAccommodationRecords() throws SQLException {
        if (importAccommodationRecords == null) {
            importAccommodationRecords = accommodationDao.findAll();
        }
        return importAccommodationRecords;
    }

    private Map<String, Integer> readHeaders(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator) {
        Row headerRow = sheet.getRow(0);
        Map<String, Integer> headers = new HashMap<>();
        if (headerRow == null) {
            return headers;
        }

        short lastCell = headerRow.getLastCellNum();
        for (int cellIndex = 0; cellIndex < lastCell; cellIndex++) {
            String headerText = cellText(headerRow.getCell(cellIndex), formatter, evaluator);
            String canonicalHeader = canonicalHeader(headerText);
            if (canonicalHeader != null && !headers.containsKey(canonicalHeader)) {
                headers.put(canonicalHeader, cellIndex);
            }
        }
        return headers;
    }

    private void validateHeaders(Map<String, Integer> headers, ImportType importType) {
        List<String> missing = new ArrayList<>();
        List<String> requiredHeaders = importType == ImportType.LEGACY
                ? LEGACY_REQUIRED_HEADERS
                : STANDARD_REQUIRED_HEADERS;
        for (String header : requiredHeaders) {
            if (!headers.containsKey(header)) {
                missing.add(header);
            }
        }
        if (!missing.isEmpty()) {
            throw new HeaderImportException("Header issue detected. Download the sample file for the correct header format.");
        }
    }

    private boolean isBlankRow(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (row == null) {
            return true;
        }
        short lastCell = row.getLastCellNum();
        for (int cellIndex = 0; cellIndex < lastCell; cellIndex++) {
            if (!cellText(row.getCell(cellIndex), formatter, evaluator).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String optionalText(
            Row row,
            Map<String, Integer> headers,
            String header,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) {
        Integer index = headers.get(header);
        if (index == null) {
            return "";
        }
        return cellText(row.getCell(index), formatter, evaluator).trim();
    }

    private Cell rowCell(Row row, Map<String, Integer> headers, String header) {
        Integer index = headers.get(header);
        return row == null || index == null ? null : row.getCell(index);
    }

    private Date dateValue(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isValidExcelDate(cell.getNumericCellValue())) {
            return DateUtil.getJavaDate(cell.getNumericCellValue());
        }
        if (cell.getCellType() == CellType.FORMULA && evaluator != null) {
            CellValue value = evaluator.evaluate(cell);
            if (value == null) {
                return null;
            }
            if (value.getCellType() == CellType.NUMERIC && DateUtil.isValidExcelDate(value.getNumberValue())) {
                return DateUtil.getJavaDate(value.getNumberValue());
            }
            if (value.getCellType() == CellType.STRING) {
                return dateTextValue(value.getStringValue());
            }
        }

        String value = cellText(cell, formatter, evaluator);
        return dateTextValue(value);
    }

    private Date dateTextValue(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return null;
        }
        for (DateTimeFormatter format : DATE_TIME_FORMATS) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(text, format);
                return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
            } catch (DateTimeParseException ignored) {
            }
        }
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                LocalDate date = LocalDate.parse(text, format);
                return Date.from(date.atTime(LocalTime.MIDNIGHT).atZone(ZoneId.systemDefault()).toInstant());
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private String cellText(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell, evaluator).trim();
    }

    private String cnicValue(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return numericIdentifier(cell.getNumericCellValue());
        }
        if (cell.getCellType() == CellType.FORMULA && evaluator != null) {
            CellValue value = evaluator.evaluate(cell);
            if (value == null) {
                return "";
            }
            if (value.getCellType() == CellType.NUMERIC) {
                return numericIdentifier(value.getNumberValue());
            }
            if (value.getCellType() == CellType.STRING) {
                return digitsOnly(value.getStringValue());
            }
        }
        return digitsOnly(cellText(cell, formatter, evaluator));
    }

    private String numericIdentifier(double value) {
        if (!Double.isFinite(value)) {
            return "";
        }
        return BigDecimal.valueOf(value)
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString()
                .replaceAll("\\D", "");
    }

    private String canonicalHeader(String header) {
        if (header == null || header.trim().isEmpty()) {
            return null;
        }
        return HEADER_ALIASES.get(normalizeHeader(header));
    }

    private static Map<String, String> aliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        addAliases(aliases, GUEST_NAME, "Guest Name", "Name", "Guest");
        addAliases(aliases, CNIC, "CNIC", "Guest CNIC", "NIC");
        addAliases(aliases, NATIONALITY, "Nationality", "Guest Nationality");
        addAliases(aliases, GUEST_CATEGORY, "Guest Category", "Category");
        addAliases(aliases, ADDRESS, "Address", "Guest Address");
        addAliases(aliases, REQUESTED_BY, "Requested By", "Request By");
        addAliases(aliases, REQUESTED_DEPARTMENT, "Requested Department", "Department");
        addAliases(aliases, APPROVED_BY, "Approved By");
        addAliases(aliases, ACCOMMODATED_BY, "Accommodated By");
        addAliases(aliases, ARRIVAL_DATE_TIME, "Arrival Date Time", "Arrival Date", "Arrival At");
        addAliases(aliases, DEPARTURE_DATE_TIME, "Departure Date Time", "Departure Date", "Departure At");
        addAliases(aliases, ACCOMMODATION_CATEGORY, "Accommodation Category", "Accommodation", "Accommodation Type");
        addAliases(aliases, ROOM, "Room", "Room Name");
        addAliases(aliases, REMARKS, "Remarks", "Remark", "Review");
        return aliases;
    }

    private static void addAliases(Map<String, String> aliases, String canonical, String... values) {
        for (String value : values) {
            aliases.put(normalizeHeader(value), canonical);
        }
    }

    private static String normalizeHeader(String value) {
        return value == null ? "" : value.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private static Set<String> categoryLookupKeys(String value) {
        Set<String> keys = new LinkedHashSet<>();
        String key = normalizeLookupValue(value);
        keys.add(key);
        if ("guestroom".equals(key)) {
            keys.add("guestrooms");
        } else if ("guestrooms".equals(key)) {
            keys.add("guestroom");
        }
        return keys;
    }

    private static String normalizeLookupValue(String value) {
        return value == null ? "" : value.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private String roomNameValue(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return ROOM_PREFIX;
        }
        if (text.regionMatches(true, 0, ROOM_PREFIX, 0, ROOM_PREFIX.length())) {
            return ROOM_PREFIX + text.substring(ROOM_PREFIX.length()).trim();
        }
        if (text.regionMatches(true, 0, ROOMS_PREFIX, 0, ROOMS_PREFIX.length())) {
            return ROOM_PREFIX + text.substring(ROOMS_PREFIX.length()).trim();
        }
        if (text.equalsIgnoreCase("Room")) {
            return ROOM_PREFIX;
        }
        if (text.equalsIgnoreCase("Rooms")) {
            return ROOM_PREFIX;
        }
        if (text.toLowerCase().startsWith("room ")) {
            return ROOM_PREFIX + text.substring("room ".length()).trim();
        }
        if (text.toLowerCase().startsWith("rooms ")) {
            return ROOM_PREFIX + text.substring("rooms ".length()).trim();
        }
        return ROOM_PREFIX + text;
    }

    private String accommodationCategoryValue(String value) {
        return value == null ? "" : value.trim();
    }

    private void addSkippedRow(List<String> skippedRows, int rowNumber, String reason) {
        String message = "Row " + rowNumber + ": " + reason;
        skippedRows.add(message);
    }

    private String friendlySqlMessage(SQLException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "Database error while importing row." : message;
    }

    /**
     * Result of an import operation.
     *
     * @param importedCount the number of successfully imported guests
     * @param skippedRows   the list of skipped row messages with reasons
     */
    public record ImportResult(int importedCount, List<String> skippedRows) {
    }

    /**
     * Exception thrown when there is a header validation issue.
     */
    public static class HeaderImportException extends IllegalArgumentException {
        private HeaderImportException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when a row fails validation during import.
     */
    private static class RowImportException extends Exception {
        private RowImportException(String message) {
            super(message);
        }
    }
}
