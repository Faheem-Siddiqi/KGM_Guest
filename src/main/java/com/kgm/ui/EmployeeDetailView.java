package com.kgm.ui;

import com.kgm.dao.EmployeeRepositoryDao;
import javax.swing.*;
import java.awt.*;
import javax.swing.border.EmptyBorder;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.panel.OtherDetailsPanel;
import com.kgm.ui.panel.BasicDetailsPanel;
import com.kgm.model.Employee;
import com.kgm.ui.panel.DocumentViewPanel;
import com.kgm.ui.panel.FooterPanel;

public class EmployeeDetailView extends JFrame {

    private String empCode;
    private JButton backBtn;
    private JButton updateBtn;

    public EmployeeDetailView(String empCode) {

        this.empCode = (empCode != null) ? empCode.trim() : null;
        Employee emp = null;

        try {
            if (this.empCode != null && !this.empCode.isEmpty()) {
                emp = new EmployeeRepositoryDao().getFullEmployeeByCode(this.empCode);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "An unexpected error occurred.\nPlease contact the administrator.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        initializeUI(emp, true);
    }

    public EmployeeDetailView() {
        initializeUI(null, false);
    }

    private void initializeUI(Employee emp, boolean isWithData) {

        setTitle("Employee Form");
        setSize(1100, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 🔸 Header
        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.setBackground(Color.WHITE);

        topContainer.add(new HeaderPanel("Employee Record"), BorderLayout.NORTH);

        // 🔹 SECOND ROW
        JPanel secondRow = new JPanel(new BorderLayout());
        secondRow.setBackground(Color.WHITE);
        secondRow.setBorder(new EmptyBorder(10, 20, 0, 16));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setBackground(Color.WHITE);

        backBtn = new JButton("BACK");
        backBtn.setBorderPainted(false);
        backBtn.setContentAreaFilled(false);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backBtn.setForeground(new Color(0, 102, 204));
        backBtn.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));

        backBtn.addActionListener(e -> {
            this.dispose();
            new HomeView();
        });

        left.add(backBtn);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBackground(Color.WHITE);
        right.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 18));

        String nameValue = (emp != null) ? emp.getEMP_NAME() : "";
        String codeValue = (emp != null) ? emp.getEMPLOYEE_CODE() : "";

        JLabel name = new JLabel(nameValue);
    name.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
       name.setForeground(new Color(100, 100, 100));
        name.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel code = new JLabel("Code: " + codeValue);
        code.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        code.setForeground(new Color(90, 90, 90));
        code.setAlignmentX(Component.RIGHT_ALIGNMENT);

        right.add(name);
        right.add(Box.createVerticalStrut(2));
        right.add(code);

        secondRow.add(left, BorderLayout.WEST);
        secondRow.add(right, BorderLayout.EAST);

        topContainer.add(secondRow, BorderLayout.CENTER);

        add(topContainer, BorderLayout.NORTH);

        // 🔸 CENTER
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBorder(new EmptyBorder(10, 20, 10, 20));
        centerWrapper.setBackground(Color.WHITE);

        JTabbedPane tabs = new JTabbedPane();

        if (isWithData) {
            tabs.addTab("Basic", new BasicDetailsPanel(emp));
            tabs.addTab("Others", new OtherDetailsPanel(emp));
        } else {
            tabs.addTab("Core", new BasicDetailsPanel());
            tabs.addTab("Details", new OtherDetailsPanel());
        }

        tabs.addTab("Documents", new DocumentViewPanel());

        centerWrapper.add(tabs, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        // 🔸 FOOTER
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setBackground(Color.WHITE);

        updateBtn = new JButton("Update");
        updateBtn.setPreferredSize(new Dimension(110, 32));
        updateBtn.setFocusPainted(false);
        updateBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        updateBtn.setForeground(Color.WHITE);
        updateBtn.setBackground(new Color(0, 38, 77));

        actions.add(updateBtn);
        add(new FooterPanel(actions), BorderLayout.SOUTH);

        // =========================================================
        // 🔥 CORE FIX: SMART ENABLE/DISABLE (NO TAB LOGIC ANYMORE)
        // =========================================================

        Runnable refreshButtonState = () -> {

            boolean canUpdate = false;

            for (int i = 0; i < tabs.getTabCount(); i++) {
                Component comp = tabs.getComponentAt(i);

                if (comp instanceof BasicDetailsPanel bp && panelHasEditableFields(bp)) {
                    canUpdate = true;
                    break;
                }

                if (comp instanceof OtherDetailsPanel op && panelHasEditableFields(op)) {
                    canUpdate = true;
                    break;
                }
            }

            updateBtn.setEnabled(canUpdate);
        };

        // helper check (keeps parent clean, no panel redesign needed)
        tabs.addChangeListener(e -> refreshButtonState.run());

        // initial state
        refreshButtonState.run();

        // 🔸 Update action
        updateBtn.addActionListener(e -> {

            try {
                BasicDetailsPanel basicPanel = null;
                OtherDetailsPanel otherPanel = null;

                // find panels
                for (int i = 0; i < tabs.getTabCount(); i++) {
                    Component comp = tabs.getComponentAt(i);

                    if (comp instanceof BasicDetailsPanel bp) {
                        basicPanel = bp;
                    }

                    if (comp instanceof OtherDetailsPanel op) {
                        otherPanel = op;
                    }
                }

                EmployeeRepositoryDao dao = new EmployeeRepositoryDao();
                boolean updatedAny = false;

                if (basicPanel != null && panelHasEditableFields(basicPanel)) {
                    Employee updatedBasic = basicPanel.getEmployeeFromForm();
                    updatedBasic.setEMPLOYEE_CODE(empCode);
                    dao.updateEmployeeDynamic(updatedBasic);
                    updatedAny = true;
                }

                if (otherPanel != null && panelHasEditableFields(otherPanel)) {
                    Employee updatedOther = otherPanel.getUpdatedOtherDetails();
                    updatedOther.setEMPLOYEE_CODE(empCode);
                    dao.updateEmployeeDynamic(updatedOther);
                    updatedAny = true;
                }

                if (!updatedAny) {
                    JOptionPane.showMessageDialog(this, "No editable fields found");
                    return;
                }

                JOptionPane.showMessageDialog(this, "Updated successfully");

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Update failed");
            }
        });
        setVisible(true);
    }

    // ================= SIMPLE CHECK =================
    // avoids interface changes in panels
private boolean panelHasEditableFields(Container container) {

    for (Component comp : container.getComponents()) {

        // JTextField
        if (comp instanceof JTextField tf && tf.isEditable()) {
            return true;
        }

        // JTextArea
        if (comp instanceof JTextArea ta && ta.isEditable()) {
            return true;
        }

        // JComboBox
        if (comp instanceof JComboBox<?> cb && cb.isEnabled()) {
            return true;
        }

        // JSpinner
        if (comp instanceof JSpinner sp && sp.isEnabled()) {
            return true;
        }

        // 🔁 recursive check (VERY IMPORTANT)
        if (comp instanceof Container child) {
            if (panelHasEditableFields(child)) {
                return true;
            }
        }
    }

    return false;
}
}
