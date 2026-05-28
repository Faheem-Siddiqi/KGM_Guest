package com.kgm.service;

import com.kgm.dao.GuestDao;
import com.kgm.model.Guest;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GuestReportService {
    private final GuestDao guestDao = new GuestDao();
    private final GuestPdfReportGenerator pdfReportGenerator = new GuestPdfReportGenerator();
    private final GuestExcelReportGenerator excelReportGenerator = new GuestExcelReportGenerator();

    public File generateReport(File target, ReportRange range) throws Exception {
        List<Guest> guests = guestsFor(range);
        return pdfReportGenerator.write(target, range, guests);
    }

    public ReportExportResult generateReports(ReportExportRequest request, ReportProgressListener listener)
            throws Exception {
        ReportProgressListener progress = listener == null ? stage -> {
        } : listener;
        if (request == null || !request.hasFormat()) {
            throw new IllegalArgumentException("Select at least one report format.");
        }

        progress.onProgress("Scanning guest rows for the selected period...");
        List<Guest> guests = guestsFor(request.range());

        List<File> files = new ArrayList<>();
        String baseName = reportBaseName(request.range());
        File saveTarget = reportSaveTarget(request, baseName);

        if (request.includeExcel() && request.includePdf()) {
            progress.onProgress("Creating report folder...");
            File folder = createReportFolder(saveTarget);
            progress.onProgress("Preparing Excel report...");
            File excel = new File(folder, baseName + ".xlsx");
            files.add(excelReportGenerator.write(excel, request.range(), guests));
            progress.onProgress("Preparing PDF report...");
            File pdf = new File(folder, baseName + ".pdf");
            files.add(pdfReportGenerator.write(pdf, request.range(), guests));
            progress.onProgress("Finalizing report files...");
            return new ReportExportResult(folder, files);
        }

        if (request.includeExcel()) {
            progress.onProgress("Preparing Excel report...");
            File excel = withExtension(saveTarget, ".xlsx");
            files.add(excelReportGenerator.write(excel, request.range(), guests));
        }

        if (request.includePdf()) {
            progress.onProgress("Preparing PDF report...");
            File pdf = withExtension(saveTarget, ".pdf");
            files.add(pdfReportGenerator.write(pdf, request.range(), guests));
        }

        progress.onProgress("Finalizing report files...");
        File firstFile = files.isEmpty() ? saveTarget : files.get(0);
        return new ReportExportResult(parentDirectory(firstFile), files);
    }

    private List<Guest> guestsFor(ReportRange range) throws Exception {
        return guestDao.findByStayOverlapRange(
                dateAtStart(range.startDate()),
                dateAfter(range.endDate())
        );
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

    private Date dateAtStart(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date dateAfter(LocalDate date) {
        return Date.from(date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public record ReportRange(String label, LocalDate startDate, LocalDate endDate) {
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
