package com.kgm.service;

import com.kgm.dao.AccommodationCategoryDao;
import com.kgm.dao.AccommodationDao;
import com.kgm.dao.GuestDao;
import com.kgm.model.Guest;
import com.kgm.ui.panel.AccommodationRecord;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final AccommodationCategoryDao accommodationCategoryDao = new AccommodationCategoryDao();
    private Map<String, String> accommodationCategoryLookup;
    private Map<String, ImportAccommodation> accommodationRoomLookup;
    private Set<String> accommodationRoomCategoryKeys;
    private List<AccommodationRecord> importAccommodationRecords;

    public ImportResult importGuests(File file) throws IOException {
        List<String> skippedRows = new ArrayList<>();
        int imported = 0;
        accommodationCategoryLookup = null;
        accommodationRoomLookup = null;
        accommodationRoomCategoryKeys = null;
        importAccommodationRecords = null;

        try (Workbook workbook = openWorkbook(file)) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("Excel file has no sheet to import.");
            }

            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Map<String, Integer> headers = readHeaders(sheet, formatter, evaluator);
            validateHeaders(headers);

            boolean hasGuestRows = false;
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isBlankRow(row, formatter, evaluator)) {
                    continue;
                }
                hasGuestRows = true;

                try {
                    Guest guest = guestFromRow(row, headers, formatter, evaluator);
                    String accommodationCategory = resolveAccommodationCategory(
                            guest.getAccommodation(),
                            guest.getRoomName()
                    );
                    guest.setAccommodation(accommodationCategory);
                    validateAccommodation(guest.getAccommodation(), guest.getRoomName());
                    if (guestDao.existsByNameOnArrivalDate(guest.getGuestName(), guest.getArrivalAt())) {
                        throw new RowImportException("Guest name already exists for this arrival date: "
                                + guest.getGuestName());
                    }
                    guestDao.save(guest);
                    markAccommodationUsed(guest.getAccommodation(), guest.getRoomName());
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
        Path target = file.toPath();
        Path parent = target.toAbsolutePath().getParent();
        Path temporaryFile = parent == null
                ? Files.createTempFile("guest_import_sample_", ".xlsx")
                : Files.createTempFile(parent, "guest_import_sample_", ".xlsx");
        try {
            try (Workbook workbook = new XSSFWorkbook();
                 FileOutputStream output = new FileOutputStream(temporaryFile.toFile())) {
                List<SampleAccommodation> accommodations = sampleAccommodations();
                List<String> accommodationCategories = sampleAccommodationCategories();
                List<String> guestCategories = sampleGuestCategories();

                Sheet sheet = workbook.createSheet("Guest Import");
                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerFont.setColor(IndexedColors.WHITE.getIndex());
                headerStyle.setFont(headerFont);
                headerStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                headerStyle.setLocked(false);

                CellStyle editableStyle = workbook.createCellStyle();
                editableStyle.setLocked(false);
                editableStyle.setWrapText(true);
                unlockColumns(sheet, editableStyle, TEMPLATE_HEADERS.size());

                Row headerRow = sheet.createRow(0);
                for (int index = 0; index < TEMPLATE_HEADERS.size(); index++) {
                    Cell cell = headerRow.createCell(index);
                    cell.setCellValue(TEMPLATE_HEADERS.get(index));
                    cell.setCellStyle(headerStyle);
                }

                List<String[]> rows = sampleRows(accommodations, guestCategories);
                for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                    Row row = sheet.createRow(rowIndex + 1);
                    String[] values = rows.get(rowIndex);
                    for (int cellIndex = 0; cellIndex < values.length; cellIndex++) {
                        textCell(row, cellIndex, values[cellIndex], editableStyle);
                    }
                }

                for (int index = 0; index < TEMPLATE_HEADERS.size(); index++) {
                    sheet.autoSizeColumn(index);
                }

                writeValidValuesSheet(workbook, headerStyle, editableStyle, accommodationCategories, accommodations, guestCategories);
                workbook.write(output);
                output.flush();
            }
            makeEditableFile(target);
            Files.move(temporaryFile, target, StandardCopyOption.REPLACE_EXISTING);
            makeEditableFile(target);
            validateGeneratedWorkbook(target);
        } catch (SQLException exception) {
            throw new IOException("Could not create the sample Excel file from current accommodation and guest category data.", exception);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    public static String importGuideMessage() {
        return """
                Guest import sample columns:
                %s

                Current accommodation and guest category values:
                Download the sample file and review the Valid Values sheet. It is generated from the current database and includes active accommodation categories, all active rooms, room status, available beds, and all active guest categories.

                This import is only for guest data. It checks existing accommodation categories and rooms from DB; it does not create, edit, or rename accommodation records.
                """.formatted(templateHeaderLine());
    }

    private static List<String[]> sampleRows(List<SampleAccommodation> accommodations, List<String> guestCategories) {
        List<String> sampleGuestCategories = guestCategories.isEmpty() ? fallbackGuestCategories() : guestCategories;
        List<String[]> rows = new ArrayList<>();
        int rowIndex = 0;
        // Include every configured room/category scenario so the sample stays current with the database.
        for (SampleAccommodation accommodation : accommodations) {
            for (String guestCategory : sampleGuestCategories) {
                LocalDateTime arrival = LocalDate.now().plusDays(rowIndex + 1L).atTime(9 + rowIndex % 8, 0);
                LocalDateTime departure = arrival.plusDays(1);
                rows.add(new String[]{
                        sampleGuestName(rowIndex),
                        sampleCnic(rowIndex),
                        "Pakistani",
                        guestCategory,
                        "Sample address " + (rowIndex + 1),
                        "Sample Requester",
                        sampleDepartment(rowIndex),
                        "Sample Approver",
                        "Admin Office",
                        arrival.format(DATE_TIME_FORMATS.get(0)),
                        departure.format(DATE_TIME_FORMATS.get(0)),
                        accommodation.category(),
                        accommodation.room(),
                        sampleRemark(accommodation)
                });
                rowIndex++;
            }
        }
        return rows;
    }

    private static List<SampleAccommodation> sampleAccommodations() throws SQLException {
        List<SampleAccommodation> values = new ArrayList<>();
        for (AccommodationRecord record : new AccommodationDao().findAll()) {
            values.add(new SampleAccommodation(
                    record.getCategory(),
                    record.getName(),
                    record.getStatus(),
                    record.getCapacity(),
                    record.getAvailableSeats()
                ));
        }
        return values;
    }

    private static List<String> sampleAccommodationCategories() throws SQLException {
        return new AccommodationCategoryDao().findActiveNames();
    }

    private static List<String> sampleGuestCategories() throws SQLException {
        Set<String> categories = new LinkedHashSet<>();
        categories.addAll(fallbackGuestCategories());
        categories.addAll(new GuestDao().findActiveGuestCategoryNames());
        return new ArrayList<>(categories);
    }

    private static List<String> fallbackGuestCategories() {
        return List.of("Family", "Non-Family");
    }

    private static void writeValidValuesSheet(
            Workbook workbook,
            CellStyle headerStyle,
            CellStyle editableStyle,
            List<String> accommodationCategories,
            List<SampleAccommodation> accommodations,
            List<String> guestCategories
    ) {
        Sheet values = workbook.createSheet("Valid Values");
        String[] headers = {
                "Guest Category",
                "Accommodation Category",
                "Room",
                "Room Status",
                "Capacity",
                "Available Beds",
                "Importable"
        };
        unlockColumns(values, editableStyle, headers.length);
        Row header = values.createRow(0);
        for (int index = 0; index < headers.length; index++) {
            Cell cell = header.createCell(index);
            cell.setCellValue(headers[index]);
            cell.setCellStyle(headerStyle);
        }

        Set<String> coveredCategories = new LinkedHashSet<>();
        int rowIndex = 1;
        int guestCategoryIndex = 0;
        for (SampleAccommodation accommodation : accommodations) {
            coveredCategories.add(accommodation.category());
            Row row = values.createRow(rowIndex++);
            if (guestCategoryIndex < guestCategories.size()) {
                textCell(row, 0, guestCategories.get(guestCategoryIndex++), editableStyle);
            }
            writeAccommodationValueRow(row, accommodation, editableStyle);
        }

        for (String category : accommodationCategories) {
            if (coveredCategories.contains(category)) {
                continue;
            }
            Row row = values.createRow(rowIndex++);
            if (guestCategoryIndex < guestCategories.size()) {
                textCell(row, 0, guestCategories.get(guestCategoryIndex++), editableStyle);
            }
            textCell(row, 1, category, editableStyle);
            textCell(row, 2, "No active rooms configured", editableStyle);
            textCell(row, 3, "Not importable", editableStyle);
            textCell(row, 6, "No", editableStyle);
        }

        while (guestCategoryIndex < guestCategories.size()) {
            Row row = values.createRow(rowIndex++);
            textCell(row, 0, guestCategories.get(guestCategoryIndex++), editableStyle);
        }

        for (int index = 0; index < headers.length; index++) {
            values.autoSizeColumn(index);
        }
    }

    private static void writeAccommodationValueRow(
            Row row,
            SampleAccommodation accommodation,
            CellStyle editableStyle
    ) {
        textCell(row, 1, accommodation.category(), editableStyle);
        textCell(row, 2, accommodation.room(), editableStyle);
        textCell(row, 3, accommodation.status(), editableStyle);
        numberCell(row, 4, accommodation.capacity(), editableStyle);
        numberCell(row, 5, accommodation.availableBeds(), editableStyle);
        textCell(row, 6, "Ready for Assignment".equalsIgnoreCase(accommodation.status()) ? "Yes" : "No", editableStyle);
    }

    private static void textCell(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private static void numberCell(Row row, int index, int value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void unlockColumns(Sheet sheet, CellStyle editableStyle, int columns) {
        for (int index = 0; index < columns; index++) {
            sheet.setDefaultColumnStyle(index, editableStyle);
        }
    }

    private static void makeEditableFile(Path file) {
        try {
            if (Files.exists(file)) {
                file.toFile().setWritable(true, false);
                Files.setAttribute(file, "dos:readonly", false);
            }
        } catch (Exception ignored) {
        }
    }

    private static void validateGeneratedWorkbook(Path file) throws IOException {
        try (Workbook ignored = WorkbookFactory.create(file.toFile())) {
            // Opening the generated workbook here catches incomplete or corrupted files before users open Excel.
        }
    }

    private static String sampleRemark(SampleAccommodation accommodation) {
        String importHint = "Ready for Assignment".equalsIgnoreCase(accommodation.status())
                ? "Import-ready room."
                : "Reference row: this room may not import until it is ready.";
        return "Sample scenario. Status: " + accommodation.status()
                + ", available beds: " + accommodation.availableBeds()
                + ". " + importHint;
    }

    private static String sampleGuestName(int index) {
        String[] names = {
                "Ali Khan", "Sara Ahmed", "Bilal Hussain", "Mariam Iqbal", "Omer Farooq",
                "Ayesha Noor", "Hassan Raza", "Zain Malik", "Nida Aslam", "Usman Ali"
        };
        return names[index % names.length] + " " + (index + 1);
    }

    private static String sampleCnic(int index) {
        return String.format("35202%08d", index + 1);
    }

    private static String sampleDepartment(int index) {
        String[] departments = {
            "HR", "Admin", "Finance", "Spinning", "Power House", "IT"

        };
        return departments[index % departments.length];
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

        List<String> issues = rowIssues(
                row,
                headers,
                formatter,
                evaluator,
                cnic,
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

    private List<String> rowIssues(
            Row row,
            Map<String, Integer> headers,
            DataFormatter formatter,
            FormulaEvaluator evaluator,
            String cnic,
            Date arrivalAt,
            Date departureAt
    ) {
        List<String> issues = new ArrayList<>();
        List<String> missing = missingRequiredFields(row, headers, formatter, evaluator);
        if (!missing.isEmpty()) {
            issues.add("Missing required fields: " + String.join(", ", missing) + ".");
        }

        if (!missing.contains(CNIC) && !cnic.matches("\\d{13}")) {
            issues.add("CNIC must contain exactly 13 digits.");
        }
        if (!missing.contains(ARRIVAL_DATE_TIME) && arrivalAt == null) {
            issues.add(ARRIVAL_DATE_TIME + " must be a valid date/time.");
        }
        if (!missing.contains(DEPARTURE_DATE_TIME) && departureAt == null) {
            issues.add(DEPARTURE_DATE_TIME + " must be a valid date/time.");
        }
        if (arrivalAt != null && departureAt != null && !departureAt.after(arrivalAt)) {
            issues.add("Departure Date Time must be after Arrival Date Time.");
        }
        return issues;
    }

    private List<String> missingRequiredFields(
            Row row,
            Map<String, Integer> headers,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) {
        List<String> missing = new ArrayList<>();
        for (String header : REQUIRED_HEADERS) {
            if (optionalText(row, headers, header, formatter, evaluator).isEmpty()) {
                missing.add(header);
            }
        }
        return missing;
    }

    private void validateAccommodation(String accommodationCategory, String room) throws RowImportException, SQLException {
        Map<String, ImportAccommodation> rooms = accommodationRoomLookup();
        if (accommodationRoomCategoryKeys == null
                || !accommodationRoomCategoryKeys.contains(normalizeLookupValue(accommodationCategory))) {
            throw new RowImportException("Accommodation Category not found in DB: "
                    + accommodationRoomText(accommodationCategory, room));
        }
        ImportAccommodation accommodation = rooms.get(accommodationRoomKey(accommodationCategory, room));
        if (accommodation == null) {
            throw new RowImportException("Room not found in DB: "
                    + accommodationRoomText(accommodationCategory, room));
        }
        if (!accommodation.readyForAssignment()) {
            throw new RowImportException("Room is not ready for assignment: "
                    + accommodationRoomText(accommodationCategory, room));
        }
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

    private Map<String, ImportAccommodation> accommodationRoomLookup() throws SQLException {
        if (accommodationRoomLookup != null) {
            return accommodationRoomLookup;
        }

        Map<String, ImportAccommodation> lookup = new LinkedHashMap<>();
        Set<String> categories = new LinkedHashSet<>();
        for (AccommodationRecord record : importAccommodationRecords()) {
            ImportAccommodation accommodation = new ImportAccommodation(
                    record.getCategory(),
                    record.getName(),
                    "Ready for Assignment".equalsIgnoreCase(record.getStatus())
            );
            lookup.put(accommodationRoomKey(record.getCategory(), record.getName()), accommodation);
            categories.add(normalizeLookupValue(record.getCategory()));
        }
        accommodationRoomLookup = lookup;
        accommodationRoomCategoryKeys = categories;
        return accommodationRoomLookup;
    }

    private void markAccommodationUsed(String category, String room) {
        if (accommodationRoomLookup == null) {
            return;
        }
        String key = accommodationRoomKey(category, room);
        ImportAccommodation accommodation = accommodationRoomLookup.get(key);
        if (accommodation != null) {
            accommodationRoomLookup.put(key, new ImportAccommodation(
                    accommodation.category(),
                    accommodation.room(),
                    false
            ));
        }
    }

    private String accommodationRoomKey(String category, String room) {
        return normalizeLookupValue(category) + "|" + normalizeLookupValue(room);
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

    private void validateHeaders(Map<String, Integer> headers) {
        List<String> missing = new ArrayList<>();
        for (String header : REQUIRED_HEADERS) {
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

    private Cell rowCell(Row row, Map<String, Integer> headers, String header) {
        Integer index = headers.get(header);
        return row == null || index == null ? null : row.getCell(index);
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

    private record SampleAccommodation(String category, String room, String status, int capacity, int availableBeds) {
    }

    private record ImportAccommodation(String category, String room, boolean readyForAssignment) {
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
