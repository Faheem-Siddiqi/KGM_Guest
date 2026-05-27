package com.kgm.ui;

import com.kgm.dao.DashboardDao;
import com.kgm.database.DatabaseInitializer;
import com.kgm.service.ExcelImportService;
import com.kgm.service.ExcelSampleGenerator;
import com.kgm.service.GuestReportService;
import com.kgm.ui.dialog.DelayedProgressDialog;
import com.kgm.ui.dialog.ImportProgressDialog;
import com.kgm.ui.dialog.ReportPeriodDialog;
import com.kgm.ui.panel.AccommodationListViewPanel;
import com.kgm.ui.panel.AccommodationManagementPanel;
import com.kgm.ui.panel.AccommodationRecord;
import com.kgm.ui.panel.DepartmentAnalysisGraphPanel;
import com.kgm.ui.panel.GuestFilterPanel;
import com.kgm.ui.panel.GuestDetailsPanel;
import com.kgm.ui.panel.GuestRecordPanel;
import com.kgm.ui.panel.FooterPanel;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.panel.HomeKpiPanel;
import com.kgm.ui.panel.HouseOccupancyGraphPanel;
import com.kgm.ui.panel.KPICategoryPanel;
import com.kgm.ui.panel.RoomDetailPagePanel;
import com.kgm.ui.panel.UniversalGraphPanel;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.HomeViewHelper;
import com.kgm.ui.util.FileDialogHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
public class HomeView extends JFrame {
    private static final int DASHBOARD_TAB = 0;
    private static final int ADD_GUEST_TAB = 1;
    private static final int KPI_BOTTOM_MARGIN = 18;
    private static final int DASHBOARD_REFRESH_DELAY_MS = 5000;
    private static final int GRAPH_SCROLL_HEIGHT = 430;
    private static final String DASHBOARD_SCREEN = "dashboard";
    private static final String GUEST_DETAILS_SCREEN = "guestDetails";
    private static final String ACCOMMODATION_LIST_SCREEN = "accommodationList";
    private static final String ROOM_DETAILS_SCREEN = "roomDetails";
    private static final String HIDDEN_HOUSE_CAPACITY_CATEGORY = "Security Block";
    private static final DateTimeFormatter REPORT_FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private GuestFilterPanel guestFilterPanel;
    private GuestRecordPanel guestRecordPanel;
    private HomeKpiPanel homeKpiPanel;
    private final DashboardDao dashboardDao = new DashboardDao();
    private final DashboardDao houseCapacityDashboardDao = new DashboardDao() {
        @Override
        public OccupancyChartData loadOccupancyChart(String category) throws SQLException {
            return loadHouseCapacityOccupancyChart(category);
        }
    };
    private final ExcelImportService excelImportService = new ExcelImportService();
    private final GuestReportService guestReportService = new GuestReportService();
    private JPanel dashboardScreens;
    private JPanel dashboardPage;
    private Component guestDetailsScreen;
    private Component accommodationListScreen;
    private Component roomDetailsScreen;
    private AccommodationRecord selectedAccommodationFromList;
    private String dashboardCard = DASHBOARD_SCREEN;
    private JTabbedPane mainTabs;
    private boolean refreshingAddGuestTab;
    private JButton importExcelButton;
    private Timer dashboardStatsTimer;
    private SwingWorker<List<DashboardDao.CategoryKpiStats>, Void> dashboardStatsWorker;
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
        setMinimumSize(new Dimension(760, 620));
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
        } else if (selectedIndex == 2) { // Accommodations tab
            refreshAccommodationsTab(tabs);
        }
    }
    
    private void refreshAccommodationsTab(JTabbedPane tabs) {
        // Replace the accommodations tab with a fresh instance to trigger reload
        Component accommodationsTab = tabs.getComponentAt(2);
        if (accommodationsTab instanceof AccommodationManagementPanel) {
            // Create new panel which will trigger async data loading
            AccommodationManagementPanel newPanel = new AccommodationManagementPanel(this::showDashboardTab);
            tabs.setComponentAt(2, newPanel);
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
        
        // Create filter panel and import actions first (no data loading needed)
        guestFilterPanel = new GuestFilterPanel(
                this::performSearch,
                this::clearSearch
        );
        
        // Create placeholder KPI panel with loading state
        GridBagConstraints gbc = HomeViewHelper.pageConstraints(0);
        gbc.insets = new Insets(0, 0, KPI_BOTTOM_MARGIN, 0);
        homeKpiPanel = new HomeKpiPanel(new DashboardDao.DashboardStats(0, 0, 0, 0, 0, 0, "Loading..."));
        homeKpiPanel.showCategoryLoading();
        dashboardPage.add(homeKpiPanel, gbc);
        
        gbc = HomeViewHelper.pageConstraints(1);
        dashboardPage.add(createImportActionsRow(), gbc);
        
        gbc = HomeViewHelper.pageConstraints(2);
        dashboardPage.add(guestFilterPanel, gbc);
        
        // Create placeholder guest record panel (will be populated asynchronously)
        guestRecordPanel = new GuestRecordPanel(this::showGuestDetails, this::showReportDialog);
        gbc = HomeViewHelper.pageConstraints(3);
        dashboardPage.add(guestRecordPanel, gbc);
        
        // Create placeholder graph panel (will be populated asynchronously)
        gbc = HomeViewHelper.pageConstraints(4);
        dashboardPage.add(createPlaceholderGraphPanel(), gbc);
        
        gbc = HomeViewHelper.pageConstraints(5);
        gbc.weighty = 1.0;
        dashboardPage.add(Box.createVerticalGlue(), gbc);
        dashboardPage.revalidate();
        dashboardPage.repaint();
        
        // Now load all data asynchronously with progress indication
        refreshAllDashboardDataAsync();
    }
    
    private void refreshAllDashboardDataAsync() {
        DelayedProgressDialog.Handle progress = DelayedProgressDialog.showAfter(
                this,
                "Loading Dashboard",
                "Database is taking longer than usual. Loading dashboard data..."
        );
        
        new SwingWorker<DashboardData, Void>() {
            @Override
            protected DashboardData doInBackground() throws Exception {
                List<DashboardDao.CategoryKpiStats> categoryStats = dashboardDao.loadCategoryKpiStats(true);
                String[] categories = visibleHouseCapacityCategories(dashboardDao.loadAccommodationCategories());
                DashboardDao.OccupancyChartData occupancyData = loadHouseCapacityOccupancyChart(
                        defaultOccupancyCategory(categories)
                );
                DashboardDao.DepartmentChartData departmentData = dashboardDao.loadDepartmentChart();
                return new DashboardData(categoryStats, occupancyData, categories, departmentData);
            }
            
            @Override
            protected void done() {
                try {
                    DashboardData data = get();
                    if (isDashboardTabOpen()) {
                        if (homeKpiPanel != null) {
                            homeKpiPanel.updateCategoryStats(data.categoryStats());
                        }

                        JPanel graphs = createGraphPanelWithData(data.occupancyData(), data.categories(), data.departmentData());
                        Component[] components = dashboardPage.getComponents();
                        for (int i = 0; i < components.length; i++) {
                            if (components[i] instanceof JPanel && ((JPanel) components[i]).getClientProperty("placeholder_graph") != null) {
                                dashboardPage.remove(i);
                                GridBagConstraints gbc = HomeViewHelper.pageConstraints(i);
                                dashboardPage.add(graphs, gbc);
                                break;
                            }
                        }
                        
                        dashboardPage.revalidate();
                        dashboardPage.repaint();
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    System.err.println("Dashboard data refresh failed: " 
                            + (cause == null ? exception.getMessage() : cause.getMessage()));
                } finally {
                    progress.done();
                }
            }
        }.execute();
    }
    
    private JPanel createPlaceholderGraphPanel() {
        JPanel placeholder = new JPanel(new BorderLayout());
        placeholder.setOpaque(false);
        placeholder.setPreferredSize(new Dimension(0, GRAPH_SCROLL_HEIGHT));
        placeholder.putClientProperty("placeholder_graph", true);
        JLabel loadingLabel = new JLabel("Loading graphs...", SwingConstants.CENTER);
        loadingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        loadingLabel.setForeground(HomeViewHelper.TEXT_SECONDARY);
        placeholder.add(loadingLabel, BorderLayout.CENTER);
        return placeholder;
    }
    
    private JPanel createGraphPanelWithData(DashboardDao.OccupancyChartData occupancyData, 
                                            String[] categories, 
                                            DashboardDao.DepartmentChartData departmentData) {
        JPanel graphs = new JPanel();
        graphs.setLayout(new BoxLayout(graphs, BoxLayout.Y_AXIS));
        graphs.setOpaque(false);
        graphs.setPreferredSize(new Dimension(0, GRAPH_SCROLL_HEIGHT * 2 + 16));
        JComponent houseGraph = graphScroll(new HouseOccupancyGraphPanel(houseCapacityDashboardDao, occupancyData, categories));
        JComponent departmentGraph = graphScroll(new DepartmentAnalysisGraphPanel(departmentData));
        houseGraph.setAlignmentX(Component.LEFT_ALIGNMENT);
        departmentGraph.setAlignmentX(Component.LEFT_ALIGNMENT);
        graphs.add(houseGraph);
        graphs.add(Box.createVerticalStrut(16));
        graphs.add(departmentGraph);
        return graphs;
    }
    
    // Data container for async loading
    private record DashboardData(
            List<DashboardDao.CategoryKpiStats> categoryStats,
            DashboardDao.OccupancyChartData occupancyData,
            String[] categories,
            DashboardDao.DepartmentChartData departmentData
    ) {}
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
        return DASHBOARD_SCREEN.equals(dashboardCard);
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
            protected List<DashboardDao.CategoryKpiStats> doInBackground() throws Exception {
                return dashboardDao.loadCategoryKpiStats(true);
            }
            protected void done() {
                try {
                    if (isDashboardTabOpen() && homeKpiPanel != null) {
                        homeKpiPanel.updateCategoryStats(get());
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
        dashboardCard = name;
        CardLayout layout = (CardLayout) dashboardScreens.getLayout();
        layout.show(dashboardScreens, name);
        if (ACCOMMODATION_LIST_SCREEN.equals(name)) {
            refreshAccommodationListFromDashboard();
        }
        dashboardScreens.revalidate();
        dashboardScreens.repaint();
    }
    private void showAccommodationList(KPICategoryPanel.MetricSelection selection) {
        if (selection == null || dashboardScreens == null) {
            return;
        }
        if (accommodationListScreen != null) {
            dashboardScreens.remove(accommodationListScreen);
        }
        accommodationListScreen = new AccommodationListViewPanel(
                selection,
                this::showDashboard,
                this::showRoomDetailsFromAccommodationList
        );
        dashboardScreens.add(accommodationListScreen, ACCOMMODATION_LIST_SCREEN);
        showDashboardCard(ACCOMMODATION_LIST_SCREEN);
    }
    private void showRoomDetailsFromAccommodationList(AccommodationRecord accommodation) {
        if (accommodation == null || dashboardScreens == null) {
            return;
        }
        selectedAccommodationFromList = accommodation;
        if (roomDetailsScreen != null) {
            dashboardScreens.remove(roomDetailsScreen);
        }
        roomDetailsScreen = new RoomDetailPagePanel(
                accommodation,
                () -> showDashboardCard(ACCOMMODATION_LIST_SCREEN),
                this::showGuestDetailsFromRoomDetails
        );
        dashboardScreens.add(roomDetailsScreen, ROOM_DETAILS_SCREEN);
        showDashboardCard(ROOM_DETAILS_SCREEN);
    }
    private void showGuestDetailsFromRoomDetails(Object[] guestRecord) {
        if (dashboardScreens == null) {
            return;
        }
        if (guestDetailsScreen != null) {
            dashboardScreens.remove(guestDetailsScreen);
        }
        guestDetailsScreen = new GuestDetailsPanel(
                guestRecord,
                () -> showDashboardCard(ROOM_DETAILS_SCREEN),
                this::refreshRoomDetailsFromAccommodationList
        );
        dashboardScreens.add(guestDetailsScreen, GUEST_DETAILS_SCREEN);
        showDashboardCard(GUEST_DETAILS_SCREEN);
    }
    private void refreshRoomDetailsFromAccommodationList() {
        refreshAccommodationListFromDashboard();
        if (roomDetailsScreen instanceof RoomDetailPagePanel roomDetailPagePanel) {
            roomDetailPagePanel.refreshData();
            return;
        }
        if (selectedAccommodationFromList != null) {
            showRoomDetailsFromAccommodationList(selectedAccommodationFromList);
        }
    }
    private void refreshAccommodationListFromDashboard() {
        if (accommodationListScreen instanceof AccommodationListViewPanel accommodationListViewPanel) {
            accommodationListViewPanel.refreshData();
        }
    }
    private void performSearch() {
        if (guestFilterPanel == null || guestRecordPanel == null) {
            return;
        }
        guestRecordPanel.search(
                guestFilterPanel.getSearchText(),
                guestFilterPanel.getStatusText(),
                guestFilterPanel.getDateRange()
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
        JPanel graphs = new JPanel();
        graphs.setLayout(new BoxLayout(graphs, BoxLayout.Y_AXIS));
        graphs.setOpaque(false);
        String[] accommodationCategories = loadAccommodationCategories();
        JComponent houseGraph = graphScroll(new HouseOccupancyGraphPanel(
                houseCapacityDashboardDao,
                loadOccupancyChart(defaultOccupancyCategory(accommodationCategories)),
                accommodationCategories
        ));
        JComponent departmentGraph = graphScroll(new DepartmentAnalysisGraphPanel(loadDepartmentChart()));
        houseGraph.setAlignmentX(Component.LEFT_ALIGNMENT);
        departmentGraph.setAlignmentX(Component.LEFT_ALIGNMENT);
        graphs.add(houseGraph);
        graphs.add(Box.createVerticalStrut(16));
        graphs.add(departmentGraph);
        return graphs;
    }
    private JPanel createImportActionsRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        row.setOpaque(false);
        importExcelButton = new JButton("Import Excel");
        importExcelButton.setPreferredSize(new Dimension(126, 32));
        importExcelButton.setBackground(HomeViewHelper.PRIMARY);
        importExcelButton.setForeground(Color.WHITE);
        importExcelButton.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        importExcelButton.setFocusPainted(false);
        importExcelButton.setBorderPainted(false);
        importExcelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        importExcelButton.addActionListener(event -> showExcelImportOptions());
        row.add(importExcelButton);
        return row;
    }
    private void showExcelImportOptions() {
        int selected = DialogHelper.option(
                this,
                "Excel Import",
                "Choose an import action\nDownload the sample workbook if you need the correct Excel format, or import a completed guest workbook.",
                "Import Excel",
                "Download Sample"
        );
        if (selected == 0) {
            chooseExcelImportFile();
        } else if (selected == 1) {
            downloadSampleExcel();
        }
    }
    private void chooseExcelImportFile() {
        FileDialogHandler.FileDialogConfig config = new FileDialogHandler.FileDialogConfig()
                .withParent(this)
                .withTitle("Import Guest Excel Sheet")
                .withFileType(FileDialogHandler.FileType.EXCEL);

        FileDialogHandler.openFileDialog(config, selectedFiles -> {
            if (selectedFiles.length == 0) {
                return;
            }
            File selectedFile = selectedFiles[0];
            if (!isExcelImportFile(selectedFile)) {
                showUnsupportedImportFileDialog();
                return;
            }
            ExcelImportService.ImportType importType = chooseExcelImportType();
            if (importType == null) {
                return;
            }
            importGuestsFromExcel(selectedFile, importType);
        });
    }
    private ExcelImportService.ImportType chooseExcelImportType() {
        JRadioButton standardImport = new JRadioButton("Import New / Standard Data", true);
        JRadioButton legacyImport = new JRadioButton("Import Legacy / Historical Data");
        ButtonGroup group = new ButtonGroup();
        group.add(standardImport);
        group.add(legacyImport);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel("Choose how this Excel workbook should be imported:"));
        panel.add(Box.createVerticalStrut(10));
        panel.add(standardImport);
        panel.add(new JLabel("Apply Add Guest rules: required fields, CNIC, overlap, capacity, and room status."));
        panel.add(Box.createVerticalStrut(8));
        panel.add(legacyImport);
        panel.add(new JLabel("Only require dates, accommodation category, and room. No duplicate, overlap, capacity, or status checks."));

        int selected = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Select Import Type",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (selected != JOptionPane.OK_OPTION) {
            return null;
        }
        return legacyImport.isSelected()
                ? ExcelImportService.ImportType.LEGACY
                : ExcelImportService.ImportType.STANDARD;
    }
    private void importGuestsFromExcel(File file, ExcelImportService.ImportType importType) {
        if (!isExcelImportFile(file)) {
            showUnsupportedImportFileDialog();
            return;
        }
        setImportButtonEnabled(false);
        ImportProgressDialog progress = new ImportProgressDialog(this, file, importType.label());
        SwingWorker<ExcelImportService.ImportResult, ExcelImportService.ImportProgress> worker = new SwingWorker<>() {
            protected ExcelImportService.ImportResult doInBackground() throws Exception {
                return excelImportService.importGuests(file, importType, progressEvent -> publish(progressEvent));
            }
            protected void process(List<ExcelImportService.ImportProgress> chunks) {
                if (!chunks.isEmpty()) {
                    progress.updateProgress(chunks.get(chunks.size() - 1));
                }
            }
            protected void done() {
                progress.close();
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
                }
            }
        };
        worker.execute();
        progress.open();
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
        FileDialogHandler.FileDialogConfig config = new FileDialogHandler.FileDialogConfig()
                .withParent(this)
                .withTitle("Save Guest Import Sample")
                .withFileType(FileDialogHandler.FileType.EXCEL)
                .withDefaultFileName("guest_import_sample.xlsx");

        FileDialogHandler.saveFileDialog(config, selectedFile -> {
            File target = xlsxFile(selectedFile);
            try {
                ExcelSampleGenerator.writeSampleWorkbook(target);
                showDownloadedFileDialog(
                        target,
                        "Sample Downloaded",
                        "Sample Excel file downloaded:\n" + target.getAbsolutePath(),
                        "Open Excel"
                );
            } catch (IOException exception) {
                DialogHelper.error(this, "Sample not saved", friendlySampleSaveFailure(target, exception));
            } catch (Exception exception) {
                DialogHelper.error(this, "Sample not saved", exception.getMessage());
            }
        });
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
        if (reason.equals("CNIC must contain exactly 13 digits.")
                || reason.equals("Guest CNIC must contain exactly 13 digits.")) {
            return rowPrefix + "CNIC must be exactly 13 digits.";
        }
        if (reason.equals("Arrival and departure dates are required.")) {
            return rowPrefix + "Add valid arrival and departure dates using yyyy-MM-dd HH:mm.";
        }
        if (reason.equals("Departure Date Time must be after Arrival Date Time.")
                || reason.equals("Departure date must be after arrival date.")) {
            return rowPrefix + "Departure date must be after arrival date.";
        }
        if (reason.startsWith("Guest CNIC already exists for this arrival date:")) {
            return rowPrefix + "Guest CNIC already exists for this arrival date.";
        }
        if (reason.startsWith("Guest already exists for this guest name, arrival date, departure date, and guest category.")) {
            return rowPrefix + "Guest already exists.";
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
        if (reason.startsWith("No ready room is available for ")) {
            return rowPrefix + reason;
        }
        if (reason.equals("Assigned room was not found in the selected accommodation category.")) {
            return rowPrefix + "Room must already exist under the selected accommodation category.";
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
        final File[] result = new File[1];
        FileDialogHandler.FileDialogConfig config = new FileDialogHandler.FileDialogConfig()
                .withParent(this)
                .withTitle("Download Guest Report")
                .withFileType(FileDialogHandler.FileType.PDF)
                .withDefaultFileName(defaultReportFileName(range));

        FileDialogHandler.saveFileDialog(config, selectedFile -> {
            result[0] = pdfFile(selectedFile);
        });
        return result[0];
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
        DelayedProgressDialog.Handle progress = DelayedProgressDialog.showAfter(
                this,
                "Generating Report",
                "Preparing PDF report and saving the file...",
                0
        );
        SwingWorker<File, Void> worker = new SwingWorker<>() {
            protected File doInBackground() throws Exception {
                return guestReportService.generateReport(target, range);
            }
            protected void done() {
                progress.done();
                try {
                    File savedFile = get();
                    showReportSavedDialog(savedFile, sameFilePath(savedFile, target));
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    DialogHelper.error(HomeView.this, "Report generation stopped", "Report generation was interrupted.");
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    String message = reportFailureMessage(cause == null ? exception : cause);
                    DialogHelper.error(HomeView.this, "Report generation failed", message);
                }
            }
        };
        worker.execute();
    }
    private String reportFailureMessage(Throwable failure) {
        String message = failure == null ? null : failure.getMessage();
        if (message == null || message.isBlank()) {
            return "The report could not be generated. Please try again.";
        }
        return message;
    }
    private void showReportSavedDialog(File savedFile, boolean savedToRequestedPath) {
        String message = savedToRequestedPath
                ? "PDF report downloaded:\n" + savedFile.getAbsolutePath()
                : "The selected file was open, so a new PDF copy was downloaded:\n" + savedFile.getAbsolutePath();
        showDownloadedFileDialog(
                savedFile,
                "Report Downloaded",
                message,
                "Open PDF"
        );
    }
    private void showDownloadedFileDialog(File file, String title, String message, String openOption) {
        int selected = DialogHelper.successOption(
                this,
                title,
                message,
                openOption,
                "Close"
        );
        if (selected == 0) {
            openDownloadedFile(file, openOption);
        }
    }
    private void openDownloadedFile(File file, String openOption) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            DialogHelper.warning(
                    this,
                    openOption + " unavailable",
                    "The file was downloaded, but this system does not allow the app to open files automatically.\n"
                            + file.getAbsolutePath()
            );
            return;
        }
        try {
            Desktop.getDesktop().open(file);
        } catch (IOException exception) {
            DialogHelper.error(
                    this,
                    "File not opened",
                    "The file was downloaded, but it could not be opened automatically.\n"
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
        scroll.setPreferredSize(new Dimension(0, GRAPH_SCROLL_HEIGHT));
        scroll.setMinimumSize(new Dimension(320, GRAPH_SCROLL_HEIGHT));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, GRAPH_SCROLL_HEIGHT));
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
            return new DashboardDao.DashboardStats(0, 0, 0, 0, 0, 0, "-");
        }
    }
    private DashboardDao.OccupancyChartData loadOccupancyChart(String category) {
        try {
            return loadHouseCapacityOccupancyChart(category);
        } catch (SQLException exception) {
            return new DashboardDao.OccupancyChartData(new String[0], new int[0], new int[0]);
        }
    }
    private String[] loadAccommodationCategories() {
        try {
            return visibleHouseCapacityCategories(dashboardDao.loadAccommodationCategories());
        } catch (SQLException exception) {
            return new String[0];
        }
    }
    private String defaultOccupancyCategory(String[] categories) {
        return categories.length == 0 ? "" : categories[0];
    }
    private DashboardDao.OccupancyChartData loadHouseCapacityOccupancyChart(String category) throws SQLException {
        if (isHiddenHouseCapacityCategory(category)) {
            return new DashboardDao.OccupancyChartData(new String[0], new int[0], new int[0]);
        }
        return visibleHouseCapacityData(dashboardDao.loadOccupancyChart(category));
    }
    private String[] visibleHouseCapacityCategories(String[] categories) {
        List<String> visibleCategories = new ArrayList<>();
        for (String category : categories) {
            if (!isHiddenHouseCapacityCategory(category)) {
                visibleCategories.add(category);
            }
        }
        return visibleCategories.toArray(new String[0]);
    }
    private DashboardDao.OccupancyChartData visibleHouseCapacityData(DashboardDao.OccupancyChartData data) {
        List<String> labels = new ArrayList<>();
        List<Integer> capacity = new ArrayList<>();
        List<Integer> occupied = new ArrayList<>();
        String[] dataLabels = data.labels();
        int[] dataCapacity = data.capacity();
        int[] dataOccupied = data.occupied();
        for (int index = 0; index < dataLabels.length; index++) {
            if (isHiddenHouseCapacityLabel(dataLabels[index])) {
                continue;
            }
            labels.add(dataLabels[index]);
            capacity.add(index < dataCapacity.length ? dataCapacity[index] : 0);
            occupied.add(index < dataOccupied.length ? dataOccupied[index] : 0);
        }
        return new DashboardDao.OccupancyChartData(
                labels.toArray(new String[0]),
                intArray(capacity),
                intArray(occupied)
        );
    }
    private boolean isHiddenHouseCapacityLabel(String label) {
        String text = label == null ? "" : label.trim();
        int lineBreak = text.indexOf('\n');
        String category = lineBreak < 0 ? text : text.substring(0, lineBreak).trim();
        return isHiddenHouseCapacityCategory(category);
    }
    private boolean isHiddenHouseCapacityCategory(String category) {
        return category != null && HIDDEN_HOUSE_CAPACITY_CATEGORY.equalsIgnoreCase(category.trim());
    }
    private int[] intArray(List<Integer> values) {
        int[] result = new int[values.size()];
        for (int index = 0; index < values.size(); index++) {
            result[index] = values.get(index);
        }
        return result;
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
