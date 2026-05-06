package com.kgm.ui;
import javax.swing.*;
import java.awt.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import com.kgm.ui.panel.FooterPanel;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.panel.FormPanel;
import com.kgm.ui.panel.DocumentPanel;
import com.kgm.config.DatabaseConnection;
import com.kgm.dao.EmployeeDao;
import com.kgm.model.Employee;
import java.io.File;
public class EmployeeInduction extends JFrame {
    private JButton nextBackBtn;
    private JButton submitBtn;
    private JButton backBtn;
    public EmployeeInduction() {
        setTitle("Employee Form");
        setSize(1100, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BorderLayout());
        // Header
        topContainer.add(new HeaderPanel("Employee Induction"), BorderLayout.NORTH);
        // Back button row
        JPanel backRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        backRow.setBackground(Color.WHITE);
        backBtn = new JButton("Back");

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
        backRow.add(backBtn);
        // place below header
        topContainer.add(backRow, BorderLayout.CENTER);
        // add once
        add(topContainer, BorderLayout.NORTH);
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBorder(new EmptyBorder(10, 20, 10, 20));
        centerWrapper.setOpaque(true);
        centerWrapper.setBackground(Color.WHITE);
        JTabbedPane tabs = new JTabbedPane();
        FormPanel formPanel = new FormPanel();
        DocumentPanel documentPanel = new DocumentPanel();
        tabs.addTab("Form", formPanel);
        tabs.addTab("Documents", documentPanel);
        centerWrapper.add(tabs, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setBackground(Color.WHITE);
        nextBackBtn = new JButton("Next");
        submitBtn = new JButton("Submit");
        nextBackBtn.setPreferredSize(new Dimension(100, 32));
        nextBackBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        nextBackBtn.setFocusPainted(false);
        nextBackBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        nextBackBtn.setForeground(Color.WHITE);
        nextBackBtn.setBackground(new Color(0, 38, 77)); // navy blue
        submitBtn.setEnabled(false);
        submitBtn.setPreferredSize(new Dimension(100, 32));
        submitBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        submitBtn.setFocusPainted(false);
        submitBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setBackground(new Color(0, 38, 77)); // navy blue
        submitBtn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        actions.add(nextBackBtn);
        actions.add(submitBtn);
        add(new FooterPanel(actions), BorderLayout.SOUTH);
        // ================= TAB CHANGE =================
        tabs.addChangeListener((ChangeEvent e) -> {
            int index = tabs.getSelectedIndex();
            if (index == 0) {
                nextBackBtn.setText("Next");
                submitBtn.setEnabled(false);
            } else {
                nextBackBtn.setText("Back");
                submitBtn.setEnabled(true);
            }
        });
        nextBackBtn.addActionListener(e -> {
            int index = tabs.getSelectedIndex();
            if (index == 0) {
                tabs.setSelectedIndex(1);
            } else {
                tabs.setSelectedIndex(0);
            }
        });
        // ================= SUBMIT =================
        submitBtn.addActionListener(e -> {
            try {
                Employee emp = formPanel.getEmployeeFromForm();
                String empCode = emp.getEMPLOYEE_CODE();
                // ================= ROOT FOLDER =================
                String basePath = System.getProperty("user.dir") + "/employees/";
                File empDir = new File(basePath + empCode);
                File docDir = new File(empDir, "documents");
                if (!docDir.exists())
                    docDir.mkdirs();
                // ================= PROFILE IMAGE =================
                File img = formPanel.getSelectedImage();
                if (img != null) {
                    File dest = new File(empDir, "EMP_IMG.jpg");
                    try (java.io.InputStream in = new java.io.FileInputStream(img);
                            java.io.OutputStream out = new java.io.FileOutputStream(dest)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = in.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }
                        emp.setEMP_IMG("employees/" + empCode + "/EMP_IMG.jpg");
                    }
                }
                // ================= DOCUMENTS SAVE =================
                DocumentPanel docPanel = documentPanel;
                String[] docs = docPanel.getAllDocumentPaths();
                if (docs != null) {
                    String[] fileNames = {
                            "CNIC_COPY.jpg",
                            "EOBI_CARD_COPY.jpg",
                            "SS_CARD_COPY.jpg",
                            "FINAL_SETTLEMENT.jpg",
                            "CLEARANCE_CERT.jpg",
                            "JOB_APPOINTMENT.jpg",
                            "APPLICATION_DOC.jpg",
                            "ISSUANCE_DOC.jpg",
                            "SETTLEMENT_DOC.jpg",
                            "TRIAL_CARD.jpg",
                            "INTERVIEW_DOC.jpg",
                            "SERVICE_LETTER.jpg",
                            "EXTENSION_LETTER.jpg",
                            "RETIREMENT_LETTER.jpg",
                            "COVID_CERT.jpg"
                    };
                    // ================= COPY FILES =================
                    for (int i = 0; i < docs.length; i++) {
                        if (docs[i] != null) {
                            File src = new File(docs[i]);
                            File dest = new File(docDir, fileNames[i]);
                            try (java.io.InputStream in = new java.io.FileInputStream(src);
                                    java.io.OutputStream out = new java.io.FileOutputStream(dest)) {
                                byte[] buffer = new byte[1024];
                                int len;
                                while ((len = in.read(buffer)) > 0) {
                                    out.write(buffer, 0, len);
                                }
                            }
                            // ================= DB PATH =================
                            String dbPath = "employees/" + empCode + "/documents/" + fileNames[i];
                            switch (i) {
                                case 0 -> emp.setCNIC_COPY(dbPath);
                                case 1 -> emp.setEOBI_CARD_COPY(dbPath);
                                case 2 -> emp.setSS_CARD_COPY(dbPath);
                                case 3 -> emp.setFINAL_SETTLEMENT(dbPath);
                                case 4 -> emp.setCLEARANCE_CERT(dbPath);
                                case 5 -> emp.setJOB_APPOINTMENT(dbPath);
                                case 6 -> emp.setAPPLICATION_DOC(dbPath);
                                case 7 -> emp.setISSUANCE_DOC(dbPath);
                                case 8 -> emp.setSETTLEMENT_DOC(dbPath);
                                case 9 -> emp.setTRIAL_CARD(dbPath);
                                case 10 -> emp.setINTERVIEW_DOC(dbPath);
                                case 11 -> emp.setSERVICE_LETTER(dbPath);
                                case 12 -> emp.setEXTENSION_LETTER(dbPath);
                                case 13 -> emp.setRETIREMENT_LETTER(dbPath);
                                case 14 -> emp.setCOVID_CERT(dbPath);
                            }
                        }
                    }
                }
                // ================= DB INSERT =================
                EmployeeDao dao = new EmployeeDao(DatabaseConnection.getConnection());
                dao.insertEmployee(emp);
                JOptionPane.showMessageDialog(this, "Employee Saved Successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Failed to Save Employee:\n" + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        setVisible(true);
    }
}
