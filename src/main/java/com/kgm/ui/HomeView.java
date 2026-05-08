package com.kgm.ui;
import com.kgm.dao.DashboardDao;
import com.kgm.database.DatabaseInitializer;
import com.kgm.service.ExcelImportService;
import com.kgm.service.GuestReportService;
import com.kgm.ui.dialog.DelayedProgressDialog;
import com.kgm.ui.dialog.ReportPeriodDialog;
import com.kgm.ui.dialog.ReportProgressDialog;
import com.kgm.ui.panel.AccommodationManagementPanel;
import com.kgm.ui.panel.DepartmentAnalysisGraphPanel;
import com.kgm.ui.panel.GuestFilterPanel;
import com.kgm.ui.panel.GuestDetailsPanel;
import com.kgm.ui.panel.GuestRecordPanel;
import com.kgm.ui.panel.FooterPanel;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.panel.HomeKpiPanel;
import com.kgm.ui.panel.HouseOccupancyGraphPanel;
import com.kgm.ui.panel.UniversalGraphPanel;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.HomeViewHelper;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;
public class HomeView extends JFrame {
    private static final int DASHBOARD_TAB = 0;
    private static final int ADD_GUEST_TAB = 1;
    // KPI bottom margin; adjust this value to tune space below KPI cards.
    private static final int KPI_BOTTOM_MARGIN = 36;
    private static final int DASHBOARD_REFRESH_DELAY_MS = 5000;
    private static final String DASHBOARD_SCREEN = "dashboard";
    private static final String GUEST_DETAILS_SCREEN = "guestDetails";
    private static final DateTimeFormatter REPORT_FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private GuestFilterPanel guestFilterPanel;
    private GuestRecordPanel guestRecordPanel;
    private HomeKpiPanel homeKpiPanel;
    private final DashboardDao dashboardDao = new DashboardDao();
    private final ExcelImportService excelImportService = new ExcelImportService();
    private final GuestReportService guestReportService = new GuestReportService();
    private JPanel dashboardScreens;
    private JPanel dashboardPage;
    private Component guestDetailsScreen;
    private JTabbedPane mainTabs;
    private boolean refreshingAddGuestTab;
    private JButton importExcelButton;
    private Timer dashboardStatsTimer;
    private SwingWorker<DashboardDao.DashboardStats, Void> dashboardStatsWorker;
    public HomeView() {
        DatabaseInitializer.init();
        setTitle("Guest Management Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(HomeViewHelper.PAGE_BACKGROUND);
        root.add(new HeaderPanel("Guest Management Dashboard"), BorderLayout.NORTH);
        root.add(createBody(), BorderLayout.CENTER);
        root.add(new FooterPanel(), BorderLayout.SOUTH);
        setContentPane(root);
        pack();
        setMinimumSize(new Dimension(980, 680));
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setVisible(true);
        startDashboardStatsTimer();
    }
    private JTabbedPane createBody() {
        JTabbedPane tabs = new JTabbedPane();
        mainTabs = tabs;
        HomeViewHelper.styleTabs(tabs);
        tabs.addTab("Dashboard", createHomeTab(tabs));
        tabs.addTab("Add Guest", createAddGuestTab(tabs));
        tabs.addTab("Accommodations", new AccommodationManagementPanel(this::showDashboardTab));
        tabs.addChangeListener(event -> handleTabChanged(tabs));
        tabs.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                int tabIndex = tabs.indexAtLocation(event.getX(), event.getY());
                if (tabIndex == DASHBOARD_TAB) {
                    showDashboardTab();
                }
            }
        });
        return tabs;
    }
    private JComponent createAddGuestTab(JTabbedPane tabs) {
        return AddGuest.createContent(this::showDashboardTab);
    }
    private void handleTabChanged(JTabbedPane tabs) {
        int selectedIndex = tabs.getSelectedIndex();
        if (selectedIndex == DASHBOARD_TAB) {
            showDashboard();
            startDashboardStatsTimer();
            return;
        }
        stopDashboardStatsTimer();
        if (selectedIndex == ADD_GUEST_TAB) {
            refreshAddGuestTab(tabs);
        }
    }
    private void refreshAddGuestTab(JTabbedPane tabs) {
        if (refreshingAddGuestTab) {
            return;
        }
        refreshingAddGuestTab = true;
        try {
            tabs.setComponentAt(ADD_GUEST_TAB, createAddGuestTab(tabs));
        } finally {
            refreshingAddGuestTab = false;
        }
    }
    private JComponent createHomeTab(JTabbedPane tabs) {
        dashboardScreens = new JPanel(new CardLayout());
        dashboardScreens.setBackground(Color.WHITE);
        dashboardScreens.add(createDashboardScreen(tabs), DASHBOARD_SCREEN);
        return dashboardScreens;
    }
    private JComponent createDashboardScreen(JTabbedPane tabs) {
        dashboardPage = HomeViewHelper.pagePanel();
        populateDashboardPage(tabs);
        JScrollPane scroll = new JScrollPane(dashboardPage);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }
    private void populateDashboardPage(JTabbedPane tabs) {
        if (dashboardPage == null) {
            return;
        }
        dashboardPage.removeAll();
        guestRecordPanel = new GuestRecordPanel(this::showGuestDetails, this::showReportDialog);
        guestFilterPanel = new GuestFilterPanel(
                this::performSearch,
                this::clearSearch
        );
        GridBagConstraints gbc = HomeViewHelper.pageConstraints(0);
        gbc.insets = new Insets(0, 0, KPI_BOTTOM_MARGIN, 0);
        homeKpiPanel = new HomeKpiPanel(loadDashboardStats());
        dashboardPage.add(homeKpiPanel, gbc);
        gbc = HomeViewHelper.pageConstraints(1);
        dashboardPage.add(createImportActionsRow(), gbc);
        gbc = HomeViewHelper.pageConstraints(2);
        dashboardPage.add(guestFilterPanel, gbc);
        gbc = HomeViewHelper.pageConstraints(3);
        dashboardPage.add(guestRecordPanel, gbc);
        gbc = HomeViewHelper.pageConstraints(4);
        dashboardPage.add(createGraphPanel(), gbc);
        gbc = HomeViewHelper.pageConstraints(5);
        gbc.weighty = 1.0;
        dashboardPage.add(Box.createVerticalGlue(), gbc);
        dashboardPage.revalidate();
        dashboardPage.repaint();
    }
    private void showGuestDetails(Object[] guestRecord) {
        if (dashboardScreens == null) {
            return;
        }
        if (guestDetailsScreen != null) {
            dashboardScreens.remove(guestDetailsScreen);
        }
        guestDetailsScreen = new GuestDetailsPanel(guestRecord, this::showDashboard, this::refreshGuestRecords);
        dashboardScreens.add(guestDetailsScreen, GUEST_DETAILS_SCREEN);
        showDashboardCard(GUEST_DETAILS_SCREEN);
    }
    private void showDashboard() {
        refreshDashboard();
        showDashboardCard(DASHBOARD_SCREEN);
    }
    private void showDashboardTab() {
        if (mainTabs != null && mainTabs.getSelectedIndex() != DASHBOARD_TAB) {
            mainTabs.setSelectedIndex(DASHBOARD_TAB);
        }
        showDashboard();
    }
    private void refreshDashboard() {
        populateDashboardPage(null);
        refreshDashboardStatsAsync();
    }
    private void startDashboardStatsTimer() {
        if (!isDashboardTabOpen()) {
            return;
        }
        if (dashboardStatsTimer == null) {
            dashboardStatsTimer = new Timer(DASHBOARD_REFRESH_DELAY_MS, event -> refreshDashboardStatsAsync());
            dashboardStatsTimer.setRepeats(true);
        }
        if (!dashboardStatsTimer.isRunning()) {
            dashboardStatsTimer.start();
        }
        refreshDashboardStatsAsync();
    }
    private void stopDashboardStatsTimer() {
        if (dashboardStatsTimer != null) {
            dashboardStatsTimer.stop();
        }
    }
    private boolean isDashboardTabOpen() {
        return mainTabs != null
                && mainTabs.getSelectedIndex() == DASHBOARD_TAB
                && isDashboardCardOpen();
    }
    private boolean isDashboardCardOpen() {
        return dashboardScreens == null || guestDetailsScreen == null || !guestDetailsScreen.isShowing();
    }
    private void refreshDashboardStatsAsync() {
        if (!isDashboardTabOpen() || homeKpiPanel == null) {
            return;
        }
        if (dashboardStatsWorker != null && !dashboardStatsWorker.isDone()) {
            return;
        }
        DelayedProgressDialog.Handle progress = DelayedProgressDialog.showAfter(
                this,
                "Updating Dashboard",
                "Database is taking longer than usual. Updating dashboard stats..."
        );
        dashboardStatsWorker = new SwingWorker<>() {
            protected DashboardDao.DashboardStats doInBackground() throws Exception {
                return dashboardDao.loadStats();
            }
            protected void done() {
                try {
                    if (isDashboardTabOpen() && homeKpiPanel != null) {
                        homeKpiPanel.updateStats(get());
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    System.err.println("Dashboard KPI refresh failed: "
                            + (cause == null ? exception.getMessage() : cause.getMessage()));
                } finally {
                    progress.done();
                    dashboardStatsWorker = null;
                }
            }
        };
        dashboardStatsWorker.execute();
    }
    private void refreshGuestRecords() {
        if (guestRecordPanel != null) {
            guestRecordPanel.refreshFromDatabaseAsync(false);
        }
    }
    private void showDashboardCard(String name) {
        if (dashboardScreens == null) {
            return;
        }
        CardLayout layout = (CardLayout) dashboardScreens.getLayout();
        layout.show(dashboardScreens, name);
        dashboardScreens.revalidate();
        dashboardScreens.repaint();
    }
    private void performSearch() {
        if (guestFilterPanel == null || guestRecordPanel == null) {
            return;
        }
        guestRecordPanel.search(
                guestFilterPanel.getSearchText(),
                guestFilterPanel.getStatusText(),
                guestFilterPanel.getDateText()
        );
    }
    private void clearSearch() {
        if (guestFilterPanel == null || guestRecordPanel == null) {
            return;
        }
        guestFilterPanel.clearSearch();
        guestRecordPanel.reset();
    }
    private JPanel createGraphPanel() {
        JPanel graphs = new JPanel(new GridLayout(1, 2, 16, 16));
        graphs.setOpaque(false);
        graphs.setPreferredSize(new Dimension(0, 360));
        String[] accommodationCategories = loadAccommodationCategories();
        graphs.add(graphScroll(new HouseOccupancyGraphPanel(
                dashboardDao,
                loadOccupancyChart(defaultOccupancyCategory(accommodationCategories)),
                accommodationCategories
        )));
        graphs.add(graphScroll(new DepartmentAnalysisGraphPanel(loadDepartmentChart())));
        return graphs;
    }
    private JPanel createImportActionsRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        row.setOpaque(false);
        importExcelButton = new JButton("Import Excel");
        importExcelButton.setPreferredSize(new Dimension(126, 32));
        importExcelButton.setBackground(new Color(28, 137, 85));
        importExcelButton.setForeground(Color.WHITE);
        importExcelButton.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        importExcelButton.setFocusPainted(false);
        importExcelButton.setBorderPainted(false);
        importExcelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        importExcelButton.addActionListener(event -> chooseExcelImportFile());
        row.add(importExcelButton);
        return row;
    }
    private void chooseExcelImportFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import Guest Excel Sheet");
        chooser.setFileFilter(new FileNameExtensionFilter("Excel files (*.xlsx, *.xls)", "xlsx", "xls"));
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File selectedFile = chooser.getSelectedFile();
        if (!isExcelImportFile(selectedFile)) {
            showUnsupportedImportFileDialog();
            return;
        }
        importGuestsFromExcel(selectedFile);
    }
    private void importGuestsFromExcel(File file) {
        if (!isExcelImportFile(file)) {
            showUnsupportedImportFileDialog();
            return;
        }
        setImportButtonEnabled(false);
        DelayedProgressDialog.Handle progress = DelayedProgressDialog.showAfter(
                this,
                "Importing Excel",
                "Database is taking longer than usual. Checking and importing guest records..."
        );
        SwingWorker<ExcelImportService.ImportResult, Void> worker = new SwingWorker<>() {
            protected ExcelImportService.ImportResult doInBackground() throws Exception {
                return excelImportService.importGuests(file);
            }
            protected void done() {
                setImportButtonEnabled(true);
                try {
                    ExcelImportService.ImportResult result = get();
                    showImportResult(result);
                    if (result.importedCount() > 0) {
                        refreshDashboard();
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    System.err.println("Excel import interrupted.");
                    DialogHelper.error(HomeView.this, "Excel import stopped", "Import was interrupted.");
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    String message = cause == null ? exception.getMessage() : cause.getMessage();
                    if (cause instanceof ExcelImportService.HeaderImportException) {
                        showHeaderImportError();
                        return;
                    }
                    DialogHelper.error(
                            HomeView.this,
                            "Excel import needs attention",
                            friendlyImportFailureMessage(message)
                    );
                } finally {
                    progress.done();
                }
            }
        };
        worker.execute();
    }
    private boolean isExcelImportFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        String name = file.getName().toLowerCase();
        return (name.endsWith(".xlsx") || name.endsWith(".xls"))
                && !name.startsWith("~$");
    }
    private void showUnsupportedImportFileDialog() {
        DialogHelper.error(
                this,
                "Unsupported import file",
                "Only Excel workbooks can be imported.\nChoose a .xlsx or .xls file and try again."
        );
    }
    private void showHeaderImportError() {
        String friendlyMessage = """
                Header issue
                This file does not match the guest import format.
                Download the sample file, keep the first row unchanged, and paste your guest data below it.
                """;
        int selected = DialogHelper.option(
                this,
                "Excel header needs attention",
                friendlyMessage,
                "Download Sample",
                "Close"
        );
        if (selected == 0) {
            downloadSampleExcel();
        }
    }
    private void downloadSampleExcel() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Guest Import Sample");
        chooser.setSelectedFile(new File("guest_import_sample.xlsx"));
        chooser.setFileFilter(new FileNameExtensionFilter("Excel workbook (*.xlsx)", "xlsx"));
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File target = xlsxFile(chooser.getSelectedFile());
        try {
            ExcelImportService.writeSampleWorkbook(target);
            DialogHelper.success(this, "Sample Excel file saved:\n" + target.getAbsolutePath());
        } catch (IOException exception) {
            DialogHelper.error(this, "Sample not saved", friendlySampleSaveFailure(target, exception));
        } catch (Exception exception) {
            DialogHelper.error(this, "Sample not saved", exception.getMessage());
        }
    }
    private String friendlySampleSaveFailure(File target, IOException exception) {
        String detail = exception.getMessage() == null ? "" : exception.getMessage();
        if (detail.toLowerCase().contains("being used by another process")
                || detail.toLowerCase().contains("process cannot access")
                || detail.toLowerCase().contains("denied")) {
            return "The sample file could not be replaced because it is open or locked.\nClose the existing file, then save the sample again.";
        }
        return "The sample file could not be saved:\n" + target.getAbsolutePath() + "\n\nDetails: " + detail;
    }
    private File xlsxFile(File file) {
        String path = file.getAbsolutePath();
        return path.toLowerCase().endsWith(".xlsx") ? file : new File(path + ".xlsx");
    }
    private void showImportResult(ExcelImportService.ImportResult result) {
        if (result.skippedRows().isEmpty()) {
            DialogHelper.success(this, importSummaryMessage(result, "Import complete"));
        } else {
            DialogHelper.warningSections(
                    this,
                    "Import completed with rows to review",
                    importSummaryMessage(result, "Import result"),
                    skippedRowsMessage(result)
            );
        }
    }
    private String importSummaryMessage(ExcelImportService.ImportResult result, String heading) {
        return heading
                + "\nImported: " + result.importedCount() + " guest" + plural(result.importedCount())
                + "\nNeeds review: " + result.skippedRows().size() + " row" + plural(result.skippedRows().size())
                + (result.skippedRows().isEmpty()
                ? "\nAll readable rows were imported successfully."
                : "");
    }
    private String skippedRowsMessage(ExcelImportService.ImportResult result) {
        StringBuilder message = new StringBuilder();
        message.append("Rows to review\n");
        for (String skippedRow : result.skippedRows()) {
            message.append(friendlySkippedRowMessage(skippedRow)).append("\n");
        }
        return message.toString().trim();
    }
    private String friendlyImportFailureMessage(String message) {
        String detail = message == null || message.isBlank() ? "The file could not be imported." : message.trim();
        if (detail.toLowerCase().contains("open or locked by another process")) {
            return "Excel file is open\nClose the file in Excel, then try the import again.";
        }
        if (detail.toLowerCase().contains("no guest rows found")) {
            return "No guest rows found\nAdd guest records below the header row, then import again.";
        }
        return "The file could not be imported\nMake sure it is a valid Excel workbook and try again.\n\nDetails: "
                + detail;
    }
    private String friendlySkippedRowMessage(String rowMessage) {
        String rowPrefix = rowPrefix(rowMessage);
        String reason = rowReason(rowMessage);
        if (reason.startsWith("Missing required fields:")) {
            String fields = reason.substring("Missing required fields:".length()).trim();
            return rowPrefix + "Add required fields: " + fields;
        }
        if (reason.endsWith(" is required.")) {
            String field = reason.substring(0, reason.length() - " is required.".length());
            return rowPrefix + "Add " + field + ".";
        }
        if (reason.endsWith(" must be a valid date/time.")) {
            String field = reason.substring(0, reason.length() - " must be a valid date/time.".length());
            return rowPrefix + "Use a valid " + field + " using yyyy-MM-dd HH:mm.";
        }
        if (reason.endsWith(" is required and must be a date/time.")) {
            String field = reason.substring(0, reason.length() - " is required and must be a date/time.".length());
            return rowPrefix + "Add a valid " + field + " using yyyy-MM-dd HH:mm.";
        }
        if (reason.equals("CNIC must contain exactly 13 digits.")) {
            return rowPrefix + "CNIC must be exactly 13 digits.";
        }
        if (reason.equals("Departure Date Time must be after Arrival Date Time.")) {
            return rowPrefix + "Departure date must be after arrival date.";
        }
        if (reason.startsWith("Guest name already exists for this arrival date:")) {
            return rowPrefix + "Guest name already exists for this arrival date.";
        }
        if (reason.startsWith("This CNIC already has an overlapping guest stay.")) {
            return rowPrefix + "This CNIC already has an overlapping stay. A guest cannot be assigned to more than one room at the same time.";
        }
        if (reason.startsWith("Accommodation Category not found in DB:")) {
            String category = quotedValue(reason, "Accommodation Category '");
            String room = quotedValue(reason, "Room '");
            return rowPrefix + "Accommodation category '" + valueOrDash(category)
                    + "' is not available for room '" + valueOrDash(room)
                    + "'. Use the Valid Values sheet from the sample file.";
        }
        if (reason.startsWith("Room not found in DB:")) {
            String category = quotedValue(reason, "Accommodation Category '");
            String room = quotedValue(reason, "Room '");
            return rowPrefix + "Room '" + valueOrDash(room)
                    + "' is not available under '" + valueOrDash(category) + "'.";
        }
        if (reason.startsWith("Room is not ready for assignment:")) {
            String category = quotedValue(reason, "Accommodation Category '");
            String room = quotedValue(reason, "Room '");
            return rowPrefix + "Room '" + valueOrDash(room)
                    + "' under '" + valueOrDash(category) + "' is not ready for assignment.";
        }
        return rowPrefix + reason;
    }
    private String rowPrefix(String rowMessage) {
        int separator = rowMessage == null ? -1 : rowMessage.indexOf(':');
        if (separator < 0) {
            return "";
        }
        return rowMessage.substring(0, separator).trim() + ": ";
    }
    private String rowReason(String rowMessage) {
        int separator = rowMessage == null ? -1 : rowMessage.indexOf(':');
        if (separator < 0) {
            return rowMessage == null ? "" : rowMessage.trim();
        }
        return rowMessage.substring(separator + 1).trim();
    }
    private String quotedValue(String text, String marker) {
        int start = text.indexOf(marker);
        if (start < 0) {
            return "";
        }
        start += marker.length();
        int end = text.indexOf("'", start);
        return end < 0 ? "" : text.substring(start, end);
    }
    private String valueOrDash(String text) {
        return text == null || text.isBlank() ? "-" : text;
    }
    private String plural(int count) {
        return count == 1 ? "" : "s";
    }
    private void setImportButtonEnabled(boolean enabled) {
        if (importExcelButton == null) {
            return;
        }
        importExcelButton.setEnabled(enabled);
        importExcelButton.setText(enabled ? "Import Excel" : "Importing...");
        importExcelButton.setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }
    private void showReportDialog() {
        ReportPeriodDialog dialog = new ReportPeriodDialog(this);
        dialog.setVisible(true);
        GuestReportService.ReportRange range = dialog.getSelectedRange();
        if (range == null) {
            return;
        }
        File target = chooseReportTarget(range);
        if (target == null) {
            return;
        }
        if (!confirmReportGeneration(range, target)) {
            return;
        }
        generateGuestReport(range, target);
    }
    private File chooseReportTarget(GuestReportService.ReportRange range) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Download Guest Report");
        chooser.setSelectedFile(new File(defaultReportFileName(range)));
        chooser.setFileFilter(new FileNameExtensionFilter("PDF document (*.pdf)", "pdf"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return pdfFile(chooser.getSelectedFile());
    }
    private String defaultReportFileName(GuestReportService.ReportRange range) {
        String label = range.label().toLowerCase().replaceAll("[^a-z0-9]+", "_");
        return "guest_report_"
                + label
                + "_"
                + range.startDate().format(REPORT_FILE_DATE)
                + "_"
                + range.endDate().format(REPORT_FILE_DATE)
                + ".pdf";
    }
    private File pdfFile(File file) {
        String path = file.getAbsolutePath();
        return path.toLowerCase().endsWith(".pdf") ? file : new File(path + ".pdf");
    }
    private boolean confirmReportGeneration(GuestReportService.ReportRange range, File target) {
        String message = "Report period: " + range.label()
                + " (" + range.startDate() + " to " + range.endDate() + ")\n"
                + "Save path: " + target.getAbsolutePath() + "\n\n"
                + "Generate this report now?";
        return DialogHelper.option(this, "Generate Report", message, "Generate", "Cancel") == 0;
    }
    private void generateGuestReport(GuestReportService.ReportRange range, File target) {
        ReportProgressDialog progress = new ReportProgressDialog(this, range, target);
        SwingWorker<File, Void> worker = new SwingWorker<>() {
            protected File doInBackground() throws Exception {
                return guestReportService.generateReport(target, range);
            }
            protected void done() {
                progress.dispose();
                try {
                    File savedFile = get();
                    showReportSavedDialog(savedFile, sameFilePath(savedFile, target));
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    DialogHelper.error(HomeView.this, "Report generation stopped", "Report generation was interrupted.");
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    String message = cause == null ? exception.getMessage() : cause.getMessage();
                    DialogHelper.error(HomeView.this, "Report generation failed", message);
                }
            }
        };
        progress.setVisible(true);
        worker.execute();
    }
    private void showReportSavedDialog(File savedFile, boolean savedToRequestedPath) {
        String message = savedToRequestedPath
                ? "Your PDF report is ready:\n" + savedFile.getAbsolutePath()
                : "The selected file was open, so a new PDF copy was saved:\n" + savedFile.getAbsolutePath();
        int selected = DialogHelper.successOption(
                this,
                "Report Saved",
                message,
                "Open PDF",
                "Close"
        );
        if (selected == 0) {
            openSavedReport(savedFile);
        }
    }
    private void openSavedReport(File file) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            DialogHelper.warning(
                    this,
                    "Open PDF unavailable",
                    "The report was saved, but this system does not allow the app to open files automatically.\n"
                            + file.getAbsolutePath()
            );
            return;
        }
        try {
            Desktop.getDesktop().open(file);
        } catch (IOException exception) {
            DialogHelper.error(
                    this,
                    "PDF not opened",
                    "The report was saved, but it could not be opened automatically.\n"
                            + file.getAbsolutePath()
                            + "\n\n" + exception.getMessage()
            );
        }
    }
    private boolean sameFilePath(File first, File second) {
        return first.toPath().toAbsolutePath().normalize().equals(second.toPath().toAbsolutePath().normalize());
    }
    private JComponent graphScroll(UniversalGraphPanel graph) {
        JScrollPane scroll = new JScrollPane(graph);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setWheelScrollingEnabled(false);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setBlockIncrement(96);
        scroll.addMouseWheelListener(event -> forwardGraphMouseWheel(event, scroll));
        scroll.getViewport().addMouseWheelListener(event -> forwardGraphMouseWheel(event, scroll));
        installGraphWheelForwarding(graph, scroll);
        return scroll;
    }
    private void installGraphWheelForwarding(Component component, JScrollPane graphScroll) {
        component.addMouseWheelListener(event -> forwardGraphMouseWheel(event, graphScroll));
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                installGraphWheelForwarding(child, graphScroll);
            }
        }
    }
    private void forwardGraphMouseWheel(MouseWheelEvent event, JScrollPane graphScroll) {
        if (event.isShiftDown() && scrollGraphHorizontally(event, graphScroll)) {
            return;
        }
        JScrollPane pageScroll = findPageScrollPane(graphScroll);
        if (pageScroll == null) {
            return;
        }
        MouseWheelEvent pageEvent = new MouseWheelEvent(
                pageScroll,
                event.getID(),
                event.getWhen(),
                event.getModifiersEx(),
                0,
                0,
                event.getXOnScreen(),
                event.getYOnScreen(),
                event.getClickCount(),
                event.isPopupTrigger(),
                event.getScrollType(),
                event.getScrollAmount(),
                event.getWheelRotation(),
                event.getPreciseWheelRotation()
        );
        pageScroll.dispatchEvent(pageEvent);
        event.consume();
    }
    private boolean scrollGraphHorizontally(MouseWheelEvent event, JScrollPane graphScroll) {
        JScrollBar horizontalBar = graphScroll.getHorizontalScrollBar();
        if (horizontalBar == null || !horizontalBar.isVisible()) {
            return false;
        }
        scrollBar(horizontalBar, event);
        event.consume();
        return true;
    }
    private JScrollPane findPageScrollPane(Component component) {
        Container parent = component.getParent();
        while (parent != null) {
            if (parent instanceof JScrollPane scrollPane) {
                return scrollPane;
            }
            parent = parent.getParent();
        }
        return null;
    }
    private void scrollBar(JScrollBar scrollBar, MouseWheelEvent event) {
        int direction = event.getWheelRotation() < 0 ? -1 : 1;
        int amount = event.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL
                ? event.getUnitsToScroll() * scrollBar.getUnitIncrement(direction)
                : event.getWheelRotation() * scrollBar.getBlockIncrement(direction);
        int max = scrollBar.getMaximum() - scrollBar.getVisibleAmount();
        int value = Math.max(scrollBar.getMinimum(), Math.min(max, scrollBar.getValue() + amount));
        scrollBar.setValue(value);
    }
    private DashboardDao.DashboardStats loadDashboardStats() {
        try {
            return dashboardDao.loadStats();
        } catch (SQLException exception) {
            return new DashboardDao.DashboardStats(0, 0, 0, 0, 0, "-");
        }
    }
    private DashboardDao.OccupancyChartData loadOccupancyChart(String category) {
        try {
            return dashboardDao.loadOccupancyChart(category);
        } catch (SQLException exception) {
            return new DashboardDao.OccupancyChartData(new String[0], new int[0], new int[0]);
        }
    }
    private String[] loadAccommodationCategories() {
        try {
            return dashboardDao.loadAccommodationCategories();
        } catch (SQLException exception) {
            return new String[0];
        }
    }
    private String defaultOccupancyCategory(String[] categories) {
        return categories.length == 0 ? "" : categories[0];
    }
    private DashboardDao.DepartmentChartData loadDepartmentChart() {
        try {
            return dashboardDao.loadDepartmentChart();
        } catch (SQLException exception) {
            return new DashboardDao.DepartmentChartData(new String[0], new int[0]);
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(HomeView::new);
    }
}
