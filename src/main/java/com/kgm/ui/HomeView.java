package com.kgm.ui;

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

public class HomeView extends JFrame {
    private static final int DASHBOARD_TAB = 0;
    // KPI bottom margin; adjust this value to tune space below KPI cards.
    private static final int KPI_BOTTOM_MARGIN = 36;
    private static final String DASHBOARD_SCREEN = "dashboard";
    private static final String GUEST_DETAILS_SCREEN = "guestDetails";

    private GuestFilterPanel guestFilterPanel;
    private GuestRecordPanel guestRecordPanel;
    private JPanel dashboardScreens;
    private Component guestDetailsScreen;

    public HomeView() {
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
        HomeViewHelper.styleTabs(tabs);

        tabs.addTab("Dashboard", createHomeTab(tabs));
        tabs.addTab("Add Guest", AddGuest.createContent(() -> tabs.setSelectedIndex(DASHBOARD_TAB)));
        tabs.addTab("Accommodations", new AccommodationManagementPanel(() -> tabs.setSelectedIndex(DASHBOARD_TAB)));
        return tabs;
    }

    private JComponent createHomeTab(JTabbedPane tabs) {
        dashboardScreens = new JPanel(new CardLayout());
        dashboardScreens.setBackground(Color.WHITE);
        dashboardScreens.add(createDashboardScreen(tabs), DASHBOARD_SCREEN);
        return dashboardScreens;
    }

    private JComponent createDashboardScreen(JTabbedPane tabs) {
        JPanel page = HomeViewHelper.pagePanel();
        guestRecordPanel = new GuestRecordPanel(this::showGuestDetails);
        guestFilterPanel = new GuestFilterPanel(
                this::performSearch,
                this::clearSearch
        );

        GridBagConstraints gbc = HomeViewHelper.pageConstraints(0);
        gbc.insets = new Insets(0, 0, KPI_BOTTOM_MARGIN, 0);
        page.add(new HomeKpiPanel(), gbc);

        gbc = HomeViewHelper.pageConstraints(1);
        page.add(guestFilterPanel, gbc);

        gbc = HomeViewHelper.pageConstraints(2);
        page.add(guestRecordPanel, gbc);

        gbc = HomeViewHelper.pageConstraints(3);
        page.add(createGraphPanel(), gbc);

        gbc = HomeViewHelper.pageConstraints(4);
        gbc.weighty = 1.0;
        page.add(Box.createVerticalGlue(), gbc);

        JScrollPane scroll = new JScrollPane(page);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    private void showGuestDetails(Object[] guestRecord) {
        if (dashboardScreens == null) {
            return;
        }
        if (guestDetailsScreen != null) {
            dashboardScreens.remove(guestDetailsScreen);
        }
        guestDetailsScreen = new GuestDetailsPanel(guestRecord, this::showDashboard);
        dashboardScreens.add(guestDetailsScreen, GUEST_DETAILS_SCREEN);
        showDashboardCard(GUEST_DETAILS_SCREEN);
    }

    private void showDashboard() {
        showDashboardCard(DASHBOARD_SCREEN);
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

        graphs.add(new HouseOccupancyGraphPanel());
        graphs.add(new DepartmentAnalysisGraphPanel());

        return graphs;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(HomeView::new);
    }
}
