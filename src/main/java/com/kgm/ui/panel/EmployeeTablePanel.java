package com.kgm.ui.panel;

import javax.swing.*;
import com.kgm.ui.EmployeeDetailView;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import com.kgm.dao.EmployeeRepositoryDao;
import com.kgm.model.Employee;

public class EmployeeTablePanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private JPanel paginationPanel;
    private final List<Employee> allData = new ArrayList<>();
    // pagination editable
    private final int rowsPerPage = 25;
    private int currentPage = 1;
    private final EmployeeRepositoryDao repo;
    private JLabel showingLabel;

    public EmployeeTablePanel(EmployeeRepositoryDao repo) {
        this.repo = repo;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        model.setColumnIdentifiers(new String[] {
                "Employee ID",
                "Name",
                "Father Name",
                "CNIC",
                "Phone",
                "Email",
                "Department",
                "Designation",
                "Gender",
                "Reason",
                "Joining Date",
                "Leaving Date",
        });
        table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFillsViewportHeight(true);
        styleTable();
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);

        // event

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = table.getSelectedRow();
                if (row != -1) {
                    // get Employee Code from first column
                    String empCode = table.getValueAt(row, 0).toString();
                    // close current window
                    java.awt.Window window = SwingUtilities.getWindowAncestor(table);
                    if (window != null) {
                        window.dispose();
                    }
                    // open detail page
                    new EmployeeDetailView(empCode);
                }
            }
        });
        // event
        paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 10));
        paginationPanel.setBackground(Color.WHITE);
        paginationPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        showingLabel = new JLabel();
        showingLabel.setForeground(Color.BLACK);
        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBackground(Color.WHITE);
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        labelPanel.setBackground(Color.WHITE);
        labelPanel.add(showingLabel);
        bottom.add(labelPanel);
        bottom.add(paginationPanel);
        add(bottom, BorderLayout.SOUTH);
        loadPage(1);
        buildPagination();
    }

    // ================= SEARCH DISPLAY (NEW) =================
    public void showSingleEmployee(Employee e) {
        model.setRowCount(0);
        if (e == null)
            return;
        model.addRow(new Object[] {
                e.getEMPLOYEE_CODE(),
                e.getEMP_NAME(),
                e.getFATHER_NAME(),
                e.getNID(),
                e.getEMP_CONTNO(),
                e.getPERSONAL_EMAIL(),
                e.getDEPARTMENT(),
                e.getDESIGNATION(),
                e.getGENDER(),
                e.getRESIGN_REASON(),
                e.getJOINING_DATE(),
                e.getRESIGN_DATE()
        });
        showingLabel.setText("Showing 1 / 1");
        paginationPanel.removeAll();
        paginationPanel.revalidate();
        paginationPanel.repaint();
    }

    private void updateShowingLabel(int totalRecords) {
        int start = ((currentPage - 1) * rowsPerPage) + 1;
        int end = Math.min(currentPage * rowsPerPage, totalRecords);
        if (totalRecords == 0) {
            showingLabel.setText("Showing 0 /  0");
        } else {
            showingLabel.setText("Showing " + start + " - " + end + " /  " + totalRecords);
        }
    }

    private void styleTable() {
        table.setRowHeight(42);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setBackground(Color.WHITE);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setPreferredSize(new Dimension(100, 45));
        header.setBackground(new Color(245, 245, 245));
        header.setForeground(new Color(60, 60, 60));
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                label.setOpaque(true);
                label.setBackground(isSelected
                        ? new Color(232, 244, 255)
                        : Color.WHITE);
                label.setForeground(new Color(50, 50, 50));
                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(235, 235, 235)),
                        BorderFactory.createEmptyBorder(6, 10, 6, 10)));
                return label;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    private void loadPage(int page) {
        model.setRowCount(0);
        int offset = (page - 1) * rowsPerPage;
        List<Employee> list = repo.getEmployees(offset);
        allData.clear();
        allData.addAll(list);
        for (Employee e : list) {
            model.addRow(new Object[] {
                    e.getEMPLOYEE_CODE(),
                    e.getEMP_NAME(),
                    e.getFATHER_NAME(),
                    e.getNID(),
                    e.getEMP_CONTNO(),
                    e.getPERSONAL_EMAIL(),
                    e.getDEPARTMENT(),
                    e.getDESIGNATION(),
                    e.getGENDER(),
                    e.getRESIGN_REASON(),
                    e.getJOINING_DATE(),
                    e.getRESIGN_DATE()
            });
        }
        updateShowingLabel(repo.countEmployees());
        SwingUtilities.invokeLater(this::autoResizeColumns);
    }

    private void autoResizeColumns() {
        for (int col = 0; col < table.getColumnCount(); col++) {
            int width = 60;
            for (int row = 0; row < table.getRowCount(); row++) {
                Component comp = table.prepareRenderer(
                        table.getCellRenderer(row, col),
                        row, col);
                width = Math.max(comp.getPreferredSize().width + 20, width);
            }
            JTableHeader header = table.getTableHeader();
            Component headerComp = header.getDefaultRenderer()
                    .getTableCellRendererComponent(
                            table,
                            table.getColumnName(col),
                            false, false, 0, col);
            width = Math.max(width, headerComp.getPreferredSize().width + 20);
            table.getColumnModel().getColumn(col).setPreferredWidth(width);
        }
    }

    private void buildPagination() {
        paginationPanel.removeAll();
        int totalRecords = repo.countEmployees();
        int totalPages = Math.max(1,
                (int) Math.ceil(totalRecords / (double) rowsPerPage));
        for (int i = 1; i <= totalPages; i++) {
            JButton btn = new JButton(String.valueOf(i));
            btn.setPreferredSize(new Dimension(38, 32));
            btn.setFocusPainted(false);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            if (i == currentPage) {
                btn.setBackground(Color.BLACK);
                btn.setForeground(Color.WHITE);
            } else {
                btn.setBackground(new Color(240, 240, 240));
                btn.setForeground(new Color(120, 120, 120));
            }
            int page = i;
            btn.addActionListener(e -> {
                currentPage = page;
                loadPage(page);
                buildPagination();
            });
            paginationPanel.add(btn);
        }
        paginationPanel.revalidate();
        paginationPanel.repaint();
    }

    public void clearTable() {
        model.setRowCount(0);
    }

    public void reload() {
        currentPage = 1;
        loadPage(currentPage);
        buildPagination();
    }
}