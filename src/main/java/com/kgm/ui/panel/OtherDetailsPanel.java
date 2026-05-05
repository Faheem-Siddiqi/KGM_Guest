package com.kgm.ui.panel;

import com.kgm.model.Employee;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class OtherDetailsPanel extends JPanel {

    private Employee data;

    // 🔥 NEW: store fields
    private Map<String, JTextField> fieldMap = new HashMap<>();

    // ================= EMPTY =================
    public OtherDetailsPanel() {
        this(null);
    }

    // ================= WITH DATA =================
    public OtherDetailsPanel(Employee data) {
        this.data = data;

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(buildUI());
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(scroll, BorderLayout.CENTER);
    }

    // ================= UI =================
    private JPanel buildUI() {

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(20, 20, 20, 20));

        root.add(createSection("Organization / Structure",
                new String[][]{
                        {"ORG ID", get("ORG_ID")},
                        {"DIVISION", get("DIVISION")},
                        {"SHIFT", get("SHIFT")},
                        {"PROB PERIOD", get("PROB_PERIOD")},
                        {"CONFIRMING ON", get("CONFIRMING_ON")},
                        {"REP UNIT", get("REP_UNT")},
                        {"REP EMP ID", get("REP_EMP_ID")},
                        {"REP DESIG CODE", get("REP_EMP_DESIG_CODE")},
                        {"REP DEPT CODE", get("REP_EMP_DEPT_CODE")},
                        {"REP TYPE", get("REP_EMP_TYPE")},
                        {"BRANCH CODE", get("BRANCH_CODE")},
                        {"BRANCH NAME", get("BRANCH_NAME")}
                }));

        root.add(Box.createVerticalStrut(10));

        root.add(createSection("Personal / HR Details",
                new String[][]{
                        {"DOB", get("DOB")},
                        {"City of Birth", get("CITY_OF_BIRTH")},
                        {"Nationality", get("NATIONALITY")},
                        {"Religion", get("RELIGION")},
                        {"Blood Group", get("BLOOD_GROUP")},
                        {"Marital Status", get("M_STATUS")},
                        {"Mother Name", get("MOTHER_NAME")}
                }));

        root.add(Box.createVerticalStrut(10));

        root.add(createSection("Banking / Finance",
                new String[][]{
                        {"Bank Name", get("BANK_NAME")},
                        {"Account No", get("BANK_AC_NO")},
                        {"SS No", get("SS_NO")},
                        {"EOBI No", get("EOBI_NO")},
                        {"Tax No", get("TAX_NO")},
                        {"EFU", get("EFU")},
                        {"EFU No", get("EFU_NO")},
                        {"PFUND Code", get("CLIPPER_PFUND_CODE")}
                }));

        root.add(Box.createVerticalStrut(10));

        root.add(createSection("Compliance / Status",
                new String[][]{
                        {"EOBI Status", get("EOBI_STATUS")},
                        {"NIC Verify", get("NIC_VERIFY")},
                        {"NIC Verify Date", get("NIC_VERIFY_DATE")},
                        {"HOD Check", get("HOD_CHECK")},
                        {"Clearance Status", get("CLEARANCE_STATUS")},
                        {"Dis Certificate", get("DIS_CERTIFICATE")}
                }));

        root.add(Box.createVerticalStrut(10));

        root.add(createSection("Emergency / Misc",
                new String[][]{
                        {"Emergency No", get("EMERGENCY_NO")},
                        {"Attendance Category", get("ATT_CATEG")}
                }));

        root.add(Box.createVerticalStrut(10));

        root.add(createSection("Vaccination / Wellness",
                new String[][]{
                        {"Wellness Club", get("WELLNESS_CLUB")},
                        {"Card Issue", get("WELLNESS_CARD_ISSUE")},
                        {"Card No", get("WELLNESS_CARD_NO")},
                        {"Valid Date", get("WELLNESS_CLUB_VALID_DATE")},
                        {"First Dose", get("FIRST_DOSE")},
                        {"Second Dose", get("SECOND_DOSE")},
                        {"First Vacc Date", get("FIRST_VACC_DATE")},
                        {"Second Vacc Date", get("SECOND_VACC_DATE")}
                }));

        return root;
    }

    // ================= SECTION =================
    private JPanel createSection(String title, String[][] data) {

        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(Color.WHITE);
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230)),
                new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel header = new JLabel(title);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setForeground(new Color(60, 60, 60));

        section.add(header, BorderLayout.NORTH);
        section.add(buildGrid(data), BorderLayout.CENTER);

        return section;
    }

    // ================= GRID =================
    private JPanel buildGrid(String[][] data) {

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        for (int i = 0; i < data.length; i += 2) {

            gbc.gridy = row++;

            gbc.gridx = 0;
            gbc.weightx = 0.5;
            grid.add(createField(data[i][0], data[i][1]), gbc);

            gbc.gridx = 1;
            gbc.weightx = 0.5;

            if (i + 1 < data.length) {
                grid.add(createField(data[i + 1][0], data[i + 1][1]), gbc);
            } else {
                grid.add(new JPanel(), gbc);
            }
        }

        return grid;
    }

    // ================= FIELD =================
    private JPanel createField(String label, String value) {

        JPanel p = new JPanel(new BorderLayout(5, 3));
        p.setBackground(Color.WHITE);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // 🔥 normalize empty -> N/A
        if (isEmpty(value)) {
            value = "N/A";
        }

        JTextField field = new JTextField(value);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        boolean editable = isEmpty(value);

        field.setEditable(editable);
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        field.setPreferredSize(new Dimension(200, 30));

        // 🔥 store field
        fieldMap.put(label, field);

        p.add(lbl, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);

        return p;
    }

    // ================= DATA GETTER =================
    private String get(String key) {
        if (data == null) return "";

        switch (key) {
            case "ORG_ID": return data.getORG_ID();
            case "DIVISION": return data.getDIVISION();
            case "SHIFT": return data.getSHIFT();
            case "PROB_PERIOD": return data.getPROB_PERIOD();
            case "CONFIRMING_ON": return data.getCONFIRMING_ON();

            case "REP_UNT": return data.getREP_UNT();
            case "REP_EMP_ID": return data.getREP_EMP_ID();
            case "REP_EMP_DESIG_CODE": return data.getREP_EMP_DESIG_CODE();
            case "REP_EMP_DEPT_CODE": return data.getREP_EMP_DEPT_CODE();
            case "REP_EMP_TYPE": return data.getREP_EMP_TYPE();

            case "BRANCH_CODE": return data.getBRANCH_CODE();
            case "BRANCH_NAME": return data.getBRANCH_NAME();

            case "DOB": return data.getDOB();
            case "CITY_OF_BIRTH": return data.getCITY_OF_BIRTH();
            case "NATIONALITY": return data.getNATIONALITY();
            case "RELIGION": return data.getRELIGION();
            case "BLOOD_GROUP": return data.getBLOOD_GROUP();
            case "M_STATUS": return data.getM_STATUS();
            case "MOTHER_NAME": return data.getMOTHER_NAME();

            case "BANK_NAME": return data.getBANK_NAME();
            case "BANK_AC_NO": return data.getBANK_AC_NO();
            case "SS_NO": return data.getSS_NO();
            case "EOBI_NO": return data.getEOBI_NO();
            case "TAX_NO": return data.getTAX_NO();
            case "EFU": return data.getEFU();
            case "EFU_NO": return data.getEFU_NO();
            case "CLIPPER_PFUND_CODE": return data.getCLIPPER_PFUND_CODE();

            case "EOBI_STATUS": return data.getEOBI_STATUS();
            case "NIC_VERIFY": return data.getNIC_VERIFY();
            case "NIC_VERIFY_DATE": return data.getNIC_VERIFY_DATE();
            case "HOD_CHECK": return data.getHOD_CHECK();
            case "CLEARANCE_STATUS": return data.getCLEARANCE_STATUS();
            case "DIS_CERTIFICATE": return data.getDIS_CERTIFICATE();

            case "EMERGENCY_NO": return data.getEMERGENCY_NO();
            case "ATT_CATEG": return data.getATT_CATEG();

            case "WELLNESS_CLUB": return data.getWELLNESS_CLUB();
            case "WELLNESS_CARD_ISSUE": return data.getWELLNESS_CARD_ISSUE();
            case "WELLNESS_CARD_NO": return data.getWELLNESS_CARD_NO();
            case "WELLNESS_CLUB_VALID_DATE": return data.getWELLNESS_CLUB_VALID_DATE();

            case "FIRST_DOSE": return data.getFIRST_DOSE();
            case "SECOND_DOSE": return data.getSECOND_DOSE();
            case "FIRST_VACC_DATE": return data.getFIRST_VACC_DATE();
            case "SECOND_VACC_DATE": return data.getSECOND_VACC_DATE();

            default: return "";
        }
    }

    // ================= EXTRACT UPDATED =================
    public Employee getUpdatedOtherDetails() {

        Employee emp = new Employee();

        fieldMap.forEach((label, field) -> {

            if (!field.isEditable()) return;

            String val = field.getText();

            if (isEmpty(val)) return;

            switch (label) {

                case "ORG ID": emp.setORG_ID(val); break;
                case "DIVISION": emp.setDIVISION(val); break;
                case "SHIFT": emp.setSHIFT(val); break;
                case "PROB PERIOD": emp.setPROB_PERIOD(val); break;
                case "CONFIRMING ON": emp.setCONFIRMING_ON(val); break;

                case "REP UNIT": emp.setREP_UNT(val); break;
                case "REP EMP ID": emp.setREP_EMP_ID(val); break;
                case "REP DESIG CODE": emp.setREP_EMP_DESIG_CODE(val); break;
                case "REP DEPT CODE": emp.setREP_EMP_DEPT_CODE(val); break;
                case "REP TYPE": emp.setREP_EMP_TYPE(val); break;

                case "BRANCH CODE": emp.setBRANCH_CODE(val); break;
                case "BRANCH NAME": emp.setBRANCH_NAME(val); break;

                case "DOB": emp.setDOB(val); break;
                case "City of Birth": emp.setCITY_OF_BIRTH(val); break;
                case "Nationality": emp.setNATIONALITY(val); break;
                case "Religion": emp.setRELIGION(val); break;
                case "Blood Group": emp.setBLOOD_GROUP(val); break;
                case "Marital Status": emp.setM_STATUS(val); break;
                case "Mother Name": emp.setMOTHER_NAME(val); break;

                case "Bank Name": emp.setBANK_NAME(val); break;
                case "Account No": emp.setBANK_AC_NO(val); break;
                case "SS No": emp.setSS_NO(val); break;
                case "EOBI No": emp.setEOBI_NO(val); break;
                case "Tax No": emp.setTAX_NO(val); break;
                case "EFU": emp.setEFU(val); break;
                case "EFU No": emp.setEFU_NO(val); break;
                case "PFUND Code": emp.setCLIPPER_PFUND_CODE(val); break;

                case "EOBI Status": emp.setEOBI_STATUS(val); break;
                case "NIC Verify": emp.setNIC_VERIFY(val); break;
                case "NIC Verify Date": emp.setNIC_VERIFY_DATE(val); break;
                case "HOD Check": emp.setHOD_CHECK(val); break;
                case "Clearance Status": emp.setCLEARANCE_STATUS(val); break;
                case "Dis Certificate": emp.setDIS_CERTIFICATE(val); break;

                case "Emergency No": emp.setEMERGENCY_NO(val); break;
                case "Attendance Category": emp.setATT_CATEG(val); break;

                case "Wellness Club": emp.setWELLNESS_CLUB(val); break;
                case "Card Issue": emp.setWELLNESS_CARD_ISSUE(val); break;
                case "Card No": emp.setWELLNESS_CARD_NO(val); break;
                case "Valid Date": emp.setWELLNESS_CLUB_VALID_DATE(val); break;

                case "First Dose": emp.setFIRST_DOSE(val); break;
                case "Second Dose": emp.setSECOND_DOSE(val); break;
                case "First Vacc Date": emp.setFIRST_VACC_DATE(val); break;
                case "Second Vacc Date": emp.setSECOND_VACC_DATE(val); break;
            }
        });

        return emp;
    }

    // ================= EMPTY CHECK =================
    private boolean isEmpty(String value) {
        if (value == null) return true;

        String v = value.trim();

        return v.isEmpty()
                || v.equalsIgnoreCase("N/A")
                || v.equalsIgnoreCase("NA");
    }
}