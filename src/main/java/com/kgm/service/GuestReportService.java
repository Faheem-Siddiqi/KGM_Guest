package com.kgm.service;

import com.kgm.dao.GuestDao;
import com.kgm.model.Guest;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.BorderStyle;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
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

public class GuestReportService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private static final double PAGE_WIDTH = 842;
    private static final double PAGE_HEIGHT = 595;
    private static final double MARGIN = 28;
    private static final double TABLE_WIDTH = PAGE_WIDTH - (MARGIN * 2);
    private static final double TABLE_FONT = 6.7;

    private static final String PRIMARY = "0070D2";
    private static final String TEXT_PRIMARY = "232B36";
    private static final String TEXT_SECONDARY = "637381";
    private static final String BORDER = "DCE2E8";
    private static final String HEADER_FILL = "EEF5FC";
    private static final String ROW_ALT = "F7F9FB";

    private static final String[] COLUMNS = {
            "Guest Name", "CNIC", "Visit Type", "Department", "Accommodation", "Room",
            "Arrival", "Departure", "Status", "Tenure"
    };
    private static final String[] EXCEL_COLUMNS = {
            "Guest Name", "CNIC", "Nationality", "Guest Category", "Address", "Company Name",
            "Visit Type", "Requested By", "Requested Department", "Approved By", "Accommodated By",
            "Arrival Date Time", "Departure Date Time", "Accommodation Category", "Room",
            "Status", "Tenure", "Remarks"
    };
    private static final double[] COLUMN_WIDTHS = {
            120, 80, 65, 80, 90, 55, 80, 80, 90, 46
    };

    private final GuestDao guestDao = new GuestDao();

    public File generateReport(File target, ReportRange range) throws Exception {
        List<Guest> guests = guestDao.findByStayOverlapRange(
                dateAtStart(range.startDate()),
                dateAfter(range.endDate())
        );
        return writePdfSafely(target, range, guests);
    }

    public ReportExportResult generateReports(ReportExportRequest request, ReportProgressListener listener)
            throws Exception {
        ReportProgressListener progress = listener == null ? stage -> {
        } : listener;
        if (request == null || !request.hasFormat()) {
            throw new IllegalArgumentException("Select at least one report format.");
        }

        progress.onProgress("Scanning guest rows for the selected period...");
        List<Guest> guests = guestDao.findByStayOverlapRange(
                dateAtStart(request.range().startDate()),
                dateAfter(request.range().endDate())
        );

        List<File> files = new ArrayList<>();
        String baseName = reportBaseName(request.range());
        File saveTarget = reportSaveTarget(request, baseName);

        if (request.includeExcel() && request.includePdf()) {
            progress.onProgress("Creating report folder...");
            File folder = createReportFolder(saveTarget);
            progress.onProgress("Preparing Excel report...");
            File excel = new File(folder, baseName + ".xlsx");
            files.add(writeExcelSafely(excel, request.range(), guests));
            progress.onProgress("Preparing PDF report...");
            File pdf = new File(folder, baseName + ".pdf");
            files.add(writePdfSafely(pdf, request.range(), guests));
            progress.onProgress("Finalizing report files...");
            return new ReportExportResult(folder, files);
        }

        if (request.includeExcel()) {
            progress.onProgress("Preparing Excel report...");
            File excel = withExtension(saveTarget, ".xlsx");
            files.add(writeExcelSafely(excel, request.range(), guests));
        }

        if (request.includePdf()) {
            progress.onProgress("Preparing PDF report...");
            File pdf = withExtension(saveTarget, ".pdf");
            files.add(writePdfSafely(pdf, request.range(), guests));
        }

        progress.onProgress("Finalizing report files...");
        File firstFile = files.isEmpty() ? saveTarget : files.get(0);
        return new ReportExportResult(parentDirectory(firstFile), files);
    }

    private File writePdfSafely(File target, ReportRange range, List<Guest> guests) throws IOException {
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
            try (OutputStream output = Files.newOutputStream(temp)) {
                PdfDocument document = buildPdf(range, guests);
                document.write(output);
            }
            File savedFile = commitPdf(temp, targetPath);
            saved = true;
            return savedFile;
        } finally {
            if (!saved) {
                Files.deleteIfExists(temp);
            }
        }
    }

    private File writeExcelSafely(File target, ReportRange range, List<Guest> guests) throws IOException {
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
            try (Workbook workbook = buildExcel(range, guests);
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

    private File reportSaveTarget(ReportExportRequest request, String baseName) {
        if (request.saveTarget() != null) {
            return request.saveTarget();
        }
        return downloadsDirectory().resolve(baseName).toFile();
    }

    private File createReportFolder(File selectedTarget) throws IOException {
        Path base = selectedTarget == null
                ? downloadsDirectory().resolve("Guest Report KGM")
                : selectedTarget.toPath().toAbsolutePath().normalize();
        Path parent = base.getParent() == null ? Path.of(".").toAbsolutePath().normalize() : base.getParent();
        Files.createDirectories(parent);
        for (int index = 0; index <= 99; index++) {
            Path candidate = index == 0
                    ? base
                    : parent.resolve(base.getFileName() + " (" + index + ")");
            try {
                return Files.createDirectory(candidate).toFile();
            } catch (FileAlreadyExistsException ignored) {
            }
        }
        throw new FileAlreadyExistsException(base.toString(), null, "No available numbered report folder.");
    }

    private File withExtension(File file, String extension) {
        if (file == null) {
            return downloadsDirectory().resolve("Guest Report KGM" + extension).toFile();
        }
        String path = file.getAbsolutePath();
        return path.toLowerCase(Locale.ROOT).endsWith(extension) ? file : new File(path + extension);
    }

    private File parentDirectory(File file) {
        File parent = file == null ? null : file.getParentFile();
        return parent == null ? downloadsDirectory().toFile() : parent;
    }

    private Path downloadsDirectory() {
        Path home = Path.of(System.getProperty("user.home", ".")).toAbsolutePath().normalize();
        Path downloads = home.resolve("Downloads");
        return Files.isDirectory(downloads) ? downloads : home;
    }

    private String reportBaseName(ReportRange range) {
        String label = range == null || range.label() == null || range.label().isBlank()
                ? "Guest"
                : range.label().trim();
        return safeFileName(label + " Guest Report KGM");
    }

    private String safeFileName(String value) {
        String clean = value == null ? "Guest Report KGM" : value.trim();
        clean = clean.replaceAll("[\\\\/:*?\"<>|]+", " ").replaceAll("\\s+", " ").trim();
        return clean.isEmpty() ? "Guest Report KGM" : clean;
    }

    private File commitPdf(Path temp, Path target) throws IOException {
        try {
            moveReplacing(temp, target);
            return target.toFile();
        } catch (IOException firstFailure) {
            Path fallback = nextAvailablePdf(target);
            try {
                moveNew(temp, fallback);
                return fallback.toFile();
            } catch (IOException secondFailure) {
                firstFailure.addSuppressed(secondFailure);
                throw reportSaveException(target, firstFailure);
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

    private void moveNew(Path temp, Path target) throws IOException {
        try {
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temp, target);
        }
    }

    private Path nextAvailablePdf(Path target) throws IOException {
        Path parent = target.getParent();
        String fileName = target.getFileName().toString();
        String base = fileName;
        String extension = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            base = fileName.substring(0, dot);
            extension = fileName.substring(dot);
        }

        for (int index = 1; index <= 99; index++) {
            Path candidate = parent.resolve(base + " (" + index + ")" + extension);
            if (!Files.exists(candidate)) {
                return candidate;
            }
        }
        throw new FileAlreadyExistsException(target.toString(), null, "No available numbered report filename.");
    }

    private IOException reportSaveException(Path target, IOException cause) {
        if (cause instanceof AccessDeniedException) {
            return new IOException("Report could not be saved. Close any open copy of the PDF report and try again.\nPath: "
                    + target, cause);
        }
        return new IOException("Report could not be saved. Close any open copy of the PDF report and try again.\nPath: "
                + target, cause);
    }

    private PdfDocument buildPdf(ReportRange range, List<Guest> guests) {
        PdfImage logo = PdfImage.fromFile(new File("images/pdfLogo.jpg"));
        PdfDocument document = new PdfDocument(logo);
        PdfCanvas page = document.addPage();
        double y = drawHeader(page, logo, range);
        y = drawSummary(page, guests, range, y);
        y = drawTableHeader(page, y);

        if (guests.isEmpty()) {
            drawNoRecordsRow(page, y);
            return document;
        }

        for (int index = 0; index < guests.size(); index++) {
            String[] values = rowValues(guests.get(index));
            double rowHeight = rowHeight(values);
            if (y - rowHeight < MARGIN) {
                page = document.addPage();
                y = drawContinuationHeader(page, logo, range);
                y = drawTableHeader(page, y);
            }
            drawGuestRow(page, y, values, rowHeight, index);
            y -= rowHeight;
        }
        return document;
    }

    private Workbook buildExcel(ReportRange range, List<Guest> guests) {
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

    private void writeSummarySheet(Workbook workbook, ExcelStyles styles, ReportRange range, List<Guest> guests) {
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

    private void writeGuestReportSheet(Workbook workbook, ExcelStyles styles, ReportRange range, List<Guest> guests) {
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

    private double drawHeader(PdfCanvas page, PdfImage logo, ReportRange range) {
        double top = PAGE_HEIGHT - MARGIN;
        double textX = MARGIN;
        if (logo != null) {
            // Resize image here if the PDF header logo needs manual adjustment.
            double logoHeight = 36;
            double logoWidth = Math.min(65, logoHeight * logo.aspectRatio());
            page.image("Logo", MARGIN, top - logoHeight, logoWidth, logoHeight);
            textX = MARGIN + logoWidth + 14;
        }
        page.text("Kohinoor Textile Mills. Gujar Khan", textX, top - 15, 18, true, TEXT_PRIMARY);
        page.text("Guest Accommodation Report", textX, top - 31, 12, false, TEXT_SECONDARY);
        page.text(range.label() + " | " + DATE.format(range.startDate()) + " to " + DATE.format(range.endDate()),
                textX, top - 47, 9, false, TEXT_SECONDARY);

        double right = PAGE_WIDTH - MARGIN - 190;
        page.text("Phone: 0092-051-54955328", right, top - 14, 8.5, false, TEXT_PRIMARY);
        page.text("Export: 0092-051-5473085", right, top - 30, 8.5, false, TEXT_PRIMARY);
        page.text("Generated: " + LocalDateTime.now().format(DATE_TIME), right, top - 46, 8.5, false, TEXT_SECONDARY);
        page.line(MARGIN, top - 58, PAGE_WIDTH - MARGIN, top - 58, BORDER);
        return top - 75;
    }

    private double drawContinuationHeader(PdfCanvas page, PdfImage logo, ReportRange range) {
        double top = PAGE_HEIGHT - MARGIN;
        double textX = MARGIN;
        if (logo != null) {
            double logoHeight = 28;
            double logoWidth = Math.min(54, logoHeight * logo.aspectRatio());
            page.image("Logo", MARGIN, top - logoHeight, logoWidth, logoHeight);
            textX = MARGIN + logoWidth + 12;
        }
        page.text("Guest Accommodation Report", textX, top - 14, 14, true, TEXT_PRIMARY);
        page.text(range.label() + " | " + DATE.format(range.startDate()) + " to " + DATE.format(range.endDate()),
                textX, top - 31, 9, false, TEXT_SECONDARY);
        page.line(MARGIN, top - 43, PAGE_WIDTH - MARGIN, top - 43, BORDER);
        return top - 60;
    }

    private double drawSummary(PdfCanvas page, List<Guest> guests, ReportRange range, double y) {
        long current = guests.stream().filter(this::currentlyStaying).count();
        long upcoming = guests.stream().filter(this::upcoming).count();
        long departed = guests.stream().filter(this::departed).count();
        String[][] cards = {
                {"Total Guests", String.valueOf(guests.size())},
                {"Currently Staying", String.valueOf(current)},
                {"Upcoming", String.valueOf(upcoming)},
                {"Departed", String.valueOf(departed)}
        };

        double gap = 10;
        double cardWidth = (TABLE_WIDTH - (gap * 3)) / 4;
        double cardHeight = 52;
        double dividerGap = 17;
        for (int index = 0; index < cards.length; index++) {
            double x = MARGIN + index * (cardWidth + gap);
            page.fillStrokeRect(x, y - cardHeight, cardWidth, cardHeight, HEADER_FILL, BORDER);
            page.text(cards[index][1], x + cardWidth / 2 - textWidth(cards[index][1], 17) / 2,
                    y - 20, 17, true, PRIMARY);
            page.text(cards[index][0], x + cardWidth / 2 - textWidth(cards[index][0], 8.5) / 2,
                    y - 39, 8.5, false, TEXT_SECONDARY);
        }

        double dividerY = y - cardHeight - dividerGap;
        page.line(MARGIN, dividerY, PAGE_WIDTH - MARGIN, dividerY, BORDER);

        double noteY = dividerY - dividerGap;
        page.text("Report scope: guests with stays overlapping " + DATE.format(range.startDate())
                + " through " + DATE.format(range.endDate()) + ".", MARGIN, noteY, 8.5, false, TEXT_SECONDARY);
        return noteY - 18;
    }

    private double drawTableHeader(PdfCanvas page, double y) {
        double x = MARGIN;
        double height = 22;
        for (int index = 0; index < COLUMNS.length; index++) {
            double width = COLUMN_WIDTHS[index];
            page.fillStrokeRect(x, y - height, width, height, PRIMARY, PRIMARY);
            page.text(COLUMNS[index], x + 4, y - 14, 7, true, "FFFFFF");
            x += width;
        }
        return y - height;
    }

    private void drawNoRecordsRow(PdfCanvas page, double y) {
        double height = 28;
        page.fillStrokeRect(MARGIN, y - height, TABLE_WIDTH, height, "FFFFFF", BORDER);
        page.text("No guest records found for this period.", MARGIN + 6, y - 17, 8, false, TEXT_SECONDARY);
    }

    private void drawGuestRow(PdfCanvas page, double y, String[] values, double rowHeight, int rowIndex) {
        double x = MARGIN;
        String fill = rowIndex % 2 == 0 ? "FFFFFF" : ROW_ALT;
        for (int column = 0; column < values.length; column++) {
            double width = COLUMN_WIDTHS[column];
            page.fillStrokeRect(x, y - rowHeight, width, rowHeight, fill, BORDER);
            List<String> lines = wrap(values[column], width - 8, TABLE_FONT, 2);
            double baseline = y - 10;
            for (String line : lines) {
                page.text(line, x + 4, baseline, TABLE_FONT, false, TEXT_PRIMARY);
                baseline -= TABLE_FONT + 2.2;
            }
            x += width;
        }
    }

    private double rowHeight(String[] values) {
        int maxLines = 1;
        for (int index = 0; index < values.length; index++) {
            maxLines = Math.max(maxLines, wrap(values[index], COLUMN_WIDTHS[index] - 8, TABLE_FONT, 2).size());
        }
        return Math.max(20, 8 + maxLines * (TABLE_FONT + 2.2));
    }

    private String[] rowValues(Guest guest) {
        return new String[]{
                text(guest.getGuestName()),
                text(guest.getCnic()),
                text(guest.getVisitType()),
                text(guest.getRequestedDepartment()),
                text(guest.getAccommodation()),
                text(guest.getRoomName()),
                dateTime(guest.getArrivalAt()),
                dateTime(guest.getDepartureAt()),
                status(guest),
                tenure(guest)
        };
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

    private Date dateAtStart(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date dateAfter(LocalDate date) {
        return Date.from(date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
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

    private static List<String> wrap(String text, double maxWidth, double fontSize, int maxLines) {
        List<String> lines = new ArrayList<>();
        String normalized = text == null || text.isBlank() ? "-" : text.trim().replaceAll("\\s+", " ");
        StringBuilder current = new StringBuilder();
        for (String word : normalized.split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (textWidth(candidate, fontSize) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
            } else {
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                appendWord(lines, current, word, maxWidth, fontSize);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        if (lines.isEmpty()) {
            lines.add("-");
        }
        return trimLines(lines, maxLines, maxWidth, fontSize);
    }

    private static void appendWord(List<String> lines, StringBuilder current, String word, double maxWidth, double fontSize) {
        if (textWidth(word, fontSize) <= maxWidth) {
            current.append(word);
            return;
        }
        StringBuilder part = new StringBuilder();
        for (int index = 0; index < word.length(); index++) {
            String candidate = part + String.valueOf(word.charAt(index));
            if (textWidth(candidate, fontSize) > maxWidth && !part.isEmpty()) {
                lines.add(part.toString());
                part.setLength(0);
            }
            part.append(word.charAt(index));
        }
        current.append(part);
    }

    private static List<String> trimLines(List<String> lines, int maxLines, double maxWidth, double fontSize) {
        if (lines.size() <= maxLines) {
            return lines;
        }
        List<String> trimmed = new ArrayList<>(lines.subList(0, maxLines));
        String last = trimmed.get(maxLines - 1);
        while (!last.isEmpty() && textWidth(last + "...", fontSize) > maxWidth) {
            last = last.substring(0, last.length() - 1);
        }
        trimmed.set(maxLines - 1, last + "...");
        return trimmed;
    }

    private static double textWidth(String text, double fontSize) {
        double width = 0;
        for (int index = 0; index < text.length(); index++) {
            char c = text.charAt(index);
            if (c == ' ') {
                width += fontSize * 0.28;
            } else if ("ilI.,:;!'|".indexOf(c) >= 0) {
                width += fontSize * 0.25;
            } else if ("MW@#%&".indexOf(c) >= 0) {
                width += fontSize * 0.78;
            } else if (Character.isUpperCase(c)) {
                width += fontSize * 0.6;
            } else {
                width += fontSize * 0.52;
            }
        }
        return width;
    }

    private static class PdfDocument {
        private final List<PdfCanvas> pages = new ArrayList<>();
        private final PdfImage logo;

        private PdfDocument(PdfImage logo) {
            this.logo = logo;
        }

        PdfCanvas addPage() {
            PdfCanvas canvas = new PdfCanvas();
            pages.add(canvas);
            return canvas;
        }

        void write(OutputStream output) throws IOException {
            List<byte[]> objects = new ArrayList<>();
            int pageCount = pages.size();
            int logoObject = logo == null ? -1 : 5;
            int firstPageObject = logo == null ? 5 : 6;
            int firstContentObject = firstPageObject + pageCount;

            objects.add(pdfObject("<< /Type /Catalog /Pages 2 0 R >>"));
            objects.add(pdfObject(pagesObject(pageCount, firstPageObject)));
            objects.add(pdfObject("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"));
            objects.add(pdfObject("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>"));
            if (logo != null) {
                objects.add(imageObject(logo));
            }

            for (int index = 0; index < pageCount; index++) {
                int contentObject = firstContentObject + index;
                objects.add(pdfObject("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 "
                        + fmt(PAGE_WIDTH) + " " + fmt(PAGE_HEIGHT)
                        + "] /Resources " + resources(logoObject) + " /Contents "
                        + contentObject + " 0 R >>"));
            }

            for (PdfCanvas page : pages) {
                byte[] stream = page.content().getBytes(StandardCharsets.ISO_8859_1);
                String prefix = "<< /Length " + stream.length + " >>\nstream\n";
                String suffix = "\nendstream";
                ByteArrayOutputStream object = new ByteArrayOutputStream();
                object.write(prefix.getBytes(StandardCharsets.ISO_8859_1));
                object.write(stream);
                object.write(suffix.getBytes(StandardCharsets.ISO_8859_1));
                objects.add(object.toByteArray());
            }

            ByteArrayOutputStream pdf = new ByteArrayOutputStream();
            pdf.write("%PDF-1.4\n%KGM\n".getBytes(StandardCharsets.ISO_8859_1));
            List<Integer> offsets = new ArrayList<>();
            for (int index = 0; index < objects.size(); index++) {
                offsets.add(pdf.size());
                pdf.write((index + 1 + " 0 obj\n").getBytes(StandardCharsets.ISO_8859_1));
                pdf.write(objects.get(index));
                pdf.write("\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
            }

            int xref = pdf.size();
            pdf.write(("xref\n0 " + (objects.size() + 1) + "\n").getBytes(StandardCharsets.ISO_8859_1));
            pdf.write("0000000000 65535 f \n".getBytes(StandardCharsets.ISO_8859_1));
            for (int offset : offsets) {
                pdf.write(String.format(Locale.US, "%010d 00000 n \n", offset).getBytes(StandardCharsets.ISO_8859_1));
            }
            pdf.write(("trailer\n<< /Size " + (objects.size() + 1)
                    + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n").getBytes(StandardCharsets.ISO_8859_1));
            output.write(pdf.toByteArray());
        }

        private String pagesObject(int pageCount, int firstPageObject) {
            StringBuilder kids = new StringBuilder();
            for (int index = 0; index < pageCount; index++) {
                if (index > 0) {
                    kids.append(' ');
                }
                kids.append(firstPageObject + index).append(" 0 R");
            }
            return "<< /Type /Pages /Count " + pageCount + " /Kids [" + kids + "] >>";
        }

        private byte[] pdfObject(String value) {
            return value.getBytes(StandardCharsets.ISO_8859_1);
        }

        private String resources(int logoObject) {
            StringBuilder resources = new StringBuilder("<< /Font << /F1 3 0 R /F2 4 0 R >>");
            if (logoObject > 0) {
                resources.append(" /XObject << /Logo ").append(logoObject).append(" 0 R >>");
            }
            resources.append(" >>");
            return resources.toString();
        }

        private byte[] imageObject(PdfImage image) throws IOException {
            String prefix = "<< /Type /XObject /Subtype /Image /Width " + image.width()
                    + " /Height " + image.height()
                    + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length "
                    + image.bytes().length + " >>\nstream\n";
            String suffix = "\nendstream";
            ByteArrayOutputStream object = new ByteArrayOutputStream();
            object.write(prefix.getBytes(StandardCharsets.ISO_8859_1));
            object.write(image.bytes());
            object.write(suffix.getBytes(StandardCharsets.ISO_8859_1));
            return object.toByteArray();
        }
    }

    private static class PdfCanvas {
        private final StringBuilder content = new StringBuilder();

        void fillRect(double x, double y, double width, double height, String fill) {
            color(fill, "rg");
            content.append(fmt(x)).append(' ').append(fmt(y)).append(' ')
                    .append(fmt(width)).append(' ').append(fmt(height)).append(" re f\n");
        }

        void fillStrokeRect(double x, double y, double width, double height, String fill, String stroke) {
            color(fill, "rg");
            color(stroke, "RG");
            content.append("0.6 w\n")
                    .append(fmt(x)).append(' ').append(fmt(y)).append(' ')
                    .append(fmt(width)).append(' ').append(fmt(height)).append(" re B\n");
        }

        void line(double x1, double y1, double x2, double y2, String stroke) {
            color(stroke, "RG");
            content.append("0.8 w\n")
                    .append(fmt(x1)).append(' ').append(fmt(y1)).append(" m ")
                    .append(fmt(x2)).append(' ').append(fmt(y2)).append(" l S\n");
        }

        void image(String name, double x, double y, double width, double height) {
            content.append("q ")
                    .append(fmt(width)).append(" 0 0 ")
                    .append(fmt(height)).append(' ')
                    .append(fmt(x)).append(' ')
                    .append(fmt(y)).append(" cm /")
                    .append(name)
                    .append(" Do Q\n");
        }

        void text(String text, double x, double y, double size, boolean bold, String fill) {
            color(fill, "rg");
            content.append("BT /").append(bold ? "F2" : "F1").append(' ')
                    .append(fmt(size)).append(" Tf 1 0 0 1 ")
                    .append(fmt(x)).append(' ').append(fmt(y)).append(" Tm (")
                    .append(escapePdf(text)).append(") Tj ET\n");
        }

        String content() {
            return content.toString();
        }

        private void color(String hex, String operator) {
            int red = Integer.parseInt(hex.substring(0, 2), 16);
            int green = Integer.parseInt(hex.substring(2, 4), 16);
            int blue = Integer.parseInt(hex.substring(4, 6), 16);
            content.append(fmt(red / 255.0)).append(' ')
                    .append(fmt(green / 255.0)).append(' ')
                    .append(fmt(blue / 255.0)).append(' ')
                    .append(operator).append('\n');
        }

        private String escapePdf(String value) {
            StringBuilder escaped = new StringBuilder();
            String clean = value == null ? "" : value;
            for (int index = 0; index < clean.length(); index++) {
                char c = clean.charAt(index);
                if (c == '\\' || c == '(' || c == ')') {
                    escaped.append('\\').append(c);
                } else if (c >= 32 && c <= 255) {
                    escaped.append(c);
                } else {
                    escaped.append('?');
                }
            }
            return escaped.toString();
        }
    }

    private record PdfImage(byte[] bytes, int width, int height) {
        private static PdfImage fromFile(File file) {
            if (file == null || !file.exists()) {
                return null;
            }
            try {
                BufferedImage image = ImageIO.read(file);
                if (image == null) {
                    return null;
                }
                return new PdfImage(Files.readAllBytes(file.toPath()), image.getWidth(), image.getHeight());
            } catch (IOException exception) {
                System.err.println("PDF logo not loaded: " + exception.getMessage());
                return null;
            }
        }

        private double aspectRatio() {
            return height == 0 ? 1 : width / (double) height;
        }
    }

    private static String fmt(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    public record ReportRange(String label, LocalDate startDate, LocalDate endDate) {
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

    public record ReportExportRequest(ReportRange range, boolean includePdf, boolean includeExcel, File saveTarget) {
        public boolean hasFormat() {
            return includePdf || includeExcel;
        }

        public String formatLabel() {
            if (includePdf && includeExcel) {
                return "PDF and Excel";
            }
            return includePdf ? "PDF" : "Excel";
        }

        public ReportExportRequest withSaveTarget(File target) {
            return new ReportExportRequest(range, includePdf, includeExcel, target);
        }
    }

    public record ReportExportResult(File folder, List<File> files) {
    }

    @FunctionalInterface
    public interface ReportProgressListener {
        void onProgress(String message);
    }
}
