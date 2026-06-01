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
import org.apache.poi.ss.util.CellRangeAddress;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Generates sample Excel templates for guest import.
 * This class provides fallback data when database connection is unavailable,
 * ensuring users can always get a sample template with correct headers.
 */
public class ExcelSampleGenerator {

    private static final String GUEST_NAME = "Guest Name";
    private static final String CNIC = "CNIC / Passport";
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
    private static final int MIN_SAMPLE_ROWS = 4;
    private static final String IDENTIFIER_RULE_MESSAGE = "Pakistani/Pakistan guests: enter CNIC as exactly 13 digits with no dashes, for example 3520212345678. Other guests: enter passport using 4 to 30 letters/digits with at least one letter, for example PX1234567 or AB-123 456.";
    private static final String IDENTIFIER_IMPORT_MODE_MESSAGE = "New/standard import applies Add Guest rules: CNIC / Passport is required, overlapping stays for the same identifier are blocked, and capacity/room status are checked. Legacy/historical import allows blank or repeated old identifier values, for example 9999999999999, but any provided value must still be a valid CNIC or passport.";
    private static final String IDENTIFIER_INVALID_EXAMPLES_MESSAGE = "Do not enter CNIC with dashes, passport numbers with slashes or underscores, repeated separators such as AB--12345, or passport values with only numbers.";
    private static final String IDENTIFIER_STORAGE_RULE = "For new/standard import, CNIC / Passport matching ignores spaces, hyphens, and letter case. The stored identifier is compacted before saving.";
    private static final String STANDARD_REQUIRED_RULE = "Required values: Guest Name, CNIC / Passport, Nationality, Guest Category, Address, Company Name, Visit Type, Requested By, Requested Department, Approved By, Accommodated By, Arrival Date Time, Departure Date Time, Accommodation Category, and Room. Remarks is optional.";
    private static final String LEGACY_REQUIRED_RULE = "Required values: Company Name, Visit Type, Arrival Date Time, Departure Date Time, Accommodation Category, and Room. Other blank guest fields are stored as N/A for historical records.";
    private static final String STANDARD_STAY_RULE = "Blocks the import when the same CNIC / Passport already has any booking or stay overlapping the selected arrival/departure period. One guest can only be allotted one room at a time.";
    private static final String LEGACY_STAY_RULE = "Overlap, capacity, and room-status checks are intentionally bypassed for historical records.";
    private static final String DATE_RULE = "Arrival and Departure are required. In the sample sheet, type exactly yyyy-MM-dd HH:mm, for example 2026-05-30 10:00. The validation alert blocks other typed formats. Departure must be after Arrival during import.";
    private static final String VISIT_TYPE_RULE = "Visit Type must be exactly Official Visit or Personal Visit.";
    private static final String STANDARD_ROOM_RULE = "Accommodation Category and Room must match the current Valid Values list. New/standard import requires a ready room with available capacity for the selected dates.";
    private static final String LEGACY_ROOM_RULE = "Accommodation Category and Room must already exist in the database, but the room does not need to be ready and capacity is not checked.";
    private static final String LEGACY_DUPLICATE_RULE = "Legacy duplicate check against existing DB records uses Guest Name + exact Arrival Date Time + exact Departure Date Time + Guest Category. CNIC / Passport does not need to be unique for legacy imports.";
    private static final String LEGACY_WORKBOOK_DUPLICATE_RULE = "Within the same legacy workbook, an exact duplicate row is skipped when all guest, request, stay, room, CNIC / Passport, and remarks values match a row already imported from that workbook.";
    private static final String STANDARD_HEADER_RULE = "Use the sample headers. Old header names such as CNIC, CNIC/Passport, Guest CNIC, and Passport are still accepted for compatibility.";
    private static final String LEGACY_HEADER_RULE = "Legacy workbooks may use the same sample headers. Only the legacy required values are mandatory.";
    private static final String STANDARD_DEFAULT_RULE = "Remarks defaults to N/A when blank. Identifiers are compacted before saving after validation.";
    private static final String LEGACY_DEFAULT_RULE = "Blank optional guest fields are stored as N/A and remarks include Old record - validation bypassed.";
    private static final String ACCEPTED_DATE_FORMATS_RULE = "For compatibility, the importer can still parse old workbooks using yyyy-MM-dd HH:mm or H:mm, yyyy/MM/dd HH:mm or H:mm, dd-MM-yyyy HH:mm or H:mm, dd/MM/yyyy HH:mm or H:mm, M/d/yyyy HH:mm or H:mm, and M/d/yy HH:mm or H:mm. The sample template validation intentionally guides new entries to yyyy-MM-dd HH:mm only.";
    private static final String IMPORT_SCOPE_RULE = "The sample uses current DB accommodation categories, rooms, and guest categories. Import checks those records only; it does not create, edit, or rename accommodation records.";
    private static final String ROOM_NAME_RULE = "Use the room name from the Valid Values table. The importer can add the Room prefix when needed, but exact DB names are safest.";
    private static final String WORKBOOK_ROW_RULE = "The importer reads the first worksheet only. Row 1 must contain headers, guest records start on Row 2, blank rows are ignored, and rows with validation errors are skipped with a reason while other valid rows continue.";
    private static final int VALID_VALUES_LAST_COLUMN = 7;
    private static final int VALIDATION_SOURCE_FIRST_ROW = 1;
    private static final int VALIDATION_SOURCE_GUEST_CATEGORY_COLUMN = 9;
    private static final int VALIDATION_SOURCE_DEPARTMENT_COLUMN = 10;
    private static final int VALIDATION_SOURCE_ACCOMMODATION_CATEGORY_COLUMN = 11;
    private static final int VALIDATION_SOURCE_ROOM_MAP_CATEGORY_COLUMN = 12;
    private static final int VALIDATION_SOURCE_ROOM_MAP_NAME_COLUMN = 13;
    private static final int VALIDATION_SOURCE_ROOM_LIST_START_COLUMN = 14;
    private static final String EMPTY_ROOM_RANGE_NAME = "KGM_EMPTY_ROOM_LIST";

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

