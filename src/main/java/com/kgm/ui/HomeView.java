package com.kgm.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;

public class HomeView extends JFrame {

    private JTable table;
    private DefaultTableModel model;
    private int currentPage = 0;
    private final int PAGE_SIZE = 5;

    private Object[][] allData;

    public HomeView() {

        setTitle("Guest Management Dashboard");
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(createHeader(), BorderLayout.NORTH);
        add(createBody(), BorderLayout.CENTER);

        setVisible(true);
    }

    // ================= HEADER =================
    private JPanel createHeader() {

        JPanel header = new JPanel();
        header.setBackground(new Color(0, 38, 77));
        header.setPreferredSize(new Dimension(100, 60));

        JLabel title = new JLabel("Guest Management Dashboard");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));

        header.add(title);
        return header;
    }

    // ================= BODY (TABS) =================
    private JTabbedPane createBody() {

        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("Home", createHomeTab());
        tabs.addTab("Edit", createEditTab());
        tabs.addTab("Add Record", createAddRecordTab());

        return tabs;
    }

    // ================= HOME TAB =================
    private JPanel createHomeTab() {

        JPanel body = new JPanel(new BorderLayout(10, 10));
        body.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        body.setBackground(Color.WHITE);

        // ===== TOP SECTION =====
        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setBackground(Color.WHITE);

        topSection.add(createKPIs(), BorderLayout.NORTH);
        topSection.add(createControlBar(), BorderLayout.SOUTH);

        body.add(topSection, BorderLayout.NORTH);

        // ===== CENTER CONTENT =====
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBackground(Color.WHITE);

        content.add(createTablePanel(), BorderLayout.NORTH);

        // ===== CHARTS =====
        JPanel chartsRow = new JPanel(new GridLayout(1, 2, 10, 10));
        chartsRow.setBackground(Color.WHITE);

        chartsRow.setPreferredSize(new Dimension(0, 500));
        chartsRow.setMinimumSize(new Dimension(0, 400));

        chartsRow.add(new HouseOccupancyChart());
        chartsRow.add(new DepartmentChart());

        content.add(chartsRow, BorderLayout.CENTER);

        body.add(content, BorderLayout.CENTER);

        // ===== SCROLL WRAP =====
        JScrollPane scroll = new JScrollPane(body);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(scroll, BorderLayout.CENTER);

        return wrapper;
    }

    // ================= EDIT TAB (DUMMY) =================
    private JPanel createEditTab() {

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JLabel label = new JLabel("Edit Records (Dummy Panel)");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JTextArea area = new JTextArea("""
                Edit functionality will be added later.

                Features:
                - Dynamic Addition of House / Rooms
                - Update Status (Vacant / Filled / Under Maintenance)
        """);

        area.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        area.setEditable(false);

        panel.add(label, BorderLayout.NORTH);
        panel.add(area, BorderLayout.CENTER);

        return panel;
    }

    // ================= ADD RECORD TAB =================
    private JPanel createAddRecordTab() {

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField name = new JTextField(15);
        JTextField dept = new JTextField(15);
        JTextField approvedBy = new JTextField(15);
        JTextField arrival = new JTextField(15);
        JTextField departure = new JTextField(15);
      JTextField remarks = new JTextField(15);
        JButton submit = new JButton("Add Record");

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Name"), gbc);
        gbc.gridx = 1;
        panel.add(name, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Department"), gbc);
        gbc.gridx = 1;
        panel.add(dept, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Approved By"), gbc);
        gbc.gridx = 1;
        panel.add(approvedBy, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Arrival Date"), gbc);
        gbc.gridx = 1;
        panel.add(arrival, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("Departure"), gbc);
        gbc.gridx = 1;
        panel.add(departure, gbc);

          gbc.gridx = 0; gbc.gridy = 5;
        panel.add(new JLabel("Remarks"), gbc);
        gbc.gridx = 1;
        panel.add(departure, gbc);

        gbc.gridx = 1; gbc.gridy = 6;
        panel.add(submit, gbc);

        return panel;
    }

    // ================= CONTROL BAR =================
    private JPanel createControlBar() {

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filters.setBackground(Color.WHITE);

        JTextField searchBar = new JTextField(15);
        JComboBox<String> statusFilter = new JComboBox<>(new String[]{"All Status", "Vacant", "Filled"});
        JComboBox<String> departmentFilter = new JComboBox<>(new String[]{"All Departments", "IT", "HR", "Ops", "Sales", "Finance"});
        JTextField dateFilter = new JTextField(10);

        filters.add(new JLabel("Search:"));
        filters.add(searchBar);
        filters.add(new JLabel("Status:"));
        filters.add(statusFilter);
        filters.add(new JLabel("Department:"));
        filters.add(departmentFilter);
        filters.add(new JLabel("Date:"));
        filters.add(dateFilter);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setBackground(Color.WHITE);

        JButton editHouse = new JButton("Edit");
        JButton addRecord = new JButton("Add Record");

        actions.add(editHouse);
        actions.add(addRecord);

        bar.add(filters, BorderLayout.WEST);
        bar.add(actions, BorderLayout.EAST);

        return bar;
    }

    // ================= KPI =================
    private JPanel createKPIs() {

        JPanel kpi = new JPanel(new GridLayout(2, 3, 10, 10));
        kpi.setPreferredSize(new Dimension(0, 180));
        kpi.setBackground(Color.WHITE);

        kpi.add(kpiCard("Monthly Occupancy", "82%"));
        kpi.add(kpiCard("Avg Stay Duration", "2.4 hrs"));
        kpi.add(kpiCard("Peak Arrival", "11:00 AM"));
        kpi.add(kpiCard("Total Seats", "120"));
        kpi.add(kpiCard("Vacant Seats", "38"));
        kpi.add(kpiCard("Occupied Seats", "82"));

        return kpi;
    }

    private JPanel kpiCard(String title, String value) {

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(245, 248, 255));
        p.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));

        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel v = new JLabel(value);
        v.setFont(new Font("Segoe UI", Font.BOLD, 20));
        v.setHorizontalAlignment(SwingConstants.CENTER);

        p.add(t, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);

        return p;
    }

    // ================= TABLE =================
    private JPanel createTablePanel() {

        JPanel wrapper = new JPanel(new BorderLayout());

        String[] cols = {"Name", "Department", "Approved By", "Arrival", "Departure", "Actions"};

        allData = new Object[][]{
                {"Ali Khan", "IT", "Mate", "2026-05-01", "2026-05-01"},
                {"Sara Ahmed", "HR", "Mate", "2026-05-02", "2026-05-02"},
                {"Usman Tariq", "Ops", "Adnan Latif", "2026-05-02", "2026-05-03"},
                {"Hassan Raza", "Finance", "Adnan Latif", "2026-05-03", "2026-05-04"},
                {"Bilal Khan", "Sales", "Adnan Latif", "2026-05-04", "2026-05-04"},
                {"Ayesha Noor", "IT", "Adnan Latif", "2026-05-04", "2026-05-05"},
                {"Zain Ali", "HR", "Adnan Latif", "2026-05-05", "2026-05-05"},
                {"Noman", "Ops", "Adnan Latif", "2026-05-05", "2026-05-06"}
        };

        model = new DefaultTableModel(cols, 0);
        table = new JTable(model);

        table.setRowHeight(30);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        loadPage();

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(0, 220));

        wrapper.add(scrollPane, BorderLayout.CENTER);
        wrapper.add(createPaginationPanel(), BorderLayout.SOUTH);

        return wrapper;
    }

    private void loadPage() {

        model.setRowCount(0);

        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, allData.length);

        for (int i = start; i < end; i++) {

            Object[] row = allData[i];

            model.addRow(new Object[]{
                    row[0], row[1], row[2], row[3], row[4], "View"
            });
        }
    }

    private JPanel createPaginationPanel() {

        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton prev = new JButton("Prev");
        JButton next = new JButton("Next");

        prev.addActionListener((ActionEvent e) -> {
            if (currentPage > 0) {
                currentPage--;
                loadPage();
            }
        });

        next.addActionListener((ActionEvent e) -> {
            if ((currentPage + 1) * PAGE_SIZE < allData.length) {
                currentPage++;
                loadPage();
            }
        });

        p.add(prev);
        p.add(next);

        return p;
    }

    // ================= CHART 1 =================
   class HouseOccupancyChart extends JPanel {

    String[] houses = {"A", "B", "C", "D", "E", "F", "G", "H"};
    int[] capacity = {10, 14, 8, 16, 12, 9, 11, 13};
    int[] occupied = {7, 10, 5, 13, 8, 6, 9, 10};

    public HouseOccupancyChart() {
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createTitledBorder("House Occupancy"));
        setPreferredSize(new Dimension(0, 500));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int max = 16;
        int h = getHeight();
        int w = getWidth();

        int baseY = h - 40;
        int chartHeight = h - 80;
        int barW = w / houses.length;

        // axis line
        g2.setColor(new Color(200, 200, 200));
        g2.drawLine(20, baseY, w - 20, baseY);

        for (int i = 0; i < houses.length; i++) {

            int capH = (int) ((capacity[i] / (double) max) * chartHeight);
            int occH = (int) ((occupied[i] / (double) max) * chartHeight);

            int x = i * barW + 15;

            // capacity (background bar)
            GradientPaint gp1 = new GradientPaint(
                    x, baseY - capH, new Color(220, 220, 220),
                    x, baseY, new Color(180, 180, 180)
            );
            g2.setPaint(gp1);
            g2.fillRoundRect(x, baseY - capH, barW - 30, capH, 10, 10);

            // occupied (foreground bar)
            GradientPaint gp2 = new GradientPaint(
                    x, baseY - occH, new Color(0, 200, 150),
                    x, baseY, new Color(0, 120, 100)
            );
            g2.setPaint(gp2);
            g2.fillRoundRect(x, baseY - occH, barW - 30, occH, 10, 10);

            // labels
            g2.setColor(Color.DARK_GRAY);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.drawString(houses[i], x + (barW / 2) - 10, baseY + 20);
        }
    }
}

    // ================= CHART 2 =================
   class DepartmentChart extends JPanel {

    String[] dept = {"IT", "HR", "Ops", "Sales", "Finance"};
    int[] values = {12, 7, 10, 9, 6};

    public DepartmentChart() {
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createTitledBorder("Department Analysis"));
        setPreferredSize(new Dimension(0, 500));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int max = 12;
        int h = getHeight();
        int w = getWidth();

        int baseY = h - 40;
        int chartHeight = h - 80;
        int barW = w / values.length;

        g2.setColor(new Color(220, 220, 220));
        g2.drawLine(20, baseY, w - 20, baseY);

        for (int i = 0; i < values.length; i++) {

            int barH = (int) ((values[i] / (double) max) * chartHeight);
            int x = i * barW + 15;

            GradientPaint gp = new GradientPaint(
                    x, baseY - barH, new Color(80, 140, 255),
                    x, baseY, new Color(30, 80, 200)
            );

            g2.setPaint(gp);
            g2.fillRoundRect(x, baseY - barH, barW - 30, barH, 10, 10);

            g2.setColor(Color.DARK_GRAY);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.drawString(dept[i], x + (barW / 2) - 15, baseY + 20);
        }
    }
}
}