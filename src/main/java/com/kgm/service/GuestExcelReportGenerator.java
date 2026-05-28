package com.kgm.service;

import com.kgm.model.Guest;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.AxisCrosses;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.BarDirection;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class GuestExcelReportGenerator {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private static final String[] EXCEL_COLUMNS = {
            "Guest Name", "CNIC / Passport", "Nationality", "Guest Category", "Address", "Company Name",
            "Visit Type", "Requested By", "Requested Department", "Approved By", "Accommodated By",
            "Arrival Date Time", "Departure Date Time", "Accommodation Category", "Room",
            "Status", "Tenure", "Remarks"
    };

    File write(File target, GuestReportService.ReportRange range, List<Guest> guests) throws IOException {
        Path targetPath = target.toPath().toAbsolutePath().normalize();
        Path parent = targetPath.getParent();
        if (parent == null) {
            parent = Path.of(".").toAbsolutePath().normalize();
            targetPath = parent.resolve(targetPath.getFileName());
        }
        Files.createDirectories(parent);

        Path temp = Files.createTempFile(parent, targetPath.getFileName().toString(), ".tmp");
        boolean saved = false;
        try {
            try (Workbook workbook = build(range, guests);
                 OutputStream output = Files.newOutputStream(temp)) {
                workbook.write(output);
            }
            moveReplacing(temp, targetPath);
            saved = true;
            return targetPath.toFile();
        } finally {
            if (!saved) {
                Files.deleteIfExists(temp);
            }
        }
    }

    private void moveReplacing(Path temp, Path target) throws IOException {
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Workbook build(GuestReportService.ReportRange range, List<Guest> guests) {
        Workbook workbook = new XSSFWorkbook();
        ExcelStyles styles = excelStyles(workbook);
        writeSummarySheet(workbook, styles, range, guests);
        writeGuestReportSheet(workbook, styles, range, guests);
        return workbook;
    }

    private ExcelStyles excelStyles(Workbook workbook) {
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
        titleStyle.setFont(titleFont);

        CellStyle metaStyle = workbook.createCellStyle();
        Font metaFont = workbook.createFont();
        metaFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        metaStyle.setFont(metaFont);

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        CellStyle metricLabelStyle = workbook.createCellStyle();
        Font metricLabelFont = workbook.createFont();
        metricLabelFont.setBold(true);
        metricLabelStyle.setFont(metricLabelFont);
        metricLabelStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        metricLabelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        metricLabelStyle.setBorderBottom(BorderStyle.THIN);
        metricLabelStyle.setBorderTop(BorderStyle.THIN);
        metricLabelStyle.setBorderLeft(BorderStyle.THIN);
        metricLabelStyle.setBorderRight(BorderStyle.THIN);

        CellStyle valueStyle = workbook.createCellStyle();
        valueStyle.setBorderBottom(BorderStyle.THIN);
        valueStyle.setBorderTop(BorderStyle.THIN);
        valueStyle.setBorderLeft(BorderStyle.THIN);
        valueStyle.setBorderRight(BorderStyle.THIN);

        CellStyle sectionTitleStyle = workbook.createCellStyle();
        Font sectionTitleFont = workbook.createFont();
        sectionTitleFont.setBold(true);
        sectionTitleFont.setColor(IndexedColors.WHITE.getIndex());
        sectionTitleStyle.setFont(sectionTitleFont);
        sectionTitleStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        sectionTitleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        sectionTitleStyle.setBorderBottom(BorderStyle.THIN);
        sectionTitleStyle.setBorderTop(BorderStyle.THIN);
        sectionTitleStyle.setBorderLeft(BorderStyle.THIN);
        sectionTitleStyle.setBorderRight(BorderStyle.THIN);

        CellStyle plainStyle = workbook.createCellStyle();
        plainStyle.setBorderBottom(BorderStyle.HAIR);
        plainStyle.setBorderTop(BorderStyle.HAIR);
        plainStyle.setBorderLeft(BorderStyle.HAIR);
        plainStyle.setBorderRight(BorderStyle.HAIR);

        return new ExcelStyles(titleStyle, metaStyle, headerStyle, metricLabelStyle, valueStyle, sectionTitleStyle, plainStyle);
    }

    private void writeSummarySheet(Workbook workbook, ExcelStyles styles, GuestReportService.ReportRange range, List<Guest> guests) {
        Sheet sheet = workbook.createSheet("Summary");
        Map<String, Integer> visitTypeCounts = countByVisitType(guests);
        Map<String, Integer> statusCounts = countByField(guests, SummaryField.STATUS);
        Map<String, Integer> guestCategoryCounts = countByField(guests, SummaryField.GUEST_CATEGORY);
        Map<String, Integer> nationalityCounts = topValues(countByField(guests, SummaryField.NATIONALITY), 8);
        Map<String, Integer> companyCounts = topValues(countByField(guests, SummaryField.COMPANY), 8);
        Map<String, Integer> accommodationCounts = topValues(countByField(guests, SummaryField.ACCOMMODATION), 8);
        int rowIndex = 0;

        mergedTextCell(sheet, rowIndex++, 0, 5, "Guest Accommodation Report", styles.titleStyle());
        mergedTextCell(sheet, rowIndex++, 0, 5, range.label() + " | " + DATE.format(range.startDate())
                + " to " + DATE.format(range.endDate()), styles.metaStyle());
        mergedTextCell(sheet, rowIndex++, 0, 5, "Generated: " + LocalDateTime.now().format(DATE_TIME), styles.metaStyle());
        rowIndex++;

        rowIndex = writeOverviewMetrics(sheet, rowIndex, summaryMetrics(guests), styles);
        rowIndex++;
        rowIndex = writeCountSectionPair(
                sheet,
                rowIndex,
                "Visit Type Summary",
                visitTypeCounts,
                "Stay Status Summary",
                statusCounts,
                guests.size(),
                styles
        );
        rowIndex++;
        rowIndex = writeCountSectionPair(
                sheet,
                rowIndex,
                "Guest Category Demographics",
                guestCategoryCounts,
                "Nationality Demographics",
                nationalityCounts,
                guests.size(),
                styles
        );
        rowIndex++;
        rowIndex = writeCountSectionPair(
                sheet,
                rowIndex,
                "Company / Organization",
                companyCounts,
                "Department Summary",
                countByField(guests, SummaryField.DEPARTMENT),
                guests.size(),
                styles
        );
        rowIndex++;
        writeCountSection(sheet, rowIndex, "Accommodation Summary", accommodationCounts,
                guests.size(), styles, 0);

        ChartDataRanges chartRanges = writeChartDataSheet(
                workbook,
                visitTypeCounts,
                statusCounts,
                guestCategoryCounts,
                nationalityCounts,
                companyCounts,
                accommodationCounts,
                styles
        );
        addSummaryCharts(sheet, chartRanges);

        int[] widths = {6200, 2600, 2600, 6200, 2600, 2600, 500};
        for (int index = 0; index < widths.length; index++) {
            sheet.setColumnWidth(index, widths[index]);
        }
        for (int index = 7; index <= 20; index++) {
            sheet.setColumnWidth(index, 3000);
        }
        sheet.createFreezePane(0, 4);
    }

    private void writeGuestReportSheet(Workbook workbook, ExcelStyles styles, GuestReportService.ReportRange range, List<Guest> guests) {
        Sheet sheet = workbook.createSheet("Guest Report");
        int rowIndex = 0;
        Row title = sheet.createRow(rowIndex++);
        textCell(title, 0, "Guest Accommodation Report", styles.titleStyle());

        Row period = sheet.createRow(rowIndex++);
        textCell(period, 0, range.label() + " | " + DATE.format(range.startDate())
                + " to " + DATE.format(range.endDate()), styles.metaStyle());

        Row generated = sheet.createRow(rowIndex++);
        textCell(generated, 0, "Generated: " + LocalDateTime.now().format(DATE_TIME), styles.metaStyle());
        rowIndex++;

        Row header = sheet.createRow(rowIndex++);
        for (int index = 0; index < EXCEL_COLUMNS.length; index++) {
            textCell(header, index, EXCEL_COLUMNS[index], styles.headerStyle());
        }

        for (Guest guest : guests) {
            Row row = sheet.createRow(rowIndex++);
            String[] values = excelRowValues(guest);
            for (int index = 0; index < values.length; index++) {
                textCell(row, index, values[index], styles.plainStyle());
            }
        }

        sheet.createFreezePane(0, 5);
        for (int index = 0; index < EXCEL_COLUMNS.length; index++) {
            sheet.autoSizeColumn(index);
            int width = Math.max(3200, Math.min(11000, sheet.getColumnWidth(index) + 800));
            sheet.setColumnWidth(index, width);
        }
    }

    private String[] excelRowValues(Guest guest) {
        return new String[]{
                text(guest.getGuestName()),
                text(guest.getCnic()),
                text(guest.getNationality()),
                text(guest.getGuestCategory()),
                text(guest.getAddress()),
                text(guest.getCompanyName()),
                text(guest.getVisitType()),
                text(guest.getRequestedBy()),
                text(guest.getRequestedDepartment()),
                text(guest.getApprovedBy()),
                text(guest.getAccommodatedBy()),
                dateTime(guest.getArrivalAt()),
                dateTime(guest.getDepartureAt()),
                text(guest.getAccommodation()),
                text(guest.getRoomName()),
                status(guest),
                tenure(guest),
                text(guest.getRemarks())
        };
    }

    private boolean currentlyStaying(Guest guest) {
        return "Currently Staying".equals(status(guest));
    }

    private boolean upcoming(Guest guest) {
        return "Upcoming".equals(status(guest));
    }

    private boolean departed(Guest guest) {
        return "Departed".equals(status(guest));
    }

    private String status(Guest guest) {
        return GuestValidationService.stayStatus(guest.getArrivalAt(), guest.getDepartureAt());
    }

    private String[][] summaryMetrics(List<Guest> guests) {
        long current = guests.stream().filter(this::currentlyStaying).count();
        long upcoming = guests.stream().filter(this::upcoming).count();
        long departed = guests.stream().filter(this::departed).count();
        Set<String> departments = uniqueValues(guests, SummaryField.DEPARTMENT);
        Set<String> accommodations = uniqueValues(guests, SummaryField.ACCOMMODATION);
        Set<String> guestCategories = uniqueValues(guests, SummaryField.GUEST_CATEGORY);
        Set<String> nationalities = uniqueValues(guests, SummaryField.NATIONALITY);
        Set<String> companies = uniqueValues(guests, SummaryField.COMPANY);
        return new String[][]{
                {"Total Guests", String.valueOf(guests.size())},
                {"Currently Staying", String.valueOf(current)},
                {"Upcoming", String.valueOf(upcoming)},
                {"Departed", String.valueOf(departed)},
                {"Official Visits", String.valueOf(countVisitType(guests, "Official Visit"))},
                {"Personal Visits", String.valueOf(countVisitType(guests, "Personal Visit"))},
                {"Guest Categories", String.valueOf(guestCategories.size())},
                {"Nationalities", String.valueOf(nationalities.size())},
                {"Companies / Organizations", String.valueOf(companies.size())},
                {"Departments Covered", String.valueOf(departments.size())},
                {"Accommodation Categories", String.valueOf(accommodations.size())},
                {"Average Planned Stay", averageTenure(guests)}
        };
    }

    private long countVisitType(List<Guest> guests, String visitType) {
        return guests.stream()
                .filter(guest -> visitType.equalsIgnoreCase(text(guest.getVisitType())))
                .count();
    }

    private Set<String> uniqueValues(List<Guest> guests, SummaryField field) {
        Set<String> values = new LinkedHashSet<>();
        for (Guest guest : guests) {
            String value = summaryValue(guest, field);
            if (!"-".equals(value) && !"Not Provided".equals(value)) {
                values.add(value);
            }
        }
        return values;
    }

    private Map<String, Integer> countByVisitType(List<Guest> guests) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("Official Visit", 0);
        counts.put("Personal Visit", 0);
        for (Guest guest : guests) {
            String key = summaryValue(guest, SummaryField.VISIT_TYPE);
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }
        return counts;
    }

    private Map<String, Integer> countByField(List<Guest> guests, SummaryField field) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Guest guest : guests) {
            String key = summaryValue(guest, field);
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }
        return counts;
    }

    private String summaryValue(Guest guest, SummaryField field) {
        String value = switch (field) {
            case VISIT_TYPE -> text(guest.getVisitType());
            case DEPARTMENT -> text(guest.getRequestedDepartment());
            case ACCOMMODATION -> text(guest.getAccommodation());
            case NATIONALITY -> text(guest.getNationality());
            case GUEST_CATEGORY -> text(guest.getGuestCategory());
            case COMPANY -> text(guest.getCompanyName());
            case STATUS -> status(guest);
        };
        return "-".equals(value) ? "Not Provided" : value;
    }

    private int writeOverviewMetrics(
            Sheet sheet,
            int rowIndex,
            String[][] metrics,
            ExcelStyles styles
    ) {
        mergedTextCell(sheet, rowIndex++, 0, 5, "Executive Overview", styles.sectionTitleStyle());
        for (int index = 0; index < metrics.length; index += 3) {
            Row labelRow = sheet.createRow(rowIndex++);
            Row valueRow = sheet.createRow(rowIndex++);
            for (int group = 0; group < 3 && index + group < metrics.length; group++) {
                int column = group * 2;
                mergedTextCell(sheet, labelRow.getRowNum(), column, column + 1, metrics[index + group][0],
                        styles.metricLabelStyle());
                mergedTextCell(sheet, valueRow.getRowNum(), column, column + 1, metrics[index + group][1],
                        styles.valueStyle());
            }
            rowIndex++;
        }
        return rowIndex;
    }

    private int writeCountSectionPair(
            Sheet sheet,
            int rowIndex,
            String leftTitle,
            Map<String, Integer> leftValues,
            String rightTitle,
            Map<String, Integer> rightValues,
            int totalGuests,
            ExcelStyles styles
    ) {
        int leftEnd = writeCountSection(sheet, rowIndex, leftTitle, leftValues, totalGuests, styles, 0);
        int rightEnd = writeCountSection(sheet, rowIndex, rightTitle, rightValues, totalGuests, styles, 3);
        return Math.max(leftEnd, rightEnd);
    }

    private int writeCountSection(
            Sheet sheet,
            int rowIndex,
            String title,
            Map<String, Integer> values,
            int totalGuests,
            ExcelStyles styles,
            int column
    ) {
        mergedTextCell(sheet, rowIndex++, column, column + 2, title, styles.sectionTitleStyle());
        Row header = rowAt(sheet, rowIndex++);
        textCell(header, column, "Segment", styles.headerStyle());
        textCell(header, column + 1, "Guests", styles.headerStyle());
        textCell(header, column + 2, "Share", styles.headerStyle());
        if (values.isEmpty()) {
            Row row = rowAt(sheet, rowIndex++);
            textCell(row, column, "No Records", styles.valueStyle());
            numberCell(row, column + 1, 0, styles.valueStyle());
            textCell(row, column + 2, "0%", styles.valueStyle());
            return rowIndex;
        }
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            Row row = rowAt(sheet, rowIndex++);
            textCell(row, column, entry.getKey(), styles.valueStyle());
            numberCell(row, column + 1, entry.getValue(), styles.valueStyle());
            textCell(row, column + 2, percentage(entry.getValue(), totalGuests), styles.valueStyle());
        }
        return rowIndex;
    }

    private ChartDataRanges writeChartDataSheet(
            Workbook workbook,
            Map<String, Integer> visitTypeCounts,
            Map<String, Integer> statusCounts,
            Map<String, Integer> guestCategoryCounts,
            Map<String, Integer> nationalityCounts,
            Map<String, Integer> companyCounts,
            Map<String, Integer> accommodationCounts,
            ExcelStyles styles
    ) {
        Sheet chartSheet = workbook.createSheet("Chart Data");
        int rowIndex = 0;

        ChartDataRange visitType = writeChartRange(chartSheet, rowIndex, "Visit Type", visitTypeCounts, styles);
        rowIndex = visitType.nextRow();
        ChartDataRange status = writeChartRange(chartSheet, rowIndex, "Stay Status", statusCounts, styles);
        rowIndex = status.nextRow();
        ChartDataRange guestCategory = writeChartRange(chartSheet, rowIndex, "Guest Category", guestCategoryCounts, styles);
        rowIndex = guestCategory.nextRow();
        ChartDataRange nationality = writeChartRange(chartSheet, rowIndex, "Nationality", nationalityCounts, styles);
        rowIndex = nationality.nextRow();
        ChartDataRange company = writeChartRange(chartSheet, rowIndex, "Company / Organization", companyCounts, styles);
        rowIndex = company.nextRow();
        ChartDataRange accommodation = writeChartRange(chartSheet, rowIndex, "Accommodation", accommodationCounts, styles);

        chartSheet.setColumnWidth(0, 7200);
        chartSheet.setColumnWidth(1, 2600);
        workbook.setSheetHidden(workbook.getSheetIndex(chartSheet), true);
        return new ChartDataRanges(
                (XSSFSheet) chartSheet,
                visitType,
                status,
                guestCategory,
                nationality,
                company,
                accommodation
        );
    }

    private ChartDataRange writeChartRange(
            Sheet sheet,
            int rowIndex,
            String title,
            Map<String, Integer> values,
            ExcelStyles styles
    ) {
        textCell(rowAt(sheet, rowIndex++), 0, title, styles.sectionTitleStyle());
        Row header = rowAt(sheet, rowIndex++);
        textCell(header, 0, "Segment", styles.headerStyle());
        textCell(header, 1, "Guests", styles.headerStyle());

        int firstDataRow = rowIndex;
        if (values.isEmpty()) {
            Row row = rowAt(sheet, rowIndex++);
            textCell(row, 0, "No Records", styles.valueStyle());
            numberCell(row, 1, 0, styles.valueStyle());
        } else {
            for (Map.Entry<String, Integer> entry : values.entrySet()) {
                Row row = rowAt(sheet, rowIndex++);
                textCell(row, 0, entry.getKey(), styles.valueStyle());
                numberCell(row, 1, entry.getValue(), styles.valueStyle());
            }
        }
        return new ChartDataRange(title, firstDataRow, rowIndex - 1, 0, 1, rowIndex + 2);
    }

    private void addSummaryCharts(Sheet sheet, ChartDataRanges ranges) {
        if (!(sheet instanceof XSSFSheet summarySheet) || ranges == null) {
            return;
        }
        XSSFDrawing drawing = summarySheet.createDrawingPatriarch();
        addBarChart(drawing, ranges.dataSheet(), ranges.visitType(), "Visit Type Mix", 7, 4, 13, 18);
        addBarChart(drawing, ranges.dataSheet(), ranges.status(), "Stay Status", 14, 4, 20, 18);
        addBarChart(drawing, ranges.dataSheet(), ranges.guestCategory(), "Guest Category Demographics", 7, 20, 13, 34);
        addBarChart(drawing, ranges.dataSheet(), ranges.nationality(), "Top Nationalities", 14, 20, 20, 34);
        addBarChart(drawing, ranges.dataSheet(), ranges.company(), "Top Companies / Organizations", 7, 36, 13, 50);
        addBarChart(drawing, ranges.dataSheet(), ranges.accommodation(), "Accommodation Mix", 14, 36, 20, 50);
    }

    private void addBarChart(
            XSSFDrawing drawing,
            XSSFSheet dataSheet,
            ChartDataRange range,
            String title,
            int column1,
            int row1,
            int column2,
            int row2
    ) {
        XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, column1, row1, column2, row2);
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(title);
        chart.setTitleOverlay(false);

        XDDFCategoryAxis categoryAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis valueAxis = chart.createValueAxis(AxisPosition.LEFT);
        valueAxis.setCrosses(AxisCrosses.AUTO_ZERO);

        XDDFDataSource<String> categories = XDDFDataSourcesFactory.fromStringCellRange(
                dataSheet,
                new CellRangeAddress(range.firstRow(), range.lastRow(), range.labelColumn(), range.labelColumn())
        );
        XDDFNumericalDataSource<Double> values = XDDFDataSourcesFactory.fromNumericCellRange(
                dataSheet,
                new CellRangeAddress(range.firstRow(), range.lastRow(), range.valueColumn(), range.valueColumn())
        );
        XDDFChartData data = chart.createData(ChartTypes.BAR, categoryAxis, valueAxis);
        XDDFChartData.Series series = data.addSeries(categories, values);
        series.setTitle("Guests", null);
        chart.plot(data);
        if (data instanceof XDDFBarChartData barData) {
            barData.setBarDirection(BarDirection.COL);
        }
    }

    private Map<String, Integer> topValues(Map<String, Integer> values, int limit) {
        if (values.size() <= limit) {
            return values;
        }
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(values.entrySet());
        entries.sort((left, right) -> Integer.compare(right.getValue(), left.getValue()));
        Map<String, Integer> top = new LinkedHashMap<>();
        int other = 0;
        for (int index = 0; index < entries.size(); index++) {
            Map.Entry<String, Integer> entry = entries.get(index);
            if (index < limit) {
                top.put(entry.getKey(), entry.getValue());
            } else {
                other += entry.getValue();
            }
        }
        if (other > 0) {
            top.put("Other", other);
        }
        return top;
    }

    private String averageTenure(List<Guest> guests) {
        long totalHours = 0;
        int countedGuests = 0;
        for (Guest guest : guests) {
            long hours = tenureHours(guest);
            if (hours >= 0) {
                totalHours += hours;
                countedGuests++;
            }
        }
        if (countedGuests == 0) {
            return "-";
        }
        return durationText(Math.round(totalHours / (double) countedGuests));
    }

    private String percentage(int value, int total) {
        if (total <= 0) {
            return "0%";
        }
        double share = (value * 100.0) / total;
        return String.format(Locale.US, "%.1f%%", share);
    }

    private String tenure(Guest guest) {
        long totalHours = tenureHours(guest);
        if (totalHours < 0) {
            return "-";
        }
        return durationText(totalHours);
    }

    private long tenureHours(Guest guest) {
        LocalDateTime arrival = localDateTime(guest.getArrivalAt());
        LocalDateTime departure = localDateTime(guest.getDepartureAt());
        if (arrival == null || departure == null || departure.isBefore(arrival)) {
            return -1;
        }
        return Duration.between(arrival, departure).toHours();
    }

    private String durationText(long totalHours) {
        long days = totalHours / 24;
        long hours = totalHours % 24;
        return days + "d " + hours + "h";
    }

    private LocalDateTime localDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private String dateTime(Date date) {
        LocalDateTime value = localDateTime(date);
        return value == null ? "" : value.format(DATE_TIME);
    }

    private String text(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private void textCell(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value == null ? "" : value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private void numberCell(Row row, int index, int value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private void mergedTextCell(Sheet sheet, int rowIndex, int firstColumn, int lastColumn, String value, CellStyle style) {
        Row row = rowAt(sheet, rowIndex);
        textCell(row, firstColumn, value, style);
        for (int column = firstColumn + 1; column <= lastColumn; column++) {
            textCell(row, column, "", style);
        }
        if (lastColumn > firstColumn) {
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, firstColumn, lastColumn));
        }
    }

    private Row rowAt(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        return row == null ? sheet.createRow(rowIndex) : row;
    }

    private record ExcelStyles(
            CellStyle titleStyle,
            CellStyle metaStyle,
            CellStyle headerStyle,
            CellStyle metricLabelStyle,
            CellStyle valueStyle,
            CellStyle sectionTitleStyle,
            CellStyle plainStyle
    ) {
    }

    private enum SummaryField {
        VISIT_TYPE,
        DEPARTMENT,
        ACCOMMODATION,
        NATIONALITY,
        GUEST_CATEGORY,
        COMPANY,
        STATUS
    }

    private record ChartDataRange(
            String title,
            int firstRow,
            int lastRow,
            int labelColumn,
            int valueColumn,
            int nextRow
    ) {
    }

    private record ChartDataRanges(
            XSSFSheet dataSheet,
            ChartDataRange visitType,
            ChartDataRange status,
            ChartDataRange guestCategory,
            ChartDataRange nationality,
            ChartDataRange company,
            ChartDataRange accommodation
    ) {
    }
}