                CNIC / Passport is strict:
                - Pakistani/Pakistan guests use a 13-digit CNIC without dashes, for example 3520212345678.
                - Other nationalities use passport values with letters/digits and optional space/hyphen separators, for example PX1234567 or AB-123 456.
                - New/standard import applies Add Guest overlap, capacity, and room status rules.
                - Legacy/historical import may repeat old placeholder CNIC / Passport values such as 9999999999999, but any provided value must still match the CNIC/passport format.
                - Legacy duplicate check uses Guest Name + exact Arrival Date Time + exact Departure Date Time + Guest Category, plus an exact-row duplicate check inside the same workbook.

                Date values must be valid and Departure Date Time must be after Arrival Date Time. %s

                %s
                """.formatted(templateHeaderLine(), ACCEPTED_DATE_FORMATS_RULE, IMPORT_SCOPE_RULE);
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
                CellStyle dateTimeTextStyle = workbook.createCellStyle();
                dateTimeTextStyle.cloneStyleFrom(plainStyle);
                dateTimeTextStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));
                sheet.setDefaultColumnStyle(TEMPLATE_HEADERS.indexOf(ARRIVAL_DATE_TIME), dateTimeTextStyle);
                sheet.setDefaultColumnStyle(TEMPLATE_HEADERS.indexOf(DEPARTURE_DATE_TIME), dateTimeTextStyle);

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
                        textCell(row, cellIndex, values[cellIndex], importCellStyle(cellIndex, plainStyle, dateTimeTextStyle));
                    }
                }

                for (int index = 0; index < TEMPLATE_HEADERS.size(); index++) {
                    sheet.autoSizeColumn(index);
                }

                ValidationSourceRanges validationSources = writeValidValuesSheet(
                        workbook,
                        headerStyle,
                        editableStyle,
                        accommodationCategories,
                        accommodations,
                        guestCategories
                );
                addSampleDataValidations(sheet, validationSources);
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
        List<SampleAccommodation> sampleAccommodations = accommodations.isEmpty()
                ? fallbackAccommodations()
                : accommodations;
        List<String> sampleGuestCategories = guestCategories.isEmpty() ? fallbackGuestCategories() : guestCategories;
        List<String[]> rows = new ArrayList<>();
        int rowIndex = 0;
        for (SampleAccommodation accommodation : sampleAccommodations) {
            for (String guestCategory : sampleGuestCategories) {
                rows.add(sampleRow(rowIndex, accommodation, guestCategory));
                rowIndex++;
            }
        }
        while (rows.size() < MIN_SAMPLE_ROWS) {
            SampleAccommodation accommodation = sampleAccommodations.get(rowIndex % sampleAccommodations.size());
            String guestCategory = sampleGuestCategories.get(rowIndex % sampleGuestCategories.size());
            rows.add(sampleRow(rowIndex, accommodation, guestCategory));
            rowIndex++;
        }
        return rows;
    }

    private static String[] sampleRow(int rowIndex, SampleAccommodation accommodation, String guestCategory) {
        LocalDateTime arrival = LocalDate.now().plusDays(rowIndex + 1L).atTime(9 + rowIndex % 8, 0);
        LocalDateTime departure = arrival.plusDays(1);
        return new String[]{
                sampleGuestName(rowIndex),
                sampleIdentifier(rowIndex),
                sampleNationality(rowIndex),
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
        };
    }

    private static final String[] VALID_DEPARTMENTS = {
        "HR", "Admin", "Finance", "Spinning", "Power House", "IT", "Security", "Others (speficy)"
    };

    private static ValidationSourceRanges writeValidValuesSheet(
            Workbook workbook,
            CellStyle headerStyle,
            CellStyle editableStyle,
            List<String> accommodationCategories,
            List<SampleAccommodation> accommodations,
            List<String> guestCategories
    ) {
        Sheet values = workbook.createSheet("Valid Values");
        values.setDisplayGridlines(false);

        CellStyle plainStyle = workbook.createCellStyle();
        plainStyle.setLocked(false);
        CellStyle ruleStyle = workbook.createCellStyle();
        ruleStyle.cloneStyleFrom(plainStyle);
        ruleStyle.setWrapText(true);
        CellStyle ruleHeaderStyle = workbook.createCellStyle();
        ruleHeaderStyle.cloneStyleFrom(headerStyle);
        ruleHeaderStyle.setWrapText(true);
        CellStyle sectionStyle = workbook.createCellStyle();
        sectionStyle.cloneStyleFrom(headerStyle);
        sectionStyle.setWrapText(true);

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

        int rowIndex = 0;
        Row valuesTitle = values.createRow(rowIndex++);
        mergedTextCell(values, valuesTitle, 0, VALID_VALUES_LAST_COLUMN, "Current Valid Values From Database", sectionStyle);
        Row valuesNote = values.createRow(rowIndex++);
        mergedTextCell(values, valuesNote, 0, VALID_VALUES_LAST_COLUMN,
                "Start here. Use these values in the Guest Import sheet. Importable = Yes means the room is Ready for Assignment in the current database snapshot.",
                ruleStyle);

        int validValuesHeaderRow = rowIndex;
        Row header = values.createRow(rowIndex++);
        for (int index = 0; index < headers.length; index++) {
            Cell cell = header.createCell(index);
            cell.setCellValue(headers[index]);
            cell.setCellStyle(headerStyle);
        }

        Set<String> coveredCategories = new LinkedHashSet<>();
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

        if (rowIndex > validValuesHeaderRow + 1) {
            values.setAutoFilter(new CellRangeAddress(validValuesHeaderRow, rowIndex - 1, 0, headers.length - 1));
        }
        rowIndex = writeDepartmentsSection(values, rowIndex, sectionStyle, ruleStyle);
        rowIndex = writeGuideHeader(values, rowIndex, sectionStyle, ruleStyle);
        rowIndex = writeImportRulesSection(values, rowIndex, sectionStyle, ruleHeaderStyle, ruleStyle);
        rowIndex = writeIdentifierExamplesSection(values, rowIndex, sectionStyle, ruleHeaderStyle, ruleStyle);
        writeDateFormatSection(values, rowIndex, sectionStyle, ruleHeaderStyle, ruleStyle);
        ValidationSourceRanges validationSources = writeValidationSources(
                workbook,
                values,
                plainStyle,
                accommodationCategories,
                accommodations,
                guestCategories
        );
        setValidValuesColumnWidths(values);
        values.createFreezePane(0, validValuesHeaderRow + 1);
        return validationSources;
    }

    private static int writeGuideHeader(Sheet sheet, int rowIndex, CellStyle titleStyle, CellStyle noteStyle) {
        sheet.createRow(rowIndex++);
        Row title = sheet.createRow(rowIndex++);
        title.setHeightInPoints(24);
        mergedTextCell(sheet, title, 0, VALID_VALUES_LAST_COLUMN, "Import Guide and Rules", titleStyle);

        Row note = sheet.createRow(rowIndex++);
        note.setHeightInPoints(44);
        mergedTextCell(sheet, note, 0, VALID_VALUES_LAST_COLUMN,
                "After choosing values from the tables above, review these rules before importing. They explain what the app checks for new and legacy data.",
                noteStyle);
        return rowIndex;
    }

    private static int writeIdentifierExamplesSection(
            Sheet sheet,
            int rowIndex,
            CellStyle titleStyle,
            CellStyle headerStyle,
            CellStyle rowStyle
    ) {
        sheet.createRow(rowIndex++);

        Row title = sheet.createRow(rowIndex++);
        mergedTextCell(sheet, title, 0, VALID_VALUES_LAST_COLUMN, "CNIC / Passport Examples", titleStyle);

        Row header = sheet.createRow(rowIndex++);
        textCell(header, 0, "Guest Nationality", headerStyle);
        textCell(header, 1, "Valid Example", headerStyle);
        textCell(header, 2, "Invalid Examples", headerStyle);
        textCell(header, 3, "Rule", headerStyle);

        rowIndex = writeExampleRow(
                sheet,
                rowIndex,
                "Pakistan or Pakistani",
                "3520212345678",
                "35202-1234567-8, 352021234567, ABC123",
                "CNIC must be exactly 13 digits and must not contain dashes.",
                rowStyle
        );
        rowIndex = writeExampleRow(
                sheet,
                rowIndex,
                "Any other nationality",
                "PX1234567 or AB-123 456",
                "12345678, AB_12345, AB--12345, AB/12345",
                "Passport must be 4 to 30 letters/digits, include at least one letter, and only use spaces or hyphens as separators.",
                rowStyle
        );
        return writeExampleRow(
                sheet,
                rowIndex,
                "Legacy historical placeholder",
                "9999999999999",
                "Repeated value is allowed only in legacy mode.",
                "Legacy mode may repeat old CNIC / Passport values and does not use CNIC / Passport for uniqueness.",
                rowStyle
        );
    }

    private static int writeDateFormatSection(
            Sheet sheet,
            int rowIndex,
            CellStyle titleStyle,
            CellStyle headerStyle,
            CellStyle rowStyle
    ) {
        sheet.createRow(rowIndex++);

        Row title = sheet.createRow(rowIndex++);
        mergedTextCell(sheet, title, 0, VALID_VALUES_LAST_COLUMN, "Date and Time Rules", titleStyle);

        Row header = sheet.createRow(rowIndex++);
        textCell(header, 0, "Area", headerStyle);
        textCell(header, 1, "Rule", headerStyle);

        rowIndex = writeTwoColumnRow(sheet, rowIndex, "Required", DATE_RULE, rowStyle);
        rowIndex = writeTwoColumnRow(sheet, rowIndex, "Accepted formats", ACCEPTED_DATE_FORMATS_RULE, rowStyle);
        rowIndex = writeTwoColumnRow(
                sheet,
                rowIndex,
                "Excel tip",
                "For manual formatting, select the date cells, press Ctrl+1, choose Custom, and use yyyy-MM-dd HH:mm.",
                rowStyle
        );
        return writeTwoColumnRow(
                sheet,
                rowIndex,
                "Import behavior",
                "Old workbooks with date-only values can still import at 00:00. In this sample template, use yyyy-MM-dd HH:mm so Excel validation accepts the row.",
                rowStyle
        );
    }

    private static int writeDepartmentsSection(
            Sheet sheet,
            int rowIndex,
            CellStyle titleStyle,
            CellStyle rowStyle
    ) {
        sheet.createRow(rowIndex++);

        Row title = sheet.createRow(rowIndex++);
        mergedTextCell(sheet, title, 0, VALID_VALUES_LAST_COLUMN, "Requested Department Values", titleStyle);

        Row note = sheet.createRow(rowIndex++);
        mergedTextCell(sheet, note, 0, VALID_VALUES_LAST_COLUMN,
                "Requested Department is required for new/standard import. These are the values shown in the Add Guest screen.",
                rowStyle);

        for (int index = 0; index < VALID_DEPARTMENTS.length; index += 4) {
            Row row = sheet.createRow(rowIndex++);
            for (int column = 0; column < 4 && index + column < VALID_DEPARTMENTS.length; column++) {
                textCell(row, column, VALID_DEPARTMENTS[index + column], rowStyle);
            }
        }
        return rowIndex;
    }

    private static int writeExampleRow(
            Sheet sheet,
            int rowIndex,
            String nationality,
            String validExample,
            String invalidExamples,
            String rule,
            CellStyle style
    ) {
        Row row = sheet.createRow(rowIndex++);
        row.setHeightInPoints(48);
        textCell(row, 0, nationality, style);
        textCell(row, 1, validExample, style);
        textCell(row, 2, invalidExamples, style);
        textCell(row, 3, rule, style);
        return rowIndex;
    }

    private static int writeTwoColumnRow(Sheet sheet, int rowIndex, String label, String value, CellStyle style) {
        Row row = sheet.createRow(rowIndex++);
        row.setHeightInPoints(44);
        textCell(row, 0, label, style);
        mergedTextCell(sheet, row, 1, VALID_VALUES_LAST_COLUMN, value, style);
        return rowIndex;
    }

    private static void mergedTextCell(Sheet sheet, Row row, int firstColumn, int lastColumn, String value, CellStyle style) {
        textCell(row, firstColumn, value, style);
        for (int column = firstColumn + 1; column <= lastColumn; column++) {
            textCell(row, column, "", style);
        }
        sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), firstColumn, lastColumn));
    }

    private static void setValidValuesColumnWidths(Sheet sheet) {
        int[] widths = {30, 56, 56, 34, 24, 16, 18, 18};
        for (int index = 0; index < widths.length; index++) {
            sheet.setColumnWidth(index, widths[index] * 256);
        }
    }

    private static ValidationSourceRanges writeValidationSources(
            Workbook workbook,
            Sheet sheet,
            CellStyle style,
            List<String> accommodationCategories,
            List<SampleAccommodation> accommodations,
            List<String> guestCategories
    ) {
        List<String> guestCategoryValues = uniqueValues(guestCategories);
        if (guestCategoryValues.isEmpty()) {
            guestCategoryValues = fallbackGuestCategories();
        }

        List<String> accommodationCategoryValues = uniqueValues(accommodationCategories);
        for (SampleAccommodation accommodation : accommodations) {
            addUnique(accommodationCategoryValues, accommodation.category());
        }
        if (accommodationCategoryValues.isEmpty()) {
            accommodationCategoryValues = fallbackAccommodationCategories();
        }

        String guestCategoryFormula = writeSourceList(
                sheet,
                VALIDATION_SOURCE_GUEST_CATEGORY_COLUMN,
                "Guest Category List",
                guestCategoryValues,
                style
        );
        String departmentFormula = writeSourceList(
                sheet,
                VALIDATION_SOURCE_DEPARTMENT_COLUMN,
                "Requested Department List",
                List.of(VALID_DEPARTMENTS),
                style
        );
        String accommodationCategoryFormula = writeSourceList(
                sheet,
                VALIDATION_SOURCE_ACCOMMODATION_CATEGORY_COLUMN,
                "Accommodation Category List",
                accommodationCategoryValues,
                style
        );
        String roomMapFormula = writeRoomValidationSources(
                workbook,
                sheet,
                style,
                accommodationCategoryValues,
                accommodations
        );
        hideValidationSourceColumns(sheet);
        return new ValidationSourceRanges(
                guestCategoryFormula,
                departmentFormula,
                accommodationCategoryFormula,
                roomMapFormula
        );
    }

    private static String writeRoomValidationSources(
            Workbook workbook,
            Sheet sheet,
            CellStyle style,
            List<String> accommodationCategories,
            List<SampleAccommodation> accommodations
    ) {
        Map<String, List<String>> roomsByCategory = new LinkedHashMap<>();
        for (String category : accommodationCategories) {
            roomsByCategory.put(category, new ArrayList<>());
        }
        for (SampleAccommodation accommodation : accommodations) {
            List<String> rooms = roomsByCategory.computeIfAbsent(accommodation.category(), ignored -> new ArrayList<>());
            addUnique(rooms, accommodation.room());
        }

        textCell(rowAt(sheet, 0), VALIDATION_SOURCE_ROOM_MAP_CATEGORY_COLUMN, "Room Map Category", style);
        textCell(rowAt(sheet, 0), VALIDATION_SOURCE_ROOM_MAP_NAME_COLUMN, "Room Map Range", style);
        textCell(rowAt(sheet, 0), VALIDATION_SOURCE_ROOM_LIST_START_COLUMN, "Empty Room List", style);
        textCell(rowAt(sheet, VALIDATION_SOURCE_FIRST_ROW), VALIDATION_SOURCE_ROOM_LIST_START_COLUMN, "", style);
        createWorkbookName(
                workbook,
                EMPTY_ROOM_RANGE_NAME,
                sourceRangeFormula(sheet, VALIDATION_SOURCE_ROOM_LIST_START_COLUMN, VALIDATION_SOURCE_FIRST_ROW, 1)
        );

        int mapRow = VALIDATION_SOURCE_FIRST_ROW;
        int roomColumn = VALIDATION_SOURCE_ROOM_LIST_START_COLUMN + 1;
        Set<String> usedNames = new HashSet<>();
        for (Map.Entry<String, List<String>> entry : roomsByCategory.entrySet()) {
            String rangeName = uniqueRoomRangeName(entry.getKey(), usedNames);
            List<String> rooms = entry.getValue().isEmpty() ? List.of("") : entry.getValue();
            textCell(rowAt(sheet, mapRow), VALIDATION_SOURCE_ROOM_MAP_CATEGORY_COLUMN, entry.getKey(), style);
            textCell(rowAt(sheet, mapRow), VALIDATION_SOURCE_ROOM_MAP_NAME_COLUMN, rangeName, style);
            writeSourceList(sheet, roomColumn, rangeName, rooms, style);
            createWorkbookName(
                    workbook,
                    rangeName,
                    sourceRangeFormula(sheet, roomColumn, VALIDATION_SOURCE_FIRST_ROW, rooms.size())
            );
            mapRow++;
            roomColumn++;
        }

        return sourceRangeFormula(
                sheet,
                VALIDATION_SOURCE_ROOM_MAP_CATEGORY_COLUMN,
                VALIDATION_SOURCE_FIRST_ROW,
                Math.max(1, roomsByCategory.size()),
                VALIDATION_SOURCE_ROOM_MAP_NAME_COLUMN
        );
    }

    private static String writeSourceList(
            Sheet sheet,
            int column,
            String header,
            List<String> values,
            CellStyle style
    ) {
        textCell(rowAt(sheet, 0), column, header, style);
        List<String> safeValues = values == null || values.isEmpty() ? List.of("") : values;
        for (int index = 0; index < safeValues.size(); index++) {
            textCell(rowAt(sheet, VALIDATION_SOURCE_FIRST_ROW + index), column, safeValues.get(index), style);
        }
        return sourceRangeFormula(sheet, column, VALIDATION_SOURCE_FIRST_ROW, safeValues.size());
    }

    private static void hideValidationSourceColumns(Sheet sheet) {
        int lastHiddenColumn = Math.max(
                VALIDATION_SOURCE_ROOM_LIST_START_COLUMN + 1,
                sheet.getRow(0).getLastCellNum()
        );
        for (int column = VALIDATION_SOURCE_GUEST_CATEGORY_COLUMN; column <= lastHiddenColumn; column++) {
            sheet.setColumnHidden(column, true);
        }
    }

    private static Row rowAt(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        return row == null ? sheet.createRow(rowIndex) : row;
    }

    private static List<String> uniqueValues(List<String> values) {
        List<String> unique = new ArrayList<>();
        if (values == null) {
            return unique;
        }
        for (String value : values) {
            addUnique(unique, value);
        }
        return unique;
    }

    private static void addUnique(List<String> values, String value) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return;
        }
        for (String existing : values) {
            if (existing.equalsIgnoreCase(text)) {
                return;
            }
        }
        values.add(text);
    }

    private static String uniqueRoomRangeName(String category, Set<String> usedNames) {
        String text = category == null ? "" : category.toUpperCase(Locale.ROOT).trim();
        String name = "KGM_ROOM_" + text.replaceAll("[^A-Z0-9_]", "_").replaceAll("_+", "_");
        if (name.endsWith("_")) {
            name = name.substring(0, name.length() - 1);
        }
        if (name.length() > 220) {
            name = name.substring(0, 220);
        }
        if ("KGM_ROOM".equals(name)) {
            name = "KGM_ROOM_CATEGORY";
        }

        String uniqueName = name;
        int suffix = 2;
        while (usedNames.contains(uniqueName)) {
            uniqueName = name + "_" + suffix++;
        }
        usedNames.add(uniqueName);
        return uniqueName;
    }

    private static void createWorkbookName(Workbook workbook, String name, String formula) {
        org.apache.poi.ss.usermodel.Name workbookName = workbook.createName();
        workbookName.setNameName(name);
        workbookName.setRefersToFormula(formula);
    }

    private static String sourceRangeFormula(Sheet sheet, int column, int firstRow, int rowCount) {
        return sourceRangeFormula(sheet, column, firstRow, rowCount, column);
    }

    private static String sourceRangeFormula(
            Sheet sheet,
            int firstColumn,
            int firstRow,
            int rowCount,
            int lastColumn
    ) {
        int lastRow = firstRow + Math.max(1, rowCount) - 1;
        return quoteSheetName(sheet.getSheetName())
                + "!" + absoluteCell(firstColumn, firstRow)
                + ":" + absoluteCell(lastColumn, lastRow);
    }

    private static String quoteSheetName(String sheetName) {
        return "'" + sheetName.replace("'", "''") + "'";
    }

    private static String absoluteCell(int column, int row) {
        return "$" + columnName(column) + "$" + (row + 1);
    }

    private static int writeImportRulesSection(
            Sheet values,
            int rowIndex,
            CellStyle titleStyle,
            CellStyle ruleHeaderStyle,
            CellStyle ruleStyle
    ) {
        values.createRow(rowIndex++);

        Row title = values.createRow(rowIndex++);
        mergedTextCell(values, title, 0, VALID_VALUES_LAST_COLUMN, "Import Rules and Checks", titleStyle);

        Row columns = values.createRow(rowIndex++);
        textCell(columns, 0, "Rule Area", ruleHeaderStyle);
        textCell(columns, 1, "Import New / Standard Data", ruleHeaderStyle);
        textCell(columns, 2, "Import Legacy / Historical Data", ruleHeaderStyle);

        rowIndex = writeRuleRow(
                values,
                rowIndex,
                "Workbook and rows",
                WORKBOOK_ROW_RULE,
                WORKBOOK_ROW_RULE,
                ruleStyle
        );
        rowIndex = writeRuleRow(
                values,
                rowIndex,
                "Headers",
                STANDARD_HEADER_RULE,
                LEGACY_HEADER_RULE,
                ruleStyle
        );
        rowIndex = writeRuleRow(
                values,
                rowIndex,
                "Required values",
                STANDARD_REQUIRED_RULE,
                LEGACY_REQUIRED_RULE,
                ruleStyle
        );
        rowIndex = writeRuleRow(
                values,
                rowIndex,
                "CNIC / Passport format",
                IDENTIFIER_RULE_MESSAGE + " " + IDENTIFIER_INVALID_EXAMPLES_MESSAGE,
                "Blank CNIC / Passport is allowed for historical rows. If a value is provided, it must still follow the same CNIC/passport format rules.",
                ruleStyle
        );
        rowIndex = writeRuleRow(
                values,
                rowIndex,
                "Identifier matching",
                IDENTIFIER_STORAGE_RULE,
                "Legacy import allows repeated CNIC / Passport values. CNIC / Passport is not used for legacy uniqueness.",
                ruleStyle
        );
        rowIndex = writeRuleRow(
                values,
                rowIndex,
                "Stay dates",
                DATE_RULE,
                DATE_RULE,
                ruleStyle
        );
        rowIndex = writeRuleRow(
                values,
                rowIndex,
                "Visit type",
                VISIT_TYPE_RULE,
                VISIT_TYPE_RULE,
                ruleStyle
        );
        rowIndex = writeRuleRow(
                values,
                rowIndex,
                "Accommodation and room",
                STANDARD_ROOM_RULE + " " + ROOM_NAME_RULE,
                LEGACY_ROOM_RULE + " " + ROOM_NAME_RULE,
                ruleStyle
        );
        rowIndex = writeRuleRow(
                values,
                rowIndex,
                "Overlap and capacity",
                STANDARD_STAY_RULE,
                LEGACY_STAY_RULE,
                ruleStyle
        );
        rowIndex = writeRuleRow(
                values,
                rowIndex,
                "Duplicate checks",
                "Standard import uses the live Add Guest checks. A guest with the same CNIC / Passport cannot be imported for overlapping dates.",
                LEGACY_DUPLICATE_RULE,
                ruleStyle
        );
        rowIndex = writeRuleRow(
                values,
                rowIndex,
                "Same-workbook duplicate rows",
                "Rows must pass standard validation independently.",
                LEGACY_WORKBOOK_DUPLICATE_RULE,
                ruleStyle
        );
        rowIndex = writeRuleRow(
                values,
                rowIndex,
                "Defaults and saved values",
                STANDARD_DEFAULT_RULE,
                LEGACY_DEFAULT_RULE,
                ruleStyle
        );
        rowIndex = writeRuleRow(
                values,
                rowIndex,
                "Import scope",
                IMPORT_SCOPE_RULE,
                IMPORT_SCOPE_RULE,
                ruleStyle
        );
        return writeRuleRow(
                values,
                rowIndex,
                "Examples",
                "Pakistani/Pakistan CNIC: 3520212345678. Non-Pakistani passport: PX1234567 or AB-123 456.",
                "Repeated legacy placeholder example: 9999999999999.",
                ruleStyle
        );
    }

    private static int writeRuleRow(
            Sheet sheet,
            int rowIndex,
            String area,
            String standardRule,
            String legacyRule,
            CellStyle style
    ) {
        Row row = sheet.createRow(rowIndex++);
        row.setHeightInPoints(56);
        textCell(row, 0, area, style);
        textCell(row, 1, standardRule, style);
        textCell(row, 2, legacyRule, style);
        return rowIndex;
    }

    private static void addSampleDataValidations(Sheet sheet, ValidationSourceRanges validationSources) {
        addIdentifierValidation(sheet);
        addFormulaListValidation(
                sheet,
                GUEST_CATEGORY,
                validationSources.guestCategoryFormula(),
                "Guest Category",
                "Choose a guest category from the current database values.",
                "Invalid Guest Category",
                "Select a guest category from the dropdown list."
        );
        addVisitTypeDropdown(sheet);
        addFlexibleFormulaListValidation(
                sheet,
                REQUESTED_DEPARTMENT,
                validationSources.departmentFormula(),
                "Requested Department",
                "Choose a department from the list or type a new department value if it is not listed."
        );
        addDateTimeValidation(sheet, ARRIVAL_DATE_TIME);
        addDateTimeValidation(sheet, DEPARTURE_DATE_TIME);
        addFormulaListValidation(
                sheet,
                ACCOMMODATION_CATEGORY,
                validationSources.accommodationCategoryFormula(),
                "Accommodation Category",
                "Choose an accommodation category from the current database values.",
                "Invalid Accommodation Category",
                "Select an accommodation category from the dropdown list."
        );
        addRoomDropdown(sheet, validationSources);
    }

    private static void addVisitTypeDropdown(Sheet sheet) {
        addExplicitListValidation(
                sheet,
                VISIT_TYPE,
                VISIT_TYPES,
                "Visit Type",
                "Choose Official Visit or Personal Visit.",
                "Invalid Visit Type",
                "Choose Official Visit or Personal Visit."
        );
    }

    private static void addIdentifierValidation(Sheet sheet) {
        int column = TEMPLATE_HEADERS.indexOf(CNIC);
        if (column < 0) {
            return;
        }

        addCustomValidation(
                sheet,
                column,
                identifierValidationFormula(columnName(column) + "2"),
                "CNIC / Passport",
                "CNIC: exactly 13 digits with no dashes. Passport: 4 to 30 letters/digits, at least one letter, spaces/hyphens only as separators.",
                "Invalid CNIC / Passport",
                "Enter either a 13-digit CNIC without dashes or a passport such as PX1234567 or AB-123 456."
        );
    }

    private static void addDateTimeValidation(Sheet sheet, String header) {
        int column = TEMPLATE_HEADERS.indexOf(header);
        if (column < 0) {
            return;
        }

        addCustomValidation(
                sheet,
                column,
                exactDateTimeFormula(columnName(column) + "2"),
                header,
                "Type date/time exactly as yyyy-MM-dd HH:mm, for example 2026-05-30 10:00.",
                "Invalid Date Time",
                "Use exact format yyyy-MM-dd HH:mm, for example 2026-05-30 10:00."
        );
    }

    private static void addRoomDropdown(Sheet sheet, ValidationSourceRanges validationSources) {
        int roomColumn = TEMPLATE_HEADERS.indexOf(ROOM);
        int categoryColumn = TEMPLATE_HEADERS.indexOf(ACCOMMODATION_CATEGORY);
        if (roomColumn < 0 || categoryColumn < 0) {
            return;
        }

        String categoryCell = "$" + columnName(categoryColumn) + "2";
        String formula = "INDIRECT(IFERROR(VLOOKUP(" + categoryCell + ","
                + validationSources.roomMapFormula() + ",2,FALSE),\"" + EMPTY_ROOM_RANGE_NAME + "\"))";
        addFormulaListValidation(
                sheet,
                ROOM,
                formula,
                "Room",
                "Choose a room for the selected accommodation category.",
                "Invalid Room",
                "Select a room from the dropdown list for the selected accommodation category."
        );
    }

    private static void addFormulaListValidation(
            Sheet sheet,
            String header,
            String formula,
            String promptTitle,
            String promptText,
            String errorTitle,
            String errorText
    ) {
        int column = TEMPLATE_HEADERS.indexOf(header);
        if (column < 0) {
            return;
        }

        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createFormulaListConstraint(formula);
        DataValidation validation = helper.createValidation(constraint, validationRange(column));
        enableInCellDropdown(validation);
        configureValidation(validation, promptTitle, promptText, errorTitle, errorText);
        sheet.addValidationData(validation);
    }

    private static void addFlexibleFormulaListValidation(
            Sheet sheet,
            String header,
            String formula,
            String promptTitle,
            String promptText
    ) {
        int column = TEMPLATE_HEADERS.indexOf(header);
        if (column < 0) {
            return;
        }

        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createFormulaListConstraint(formula);
        DataValidation validation = helper.createValidation(constraint, validationRange(column));
        enableInCellDropdown(validation);
        validation.setEmptyCellAllowed(false);
        validation.setShowPromptBox(true);
        validation.createPromptBox(promptTitle, promptText);
        validation.setShowErrorBox(false);
        sheet.addValidationData(validation);
    }

    private static void addExplicitListValidation(
            Sheet sheet,
            String header,
            String[] values,
            String promptTitle,
            String promptText,
            String errorTitle,
            String errorText
    ) {
        int column = TEMPLATE_HEADERS.indexOf(header);
        if (column < 0) {
            return;
        }

        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createExplicitListConstraint(values);
        DataValidation validation = helper.createValidation(constraint, validationRange(column));
        enableInCellDropdown(validation);
        configureValidation(validation, promptTitle, promptText, errorTitle, errorText);
        sheet.addValidationData(validation);
    }

    private static void enableInCellDropdown(DataValidation validation) {
        // XSSF stores Excel's in-cell dropdown checkbox with inverted OOXML semantics.
        validation.setSuppressDropDownArrow(true);
    }

    private static void addCustomValidation(
            Sheet sheet,
            int column,
            String formula,
            String promptTitle,
            String promptText,
            String errorTitle,
            String errorText
    ) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createCustomConstraint(formula);
        DataValidation validation = helper.createValidation(constraint, validationRange(column));
        configureValidation(validation, promptTitle, promptText, errorTitle, errorText);
        sheet.addValidationData(validation);
    }

    private static CellRangeAddressList validationRange(int column) {
        return new CellRangeAddressList(1, TEMPLATE_DROPDOWN_LAST_ROW, column, column);
    }

    private static void configureValidation(
            DataValidation validation,
            String promptTitle,
            String promptText,
            String errorTitle,
            String errorText
    ) {
        validation.setEmptyCellAllowed(false);
        validation.setShowPromptBox(true);
        validation.createPromptBox(promptTitle, promptText);
        validation.setShowErrorBox(true);
        validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        validation.createErrorBox(errorTitle, errorText);
    }

    private static String identifierValidationFormula(String cellRef) {
        String compact = "SUBSTITUTE(SUBSTITUTE(" + cellRef + ",\" \",\"\"),\"-\",\"\")";
        String characterRows = "ROW(INDIRECT(\"1:\"&LEN(" + cellRef + ")))";
        String cnic = "IFERROR(AND(LEN(" + cellRef + ")=13,"
                + "SUMPRODUCT(--ISNUMBER(--MID(" + cellRef + "," + characterRows + ",1)))=13),FALSE)";
        String allowedPassportCharacters = "SUMPRODUCT(--ISNUMBER(FIND(MID(UPPER(" + cellRef + "),"
                + characterRows + ",1),\"ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 -\")))=LEN(" + cellRef + ")";
        String passportHasLetter = "SUMPRODUCT(--ISNUMBER(FIND(MID(UPPER(" + cellRef + "),"
                + characterRows + ",1),\"ABCDEFGHIJKLMNOPQRSTUVWXYZ\")))>0";
        String passport = "IFERROR(AND(LEN(" + compact + ")>=4,LEN(" + compact + ")<=30,"
                + allowedPassportCharacters + ","
                + passportHasLetter + ","
                + "LEFT(" + cellRef + ",1)<>\" \",LEFT(" + cellRef + ",1)<>\"-\","
                + "RIGHT(" + cellRef + ",1)<>\" \",RIGHT(" + cellRef + ",1)<>\"-\","
                + "ISERROR(SEARCH(\"--\"," + cellRef + ")),"
                + "ISERROR(SEARCH(\"  \"," + cellRef + ")),"
                + "ISERROR(SEARCH(\"- \"," + cellRef + ")),"
                + "ISERROR(SEARCH(\" -\"," + cellRef + "))),FALSE)";
        return "IFERROR(OR(" + cnic + "," + passport + "),FALSE)";
    }

    private static String exactDateTimeFormula(String cellRef) {
        return "IFERROR(AND("
                + "LEN(" + cellRef + ")=16,"
                + "MID(" + cellRef + ",5,1)=\"-\","
                + "MID(" + cellRef + ",8,1)=\"-\","
                + "MID(" + cellRef + ",11,1)=\" \","
                + "MID(" + cellRef + ",14,1)=\":\","
                + "ISNUMBER(--LEFT(" + cellRef + ",4)),"
                + "ISNUMBER(--MID(" + cellRef + ",6,2)),"
                + "ISNUMBER(--MID(" + cellRef + ",9,2)),"
                + "ISNUMBER(--MID(" + cellRef + ",12,2)),"
                + "ISNUMBER(--RIGHT(" + cellRef + ",2)),"
                + "TEXT(DATE(--LEFT(" + cellRef + ",4),--MID(" + cellRef + ",6,2),--MID(" + cellRef + ",9,2)),\"yyyy-mm-dd\")=LEFT(" + cellRef + ",10),"
                + "--MID(" + cellRef + ",12,2)<24,"
                + "--RIGHT(" + cellRef + ",2)<60"
                + "),FALSE)";
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

    private static CellStyle importCellStyle(int columnIndex, CellStyle plainStyle, CellStyle dateTimeTextStyle) {
        String header = TEMPLATE_HEADERS.get(columnIndex);
        if (ARRIVAL_DATE_TIME.equals(header) || DEPARTURE_DATE_TIME.equals(header)) {
            return dateTimeTextStyle;
        }
        return plainStyle;
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

    private static String samplePassport(int index) {
        return String.format("PX%07d", index + 1);
    }

    private static String sampleIdentifier(int index) {
        return index % 2 == 0 ? sampleCnic(index) : samplePassport(index);
    }

    private static String sampleNationality(int index) {
        String[] nationalities = {
                "Pakistani", "Turkish", "Pakistani", "Chinese", "Pakistani", "Afghan"
        };
        return nationalities[index % nationalities.length];
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

    private static String columnName(int columnIndex) {
        StringBuilder name = new StringBuilder();
        int value = columnIndex + 1;
        while (value > 0) {
            int remainder = (value - 1) % 26;
            name.insert(0, (char) ('A' + remainder));
            value = (value - 1) / 26;
        }
        return name.toString();
    }

    private record ValidationSourceRanges(
            String guestCategoryFormula,
            String departmentFormula,
            String accommodationCategoryFormula,
            String roomMapFormula
    ) {
    }

    /**
     * Record representing a sample accommodation for template generation.
     */
    public record SampleAccommodation(String category, String room, String status, int capacity, int availableBeds) {
    }
}
