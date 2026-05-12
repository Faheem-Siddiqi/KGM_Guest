package com.kgm.service;

import com.kgm.dao.GuestDao;
import com.kgm.model.Guest;

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
import java.util.List;
import java.util.Locale;

public class GuestReportService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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
            "Guest Name", "CNIC", "Category", "Department", "Accommodation", "Room",
            "Arrival", "Departure", "Status", "Tenure"
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
        page.text("Kohinoor Textile Mill Ltd.", textX, top - 15, 18, true, TEXT_PRIMARY);
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
                text(guest.getGuestCategory()),
                text(guest.getRequestedDepartment()),
                text(guest.getAccommodation()),
                text(guest.getRoomName()),
                dateTime(guest.getArrivalAt()),
                dateTime(guest.getDepartureAt()),
                status(guest),
                tenure(guest)
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

    private String tenure(Guest guest) {
        LocalDateTime arrival = localDateTime(guest.getArrivalAt());
        LocalDateTime departure = localDateTime(guest.getDepartureAt());
        if (arrival == null || departure == null || departure.isBefore(arrival)) {
            return "-";
        }
        long totalHours = Duration.between(arrival, departure).toHours();
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
}
