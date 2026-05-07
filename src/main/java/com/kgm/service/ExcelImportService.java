package com.kgm.service;

import com.kgm.dao.AccommodationDao;
import com.kgm.dao.GuestDao;
import com.kgm.model.Guest;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
import java.util.List;
import java.util.Map;

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

    private static final List<String> REQUIRED_HEADERS = List.of(
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
    private static final List<String> TEMPLATE_HEADERS = List.of(
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
            ROOM,
            REMARKS
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

    public ImportResult importGuests(File file) throws IOException {
        List<String> skippedRows = new ArrayList<>();
        int imported = 0;
        System.out.println("Starting Excel guest import: " + file.getAbsolutePath());
        System.out.println(importGuideMessage());

        try (Workbook workbook = openWorkbook(file)) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("Excel file has no sheet to import.");
            }

            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Map<String, Integer> headers = readHeaders(sheet, formatter, evaluator);
            validateHeaders(headers);

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isBlankRow(row, formatter, evaluator)) {
                    continue;
                }

                try {
                    Guest guest = guestFromRow(row, headers, formatter, evaluator);
                    validateAccommodation(guest.getAccommodation(), guest.getRoomName());
                    if (guestDao.existsByNameOnArrivalDate(guest.getGuestName(), guest.getArrivalAt())) {
                        throw new RowImportException("Guest name already exists for this arrival date: "
                                + guest.getGuestName());
                    }
                    guestDao.save(guest);
                    imported++;
                } catch (RowImportException exception) {
                    addSkippedRow(skippedRows, rowIndex + 1, exception.getMessage());
                } catch (SQLException exception) {
                    addSkippedRow(skippedRows, rowIndex + 1, friendlySqlMessage(exception));
                }
            }
        }

        System.out.println("Excel guest import completed. Imported guests: " + imported
                + ", skipped rows: " + skippedRows.size());
        return new ImportResult(imported, skippedRows);
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

    public static String templateHeaderLine() {
        return String.join(", ", TEMPLATE_HEADERS);
    }

    public static List<String> templateHeaders() {
        return TEMPLATE_HEADERS;
    }

    public static void writeSampleWorkbook(File file) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream output = new FileOutputStream(file)) {
            Sheet sheet = workbook.createSheet("Guest Import");
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            for (int index = 0; index < TEMPLATE_HEADERS.size(); index++) {
                Cell cell = headerRow.createCell(index);
                cell.setCellValue(TEMPLATE_HEADERS.get(index));
                cell.setCellStyle(headerStyle);
            }

            String[][] rows = sampleRows();
            for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                for (int cellIndex = 0; cellIndex < rows[rowIndex].length; cellIndex++) {
                    row.createCell(cellIndex).setCellValue(rows[rowIndex][cellIndex]);
                }
            }

            for (int index = 0; index < TEMPLATE_HEADERS.size(); index++) {
                sheet.autoSizeColumn(index);
            }
            workbook.write(output);
        }
    }

    public static String importGuideMessage() {
        return """
                Required Excel header row:
                %s

                2026 accommodation category values:
                - Guest-House: use Room-1 to Room-7
                - Guest-Rooms: use Room-1 to Room-10
                - Guest-Room is accepted in Excel and treated as Guest-Rooms

                This import is only for guest data. It checks existing accommodation categories and rooms from DB; it does not create, edit, or rename accommodation records.
                """.formatted(templateHeaderLine());
    }

    private static String[][] sampleRows() {
        return new String[][]{
                {
                        "Ali Khan", "3520212345671", "Pakistani", "Family",
                        "House 12 Lahore", "Ahmed Raza", "HR", "Manager Khan",
                        "Faheem Siddiqi", "2026-05-08 09:00", "2026-05-09 09:00",
                        "Guest-House", "Room-1", "N/A"
                },
                {
                        "Sara Ahmed", "3520212345672", "Pakistani", "Non-Family",
                        "Street 4 Karachi", "Ayesha Noor", "Admin", "Admin Head",
                        "Faheem Siddiqi", "2026-05-08 10:00", "2026-05-10 10:00",
                        "Guest-House", "Room-2", "N/A"
                },
                {
                        "Bilal Hussain", "3520212345673", "Pakistani", "Family",
                        "Model Town Lahore", "Usman Ali", "Finance", "Finance Head",
                        "Faheem Siddiqi", "2026-05-08 11:00", "2026-05-09 18:00",
                        "Guest-Rooms", "Room-1", "N/A"
                },
                {
                        "Mariam Iqbal", "3520212345674", "Pakistani", "Non-Family",
                        "Gulberg Lahore", "Hassan Raza", "Spinning", "Plant Head",
                        "Faheem Siddiqi", "2026-05-08 12:00", "2026-05-11 12:00",
                        "Guest-Rooms", "Room-2", "N/A"
                },
                {
                        "Omer Farooq", "3520212345675", "Pakistani", "Family",
                        "Satellite Town Rawalpindi", "Zain Malik", "Others",
                        "Operations Head", "Faheem Siddiqi", "2026-05-08 13:00",
                        "2026-05-09 13:00", "Guest-Room", "Room-3", "N/A"
                }
        };
    }

    private Guest guestFromRow(
            Row row,
            Map<String, Integer> headers,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) throws RowImportException {
        String guestName = requiredText(row, headers, GUEST_NAME, formatter, evaluator);
        String cnic = digitsOnly(requiredText(row, headers, CNIC, formatter, evaluator));
        String nationality = requiredText(row, headers, NATIONALITY, formatter, evaluator);
        String guestCategory = requiredText(row, headers, GUEST_CATEGORY, formatter, evaluator);
        String address = requiredText(row, headers, ADDRESS, formatter, evaluator);
        String requestedBy = requiredText(row, headers, REQUESTED_BY, formatter, evaluator);
        String requestedDepartment = requiredText(row, headers, REQUESTED_DEPARTMENT, formatter, evaluator);
        String approvedBy = requiredText(row, headers, APPROVED_BY, formatter, evaluator);
        String accommodatedBy = requiredText(row, headers, ACCOMMODATED_BY, formatter, evaluator);
        Date arrivalAt = requiredDate(row, headers, ARRIVAL_DATE_TIME, formatter, evaluator);
        Date departureAt = requiredDate(row, headers, DEPARTURE_DATE_TIME, formatter, evaluator);
        String accommodationCategory = accommodationCategoryValue(
                requiredText(row, headers, ACCOMMODATION_CATEGORY, formatter, evaluator)
        );
        String room = roomNameValue(requiredText(row, headers, ROOM, formatter, evaluator));
        String remarks = optionalText(row, headers, REMARKS, formatter, evaluator);

        if (!cnic.matches("\\d{13}")) {
            throw new RowImportException("CNIC must contain exactly 13 digits.");
        }
        if (!departureAt.after(arrivalAt)) {
            throw new RowImportException("Departure Date Time must be after Arrival Date Time.");
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

    private void validateAccommodation(String accommodationCategory, String room) throws RowImportException, SQLException {
        if (accommodationDao.findActiveNamesByCategory(accommodationCategory).isEmpty()) {
            throw new RowImportException("Accommodation Category not found in DB: " + accommodationCategory);
        }
        if (accommodationDao.findIdByCategoryAndName(accommodationCategory, room) == null) {
            throw new RowImportException("Room not found in DB for category '" + accommodationCategory + "': " + room);
        }
        if (accommodationDao.findReadyIdByCategoryAndName(accommodationCategory, room) == null) {
            throw new RowImportException("Room is not ready for assignment: " + accommodationCategory + " / " + room);
        }
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

    private void validateHeaders(Map<String, Integer> headers) {
        List<String> missing = new ArrayList<>();
        for (String header : REQUIRED_HEADERS) {
            if (!headers.containsKey(header)) {
                missing.add(header);
            }
        }
        if (!missing.isEmpty()) {
            throw new HeaderImportException("Missing required Excel headers: " + String.join(", ", missing)
                    + "\n\n" + importGuideMessage());
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

    private String requiredText(
            Row row,
            Map<String, Integer> headers,
            String header,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) throws RowImportException {
        String value = optionalText(row, headers, header, formatter, evaluator);
        if (value.isEmpty()) {
            throw new RowImportException(header + " is required.");
        }
        return value;
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

    private Date requiredDate(
            Row row,
            Map<String, Integer> headers,
            String header,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) throws RowImportException {
        Integer index = headers.get(header);
        if (index == null) {
            throw new RowImportException(header + " is required.");
        }
        Cell cell = row.getCell(index);
        Date date = dateValue(cell, formatter, evaluator);
        if (date == null) {
            throw new RowImportException(header + " is required and must be a date/time.");
        }
        return date;
    }

    private Date dateValue(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isValidExcelDate(cell.getNumericCellValue())) {
            return DateUtil.getJavaDate(cell.getNumericCellValue());
        }

        String value = cellText(cell, formatter, evaluator);
        if (value.isEmpty()) {
            return null;
        }
        for (DateTimeFormatter format : DATE_TIME_FORMATS) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(value, format);
                return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
            } catch (DateTimeParseException ignored) {
            }
        }
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                LocalDate date = LocalDate.parse(value, format);
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
        String text = value == null ? "" : value.trim();
        String key = text.toLowerCase().replaceAll("[^a-z0-9]", "");
        if ("guesthouse".equals(key)) {
            return "Guest-House";
        }
        if ("guestroom".equals(key) || "guestrooms".equals(key)) {
            return "Guest-Rooms";
        }
        return text;
    }

    private void addSkippedRow(List<String> skippedRows, int rowNumber, String reason) {
        String message = "Row " + rowNumber + ": " + reason;
        skippedRows.add(message);
        System.err.println("Excel import skipped - " + message);
    }

    private String friendlySqlMessage(SQLException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "Database error while importing row." : message;
    }

    public record ImportResult(int importedCount, List<String> skippedRows) {
    }

    public static class HeaderImportException extends IllegalArgumentException {
        private HeaderImportException(String message) {
            super(message);
        }
    }

    private static class RowImportException extends Exception {
        private RowImportException(String message) {
            super(message);
        }
    }
}
