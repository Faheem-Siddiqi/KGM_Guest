package com.kgm.dao;

import com.kgm.model.Employee;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class EmployeeDao {

    private Connection conn;

    public EmployeeDao(Connection conn) {
        this.conn = conn;
    }

    public void insertEmployee(Employee e) {

        String sql = "INSERT INTO employees (" +
                "NID, EMP_NAME, DEPARTMENT, DESIGNATION, PERSONAL_EMAIL, " +
                "RESIGN_DATE, JOINING_DATE, EMP_CONTNO, PERMANENT_ADR, EMPLOYEE_CODE, " +
                "GENDER, RESIGN_REASON, " +   

                "CNIC_COPY, SS_CARD_COPY, EOBI_CARD_COPY, FINAL_SETTLEMENT, CLEARANCE_CERT, " +
                "JOB_APPOINTMENT, APPLICATION_DOC, ISSUANCE_DOC, SETTLEMENT_DOC, TRIAL_CARD, " +
                "INTERVIEW_DOC, SERVICE_LETTER, EXTENSION_LETTER, RETIREMENT_LETTER, COVID_CERT, " +
                "DISCIPLINARY_I, DISCIPLINARY_II, DISCIPLINARY_III, EMP_IMG" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, safe(e.getNID()));
            ps.setString(2, safe(e.getEMP_NAME()));
            ps.setString(3, safe(e.getDEPARTMENT()));
            ps.setString(4, safe(e.getDESIGNATION()));
            ps.setString(5, safe(e.getPERSONAL_EMAIL()));
            ps.setString(6, safe(e.getRESIGN_DATE()));
            ps.setString(7, safe(e.getJOINING_DATE()));
            ps.setString(8, safe(e.getEMP_CONTNO()));
            ps.setString(9, safe(e.getPERMANENT_ADR()));
            ps.setString(10, safe(e.getEMPLOYEE_CODE()));
            ps.setString(11, safe(e.getGENDER()));

            // ✅ NEW FIELD
            ps.setString(12, safe(e.getRESIGN_REASON()));

            ps.setString(13, safe(e.getCNIC_COPY()));
            ps.setString(14, safe(e.getSS_CARD_COPY()));
            ps.setString(15, safe(e.getEOBI_CARD_COPY()));
            ps.setString(16, safe(e.getFINAL_SETTLEMENT()));
            ps.setString(17, safe(e.getCLEARANCE_CERT()));
            ps.setString(18, safe(e.getJOB_APPOINTMENT()));
            ps.setString(19, safe(e.getAPPLICATION_DOC()));
            ps.setString(20, safe(e.getISSUANCE_DOC()));
            ps.setString(21, safe(e.getSETTLEMENT_DOC()));
            ps.setString(22, safe(e.getTRIAL_CARD()));
            ps.setString(23, safe(e.getINTERVIEW_DOC()));
            ps.setString(24, safe(e.getSERVICE_LETTER()));
            ps.setString(25, safe(e.getEXTENSION_LETTER()));
            ps.setString(26, safe(e.getRETIREMENT_LETTER()));
            ps.setString(27, safe(e.getCOVID_CERT()));
            ps.setString(28, safe(e.getDISCIPLINARY_I()));
            ps.setString(29, safe(e.getDISCIPLINARY_II()));
            ps.setString(30, safe(e.getDISCIPLINARY_III()));
            ps.setString(31, safe(e.getEMP_IMG()));

            ps.executeUpdate();

        } catch (SQLException ex) {
            throw new RuntimeException("Employee insert failed: " + ex.getMessage(), ex);
        }
    }

    // ================= NULL SAFE HELPER =================
    private String safe(String value) {
        return (value == null) ? "" : value;
    }
}