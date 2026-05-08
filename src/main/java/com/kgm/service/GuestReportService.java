package com.kgm.service;

import com.kgm.dao.GuestDao;
import com.kgm.model.Guest;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TableRowAlign;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

public class GuestReportService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String PRIMARY = "0070D2";
    private static final String TEXT_PRIMARY = "232B36";
    private static final String TEXT_SECONDARY = "637381";
    private static final String BORDER = "DCE2E8";
    private static final String HEADER_FILL = "EEF5FC";

    private final GuestDao guestDao = new GuestDao();

    public void generateReport(File target, ReportRange range) throws Exception {
        List<Guest> guests = guestDao.findByArrivalRange(
                dateAtStart(range.startDate()),
                dateAfter(range.endDate())
        );

        try (XWPFDocument document = new XWPFDocument();
             FileOutputStream output = new FileOutputStream(target)) {
            configureA4Landscape(document);
            addHeader(document, range);
            addSummary(document, guests, range);
            addGuestTable(document, guests);
            document.write(output);
        }
    }

    private void configureA4Landscape(XWPFDocument document) {
        CTSectPr section = document.getDocument().getBody().isSetSectPr()
                ? document.getDocument().getBody().getSectPr()
                : document.getDocument().getBody().addNewSectPr();

        CTPageSz pageSize = section.isSetPgSz() ? section.getPgSz() : section.addNewPgSz();
        pageSize.setOrient(STPageOrientation.LANDSCAPE);
        pageSize.setW(BigInteger.valueOf(16838));
        pageSize.setH(BigInteger.valueOf(11906));

        CTPageMar margins = section.isSetPgMar() ? section.getPgMar() : section.addNewPgMar();
        margins.setTop(BigInteger.valueOf(567));
        margins.setRight(BigInteger.valueOf(567));
        margins.setBottom(BigInteger.valueOf(567));
        margins.setLeft(BigInteger.valueOf(567));
    }

    private void addHeader(XWPFDocument document, ReportRange range) throws Exception {
        XWPFTable table = document.createTable(1, 2);
        table.setWidth("100%");
        table.setTableAlignment(TableRowAlign.CENTER);
        removeBorders(table);

        XWPFTableRow row = table.getRow(0);
        XWPFTableCell left = row.getCell(0);
        XWPFTableCell right = row.getCell(1);
        clearCell(left);
        clearCell(right);
        setCellWidth(left, 6800);
        setCellWidth(right, 3600);

        XWPFParagraph logoLine = left.addParagraph();
        logoLine.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun logoRun = logoLine.createRun();
        File logo = new File("images/Header.jpg");
        if (logo.exists()) {
            try (FileInputStream input = new FileInputStream(logo)) {
                logoRun.addPicture(input, XWPFDocument.PICTURE_TYPE_JPEG, logo.getName(), Units.toEMU(58), Units.toEMU(48));
            }
        }
        XWPFRun company = logoLine.createRun();
        company.setText("  Koohinoor Textile Mills");
        company.setBold(true);
        company.setFontSize(18);
        company.setColor(TEXT_PRIMARY);

        XWPFParagraph title = left.addParagraph();
        addRun(title, "Guest Accommodation Report", true, 13, TEXT_SECONDARY);
        XWPFParagraph period = left.addParagraph();
        addRun(period, range.label() + " | " + DATE.format(range.startDate()) + " to " + DATE.format(range.endDate()),
                false, 10, TEXT_SECONDARY);

        XWPFParagraph contact = right.addParagraph();
        contact.setAlignment(ParagraphAlignment.RIGHT);
        addRun(contact, "Phone: ", true, 9, TEXT_SECONDARY);
        addRun(contact, "0092-051-54955328", false, 9, TEXT_PRIMARY);
        XWPFParagraph export = right.addParagraph();
        export.setAlignment(ParagraphAlignment.RIGHT);
        addRun(export, "Export: ", true, 9, TEXT_SECONDARY);
        addRun(export, "0092-051-5473085", false, 9, TEXT_PRIMARY);
        XWPFParagraph generated = right.addParagraph();
        generated.setAlignment(ParagraphAlignment.RIGHT);
        addRun(generated, "Generated: " + LocalDateTime.now().format(DATE_TIME), false, 9, TEXT_SECONDARY);

        XWPFParagraph divider = document.createParagraph();
        divider.setBorderBottom(org.apache.poi.xwpf.usermodel.Borders.SINGLE);
        divider.setSpacingAfter(180);
    }

    private void addSummary(XWPFDocument document, List<Guest> guests, ReportRange range) {
        long current = guests.stream().filter(this::currentlyStaying).count();
        long upcoming = guests.stream().filter(this::upcoming).count();
        long departed = guests.stream().filter(this::departed).count();

        XWPFTable table = document.createTable(1, 4);
        table.setWidth("100%");
        table.setTableAlignment(TableRowAlign.CENTER);
        removeBorders(table);
        XWPFTableRow row = table.getRow(0);
        summaryCell(row.getCell(0), "Total Guests", String.valueOf(guests.size()));
        summaryCell(row.getCell(1), "Currently Staying", String.valueOf(current));
        summaryCell(row.getCell(2), "Upcoming", String.valueOf(upcoming));
        summaryCell(row.getCell(3), "Departed", String.valueOf(departed));

        XWPFParagraph note = document.createParagraph();
        note.setSpacingBefore(120);
        note.setSpacingAfter(120);
        addRun(note, "Report scope: guests with arrival dates from " + DATE.format(range.startDate())
                + " through " + DATE.format(range.endDate()) + ".", false, 9, TEXT_SECONDARY);
    }

    private void addGuestTable(XWPFDocument document, List<Guest> guests) {
        String[] columns = {
                "Guest Name", "CNIC", "Category", "Department", "Accommodation", "Room",
                "Arrival", "Departure", "Status", "Tenure"
        };

        XWPFTable table = document.createTable(Math.max(guests.size() + 1, 2), columns.length);
        table.setWidth("100%");
        table.setTableAlignment(TableRowAlign.CENTER);

        XWPFTableRow header = table.getRow(0);
        for (int index = 0; index < columns.length; index++) {
            XWPFTableCell cell = header.getCell(index);
            setCellText(cell, columns[index], true, 8, "FFFFFF");
            shade(cell, PRIMARY);
        }

        if (guests.isEmpty()) {
            XWPFTableRow row = table.getRow(1);
            for (int index = 0; index < columns.length; index++) {
                setCellText(row.getCell(index), index == 0 ? "No guest records found for this period." : "", false, 8, TEXT_SECONDARY);
            }
            return;
        }

        for (int i = 0; i < guests.size(); i++) {
            Guest guest = guests.get(i);
            XWPFTableRow row = table.getRow(i + 1);
            String[] values = {
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
            for (int column = 0; column < values.length; column++) {
                XWPFTableCell cell = row.getCell(column);
                setCellText(cell, values[column], false, 7, TEXT_PRIMARY);
                if (i % 2 == 1) {
                    shade(cell, "F7F9FB");
                }
            }
        }
    }

    private void summaryCell(XWPFTableCell cell, String label, String value) {
        clearCell(cell);
        shade(cell, HEADER_FILL);
        XWPFParagraph valueLine = cell.addParagraph();
        valueLine.setAlignment(ParagraphAlignment.CENTER);
        addRun(valueLine, value, true, 17, PRIMARY);
        XWPFParagraph labelLine = cell.addParagraph();
        labelLine.setAlignment(ParagraphAlignment.CENTER);
        addRun(labelLine, label, false, 8, TEXT_SECONDARY);
    }

    private boolean currentlyStaying(Guest guest) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime arrival = localDateTime(guest.getArrivalAt());
        LocalDateTime departure = localDateTime(guest.getDepartureAt());
        return arrival != null && departure != null && !arrival.isAfter(now) && departure.isAfter(now);
    }

    private boolean upcoming(Guest guest) {
        LocalDateTime arrival = localDateTime(guest.getArrivalAt());
        return arrival != null && arrival.isAfter(LocalDateTime.now());
    }

    private boolean departed(Guest guest) {
        LocalDateTime departure = localDateTime(guest.getDepartureAt());
        return departure != null && !departure.isAfter(LocalDateTime.now());
    }

    private String status(Guest guest) {
        if (currentlyStaying(guest)) {
            return "Currently Staying";
        }
        if (upcoming(guest)) {
            return "Upcoming";
        }
        if (departed(guest)) {
            return "Departed";
        }
        return "Unknown";
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

    private void addRun(XWPFParagraph paragraph, String text, boolean bold, int fontSize, String color) {
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setFontSize(fontSize);
        run.setFontFamily("Segoe UI");
        run.setColor(color);
    }

    private void setCellText(XWPFTableCell cell, String text, boolean bold, int fontSize, String color) {
        clearCell(cell);
        XWPFParagraph paragraph = cell.addParagraph();
        paragraph.setSpacingBefore(40);
        paragraph.setSpacingAfter(40);
        addRun(paragraph, text, bold, fontSize, color);
    }

    private void clearCell(XWPFTableCell cell) {
        for (int i = cell.getParagraphs().size() - 1; i >= 0; i--) {
            cell.removeParagraph(i);
        }
    }

    private void shade(XWPFTableCell cell, String fill) {
        CTTcPr properties = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        CTShd shading = properties.isSetShd() ? properties.getShd() : properties.addNewShd();
        shading.setVal(STShd.CLEAR);
        shading.setFill(fill);
    }

    private void setCellWidth(XWPFTableCell cell, int width) {
        CTTcPr properties = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        properties.addNewTcW().setW(BigInteger.valueOf(width));
    }

    private void removeBorders(XWPFTable table) {
        CTTblPr properties = table.getCTTbl().getTblPr();
        if (properties == null) {
            properties = table.getCTTbl().addNewTblPr();
        }
        if (properties.isSetTblBorders()) {
            properties.unsetTblBorders();
        }
    }

    public record ReportRange(String label, LocalDate startDate, LocalDate endDate) {
    }
}
