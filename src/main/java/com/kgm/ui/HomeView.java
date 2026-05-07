package com.kgm.ui;

import com.kgm.dao.DashboardDao;
import com.kgm.database.DatabaseInitializer;
import com.kgm.ui.panel.AccommodationManagementPanel;
import com.kgm.ui.panel.DepartmentAnalysisGraphPanel;
import com.kgm.ui.panel.GuestFilterPanel;
import com.kgm.ui.panel.GuestDetailsPanel;
import com.kgm.ui.panel.GuestRecordPanel;
import com.kgm.ui.panel.FooterPanel;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.panel.HomeKpiPanel;
import com.kgm.ui.panel.HouseOccupancyGraphPanel;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;

public class HomeView extends JFrame {
    private static final int DASHBOARD_TAB = 0;
    private static final int ADD_GUEST_TAB = 1;
    // KPI bottom margin; adjust this value to tune space below KPI cards.
    private static final int KPI_BOTTOM_MARGIN = 36;
    private static final String DASHBOARD_SCREEN = "dashboard";
    private static final String GUEST_DETAILS_SCREEN = "guestDetails";

    private GuestFilterPanel guestFilterPanel;
    private GuestRecordPanel guestRecordPanel;
    private final DashboardDao dashboardDao = new DashboardDao();
    private JPanel dashboardScreens;
    private JPanel dashboardPage;
    private Component guestDetailsScreen;
    private JTabbedPane mainTabs;
    private boolean refreshingAddGuestTab;

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
            return;
        }
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
        guestRecordPanel = new GuestRecordPanel(this::showGuestDetails);
        guestFilterPanel = new GuestFilterPanel(
                this::performSearch,
                this::clearSearch
        );

        GridBagConstraints gbc = HomeViewHelper.pageConstraints(0);
        gbc.insets = new Insets(0, 0, KPI_BOTTOM_MARGIN, 0);
        dashboardPage.add(new HomeKpiPanel(loadDashboardStats()), gbc);

        gbc = HomeViewHelper.pageConstraints(1);
        dashboardPage.add(guestFilterPanel, gbc);

        gbc = HomeViewHelper.pageConstraints(2);
        dashboardPage.add(guestRecordPanel, gbc);

        gbc = HomeViewHelper.pageConstraints(3);
        dashboardPage.add(createGraphPanel(), gbc);

        gbc = HomeViewHelper.pageConstraints(4);
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
    }

    private void refreshGuestRecords() {
        if (guestRecordPanel != null) {
            guestRecordPanel.refreshFromDatabase();
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

        graphs.add(new HouseOccupancyGraphPanel(loadOccupancyChart()));
        graphs.add(new DepartmentAnalysisGraphPanel(loadDepartmentChart()));

        return graphs;
    }

    private DashboardDao.DashboardStats loadDashboardStats() {
        try {
            return dashboardDao.loadStats();
        } catch (SQLException exception) {
            return new DashboardDao.DashboardStats(0, 0, 0, 0, 0, "-");
        }
    }

    private DashboardDao.OccupancyChartData loadOccupancyChart() {
        try {
            return dashboardDao.loadOccupancyChart();
        } catch (SQLException exception) {
            return new DashboardDao.OccupancyChartData(new String[0], new int[0], new int[0]);
        }
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
