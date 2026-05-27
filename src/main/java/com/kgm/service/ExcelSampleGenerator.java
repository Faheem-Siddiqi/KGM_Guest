package com.kgm.service;

import com.kgm.dao.AccommodationCategoryDao;
import com.kgm.dao.AccommodationDao;
import com.kgm.dao.GuestDao;
import com.kgm.ui.panel.AccommodationRecord;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates sample Excel templates for guest import.
 * This class provides fallback data when database connection is unavailable,
 * ensuring users can always get a sample template with correct headers.
 */
public class ExcelSampleGenerator {

    private static final String GUEST_NAME = "Guest Name";
    private static final String CNIC = "CNIC";
    private static final String NATIONALITY = "Nationality";
    private static final String GUEST_CATEGORY = "Guest Category";
    private static final String COMPANY_NAME = "Company Name";
    private static final String VISIT_TYPE = "Visit Type";
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
    private static final String[] VISIT_TYPES = {"Official Visit", "Personal Visit"};
    private static final int TEMPLATE_DROPDOWN_LAST_ROW = 1000;

    private static final List<String> TEMPLATE_HEADERS = List.of(
            GUEST_NAME,
            CNIC,
            NATIONALITY,
            GUEST_CATEGORY,
            ADDRESS,
            COMPANY_NAME,
            VISIT_TYPE,
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

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private ExcelSampleGenerator() {
        // Utility class, prevent instantiation
    }

    /**
     * Returns the template headers as a comma-separated line.
     */
    public static String templateHeaderLine() {
        return String.join(", ", TEMPLATE_HEADERS);
    }

    /**
     * Returns the list of template headers.
     */
    public static List<String> templateHeaders() {
        return TEMPLATE_HEADERS;
    }

    /**
     * Returns an import guide message explaining the template format.
     */
    public static String importGuideMessage() {
        return """
                Guest import sample columns:
                %s

                Current accommodation and guest category values:
                Download the sample file and review the Valid Values sheet. It is generated from the current database and includes active accommodation categories, all active rooms, room status, available beds, and all active guest categories.

                Visit Type is required and must be Official Visit or Personal Visit. Company Name is required.

                This import is only for guest data. It checks existing accommodation categories and rooms from DB; it does not create, edit, or rename accommodation records.
                """.formatted(templateHeaderLine());
    }

    /**
     * Writes a sample Excel workbook to the specified file.
     * If the database is unavailable, it uses fallback sample data to ensure
     * the template is always generated successfully.
     *
     * @param file the file to write the sample workbook to
     * @throws IOException if the file cannot be written
     */
    public static void writeSampleWorkbook(File file) throws IOException {
        Path target = file.toPath();
        Path parent = target.toAbsolutePath().getParent();
        Path temporaryFile = parent == null
                ? Files.createTempFile("guest_import_sample_", ".xlsx")
                : Files.createTempFile(parent, "guest_import_sample_", ".xlsx");
        try {
            List<SampleAccommodation> accommodations;
            List<String> accommodationCategories;
            List<String> guestCategories;

            try {
                accommodations = fetchAccommodationsFromDb();
                accommodationCategories = fetchAccommodationCategoriesFromDb();
                guestCategories = fetchGuestCategoriesFromDb();
            } catch (SQLException e) {
                // Use fallback data if database is unavailable
                accommodations = fallbackAccommodations();
                accommodationCategories = fallbackAccommodationCategories();
                guestCategories = fallbackGuestCategories();
            }

            try (Workbook workbook = new XSSFWorkbook();
                 FileOutputStream output = new FileOutputStream(temporaryFile.toFile())) {

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
                unlockColumns(sheet, editableStyle, TEMPLATE_HEADERS.size());

                // Plain style for data rows - no formatting
                CellStyle plainStyle = workbook.createCellStyle();
                plainStyle.setLocked(false);

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
                        textCell(row, cellIndex, values[cellIndex], plainStyle);
                    }
                }

                for (int index = 0; index < TEMPLATE_HEADERS.size(); index++) {
                    sheet.autoSizeColumn(index);
                }
                addVisitTypeDropdown(sheet);

                writeValidValuesSheet(workbook, headerStyle, editableStyle, accommodationCategories, accommodations, guestCategories);
                workbook.write(output);
                output.flush();
            }
            makeEditableFile(target);
            Files.move(temporaryFile, target, StandardCopyOption.REPLACE_EXISTING);
            makeEditableFile(target);
            validateGeneratedWorkbook(target);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private static List<SampleAccommodation> fetchAccommodationsFromDb() throws SQLException {
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

    private static List<String> fetchAccommodationCategoriesFromDb() throws SQLException {
        return new AccommodationCategoryDao().findActiveNames();
    }

    private static List<String> fetchGuestCategoriesFromDb() throws SQLException {
        Set<String> categories = new LinkedHashSet<>();
        categories.addAll(fallbackGuestCategories());
        categories.addAll(new GuestDao().findActiveGuestCategoryNames());
        return new ArrayList<>(categories);
    }

    private static List<SampleAccommodation> fallbackAccommodations() {
        return List.of(
                new SampleAccommodation("Guest Room", "Room-101", "Ready for Assignment", 4, 4),
                new SampleAccommodation("Guest Room", "Room-102", "Ready for Assignment", 4, 2),
                new SampleAccommodation("VIP Suite", "Suite-A", "Ready for Assignment", 2, 2),
                new SampleAccommodation("Guest Room", "Room-103", "Under Maintenance", 4, 0)
        );
    }

    private static List<String> fallbackAccommodationCategories() {
        return List.of("Guest Room", "VIP Suite", "Standard Room");
    }

    private static List<String> fallbackGuestCategories() {
        return List.of("Family", "Non-Family");
    }

    private static List<String[]> sampleRows(List<SampleAccommodation> accommodations, List<String> guestCategories) {
        List<String> sampleGuestCategories = guestCategories.isEmpty() ? fallbackGuestCategories() : guestCategories;
        List<String[]> rows = new ArrayList<>();
        int rowIndex = 0;
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
                        sampleCompany(rowIndex),
                        rowIndex % 2 == 0 ? "Official Visit" : "Personal Visit",
                        "Sample Requester",
                        sampleDepartment(rowIndex),
                        "Sample Approver",
                        "Admin Office",
                        arrival.format(DATE_TIME_FORMAT),
                        departure.format(DATE_TIME_FORMAT),
                        accommodation.category(),
                        accommodation.room(),
                        sampleRemark(accommodation)
                });
                rowIndex++;
            }
        }
        return rows;
    }

    private static final String[] VALID_DEPARTMENTS = {
        "HR", "Admin", "Finance", "Spinning", "Power House", "IT", "Security", "Others (speficy)"
    };

    private static void writeValidValuesSheet(
            Workbook workbook,
            CellStyle headerStyle,
            CellStyle editableStyle,
            List<String> accommodationCategories,
            List<SampleAccommodation> accommodations,
            List<String> guestCategories
    ) {
        Sheet values = workbook.createSheet("Valid Values");
        
        // Create a plain style for data rows (no formatting)
        CellStyle plainStyle = workbook.createCellStyle();
        plainStyle.setLocked(false);
        
        String[] headers = {
                "Guest Category",
                "Visit Type",
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
        int visitTypeIndex = 0;
        for (SampleAccommodation accommodation : accommodations) {
            coveredCategories.add(accommodation.category());
            Row row = values.createRow(rowIndex++);
            if (guestCategoryIndex < guestCategories.size()) {
                textCell(row, 0, guestCategories.get(guestCategoryIndex++), plainStyle);
            }
            visitTypeIndex = writeNextVisitType(row, visitTypeIndex, plainStyle);
            writeAccommodationValueRow(row, accommodation, plainStyle);
        }

        for (String category : accommodationCategories) {
            if (coveredCategories.contains(category)) {
                continue;
            }
            Row row = values.createRow(rowIndex++);
            if (guestCategoryIndex < guestCategories.size()) {
                textCell(row, 0, guestCategories.get(guestCategoryIndex++), plainStyle);
            }
            visitTypeIndex = writeNextVisitType(row, visitTypeIndex, plainStyle);
            textCell(row, 2, category, plainStyle);
            textCell(row, 3, "No active rooms configured", plainStyle);
            textCell(row, 4, "Not importable", plainStyle);
            textCell(row, 7, "No", plainStyle);
        }

        while (guestCategoryIndex < guestCategories.size()) {
            Row row = values.createRow(rowIndex++);
            textCell(row, 0, guestCategories.get(guestCategoryIndex++), plainStyle);
            visitTypeIndex = writeNextVisitType(row, visitTypeIndex, plainStyle);
        }

        while (visitTypeIndex < VISIT_TYPES.length) {
            Row row = values.createRow(rowIndex++);
            visitTypeIndex = writeNextVisitType(row, visitTypeIndex, plainStyle);
        }

        // Add blank row for separation
        values.createRow(rowIndex++);
        
        // Add Date Format Instructions
        Row dateNoteHeader = values.createRow(rowIndex++);
        Cell dateNoteHeaderCell = dateNoteHeader.createCell(0);
        dateNoteHeaderCell.setCellValue("Date Format Instructions:");
        dateNoteHeaderCell.setCellStyle(headerStyle);
        
        Row dateNote1 = values.createRow(rowIndex++);
        textCell(dateNote1, 0, "For Arrival Date Time and Departure Date Time columns:", plainStyle);
        
        Row dateNote2 = values.createRow(rowIndex++);
        textCell(dateNote2, 0, "1. Select the cells containing dates", plainStyle);
        textCell(dateNote2, 1, "Format: yyyy-MM-dd HH:mm (e.g., 2024-01-15 14:30)", plainStyle);
        
        Row dateNote3 = values.createRow(rowIndex++);
        textCell(dateNote3, 0, "2. Press Ctrl+1 to open Format Cells dialog", plainStyle);
        
        Row dateNote4 = values.createRow(rowIndex++);
        textCell(dateNote4, 0, "3. Go to Number tab > Custom category", plainStyle);
        
        Row dateNote5 = values.createRow(rowIndex++);
        textCell(dateNote5, 0, "4. In Type field, enter: yyyy-MM-dd HH:mm", plainStyle);
        
        Row dateNote6 = values.createRow(rowIndex++);
        textCell(dateNote6, 0, "5. Click OK to apply the format", plainStyle);
        
        // Add blank row for separation
        values.createRow(rowIndex++);
        
        // Add Valid Departments Section
        Row deptHeader = values.createRow(rowIndex++);
        Cell deptHeaderCell = deptHeader.createCell(0);
        deptHeaderCell.setCellValue("Valid Departments (for Requested Department column):");
        deptHeaderCell.setCellStyle(headerStyle);
        
        for (String department : VALID_DEPARTMENTS) {
            Row deptRow = values.createRow(rowIndex++);
            textCell(deptRow, 0, department, plainStyle);
        }

        for (int index = 0; index < headers.length; index++) {
            values.autoSizeColumn(index);
        }
    }

    private static void addVisitTypeDropdown(Sheet sheet) {
        int visitTypeColumn = TEMPLATE_HEADERS.indexOf(VISIT_TYPE);
        if (visitTypeColumn < 0) {
            return;
        }

        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createExplicitListConstraint(VISIT_TYPES);
        CellRangeAddressList range = new CellRangeAddressList(
                1,
                TEMPLATE_DROPDOWN_LAST_ROW,
                visitTypeColumn,
                visitTypeColumn
        );
        DataValidation validation = helper.createValidation(constraint, range);
        validation.setShowErrorBox(true);
        validation.createErrorBox("Invalid Visit Type", "Choose Official Visit or Personal Visit.");
        validation.setShowPromptBox(true);
        validation.createPromptBox("Visit Type", "Choose Official Visit or Personal Visit.");
        sheet.addValidationData(validation);
    }

    private static int writeNextVisitType(Row row, int visitTypeIndex, CellStyle style) {
        if (visitTypeIndex < VISIT_TYPES.length) {
            textCell(row, 1, VISIT_TYPES[visitTypeIndex], style);
            return visitTypeIndex + 1;
        }
        return visitTypeIndex;
    }

    private static void writeAccommodationValueRow(
            Row row,
            SampleAccommodation accommodation,
            CellStyle editableStyle
    ) {
        textCell(row, 2, accommodation.category(), editableStyle);
        textCell(row, 3, accommodation.room(), editableStyle);
        textCell(row, 4, accommodation.status(), editableStyle);
        numberCell(row, 5, accommodation.capacity(), editableStyle);
        numberCell(row, 6, accommodation.availableBeds(), editableStyle);
        textCell(row, 7, "Ready for Assignment".equalsIgnoreCase(accommodation.status()) ? "Yes" : "No", editableStyle);
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

    private static String sampleCompany(int index) {
        String[] companies = {
                "Kohinoor Textile Mills", "Vendor Partner", "Consultant Group", "Family Visitor"
        };
        return companies[index % companies.length];
    }

    /**
     * Record representing a sample accommodation for template generation.
     */
    public record SampleAccommodation(String category, String room, String status, int capacity, int availableBeds) {
    }
}
