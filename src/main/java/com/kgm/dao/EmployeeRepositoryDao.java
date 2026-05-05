package com.kgm.dao;

import com.kgm.config.DatabaseConnection;
import com.kgm.model.Employee;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeeRepositoryDao {
    private final Connection con;

    public EmployeeRepositoryDao() {
        this.con = DatabaseConnection.getConnection();
    }

    // ==============================
    // 🔹 SAFE VALUE HANDLER
    // ==============================
    private String safe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "N/A";
        }
        return value;
    }

    // ==============================
    // 🔹 PAGINATED LIST (LIMIT FIXED = 2500)
    // ==============================
    public List<Employee> getEmployees(int offset) {
        int limit = 2500; // FIXED PAGE SIZE
        List<Employee> list = new ArrayList<>();
        String sql = """
                    SELECT
                        ID,
                        EMPLOYEE_CODE,
                        EMP_NAME,
                        FATHER_NAME,
                        NID,
                        EMP_CONTNO,
                        PERSONAL_EMAIL,
                        DEPARTMENT,
                        DESIGNATION,
                        GENDER,
                        RESIGN_REASON,
                        JOINING_DATE,
                        RESIGN_DATE
                    FROM employees
                    ORDER BY ID DESC
                    LIMIT ? OFFSET ?
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Employee e = new Employee();
                e.setID(rs.getInt("ID"));
                e.setEMPLOYEE_CODE(safe(rs.getString("EMPLOYEE_CODE")));
                e.setEMP_NAME(safe(rs.getString("EMP_NAME")));
                e.setFATHER_NAME(safe(rs.getString("FATHER_NAME")));
                e.setNID(safe(rs.getString("NID")));
                e.setEMP_CONTNO(safe(rs.getString("EMP_CONTNO")));
                e.setPERSONAL_EMAIL(safe(rs.getString("PERSONAL_EMAIL")));
                e.setDEPARTMENT(safe(rs.getString("DEPARTMENT")));
                e.setDESIGNATION(safe(rs.getString("DESIGNATION")));
                e.setGENDER(safe(rs.getString("GENDER")));
                e.setRESIGN_REASON(safe(rs.getString("RESIGN_REASON")));
                e.setJOINING_DATE(safe(rs.getString("JOINING_DATE")));
                e.setRESIGN_DATE(safe(rs.getString("RESIGN_DATE")));
                list.add(e);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return list;
    }

    // ==============================
    // 🔹 TOTAL COUNT
    // ==============================
    public int countEmployees() {
        String sql = "SELECT COUNT(*) FROM employees";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return 0;
    }

    // ==============================
    // 🔹 SEARCH BY EMPLOYEE CODE (INDEXED)
    // ==============================
    public Employee getEmployeeByCode(String empCode) {
        String sql = """
                    SELECT
                        ID,
                        EMPLOYEE_CODE,
                        EMP_NAME,
                        FATHER_NAME,
                        NID,
                        EMP_CONTNO,
                        PERSONAL_EMAIL,
                        DEPARTMENT,
                        DESIGNATION,
                        GENDER,
                        RESIGN_REASON,
                        JOINING_DATE,
                        RESIGN_DATE
                    FROM employees
                    WHERE EMPLOYEE_CODE = ?
                    LIMIT 1
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, empCode);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Employee e = new Employee();
                e.setID(rs.getInt("ID"));
                e.setEMPLOYEE_CODE(safe(rs.getString("EMPLOYEE_CODE")));
                e.setEMP_NAME(safe(rs.getString("EMP_NAME")));
                e.setFATHER_NAME(safe(rs.getString("FATHER_NAME")));
                e.setNID(safe(rs.getString("NID")));
                e.setEMP_CONTNO(safe(rs.getString("EMP_CONTNO")));
                e.setPERSONAL_EMAIL(safe(rs.getString("PERSONAL_EMAIL")));
                e.setDEPARTMENT(rs.getString("DEPARTMENT"));
                e.setDESIGNATION(safe(rs.getString("DESIGNATION")));
                e.setGENDER(safe(rs.getString("GENDER")));
                e.setRESIGN_REASON(safe(rs.getString("RESIGN_REASON")));
                e.setJOINING_DATE(safe(rs.getString("JOINING_DATE")));
                e.setRESIGN_DATE(safe(rs.getString("RESIGN_DATE")));
                return e;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    // ==============================
    // 🔹 FULL EMPLOYEE BY CODE (ALL FIELDS)
    // ==============================
    public Employee getFullEmployeeByCode(String empCode) {
        String sql = "SELECT * FROM employees WHERE EMPLOYEE_CODE = ? LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, empCode);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Employee e = new Employee();

                // ================= BASIC =================
                e.setID(rs.getInt("ID"));
                e.setUNT_CODE(safe(rs.getString("UNT_CODE")));
                e.setEMPLOYEE_CODE(safe(rs.getString("EMPLOYEE_CODE")));
                e.setEMP_NAME(safe(rs.getString("EMP_NAME")));
                e.setFATHER_NAME(safe(rs.getString("FATHER_NAME")));
                e.setMOTHER_NAME(safe(rs.getString("MOTHER_NAME")));
                e.setGENDER(safe(rs.getString("GENDER")));
                e.setDOB(safe(rs.getString("DOB")));
                e.setCITY_OF_BIRTH(safe(rs.getString("CITY_OF_BIRTH")));
                e.setNATIONALITY(safe(rs.getString("NATIONALITY")));
                e.setRELIGION(safe(rs.getString("RELIGION")));
                e.setBLOOD_GROUP(safe(rs.getString("BLOOD_GROUP")));
                e.setM_STATUS(safe(rs.getString("M_STATUS")));
                e.setNID(safe(rs.getString("NID")));

                // ================= EMPLOYMENT =================
                e.setDEPARTMENT(safe(rs.getString("DEPARTMENT")));
                e.setDESIG_CODE(safe(rs.getString("DESIG_CODE")));
                e.setDESIGNATION(safe(rs.getString("DESIGNATION")));
                e.setGRADE(safe(rs.getString("GRADE")));
                e.setJOINING_DATE(safe(rs.getString("JOINING_DATE")));
                e.setCONFIRMING_ON(safe(rs.getString("CONFIRMING_ON")));
                e.setEMP_STATUS(safe(rs.getString("EMP_STATUS")));
                e.setSHIFT(safe(rs.getString("SHIFT")));
                e.setPROB_PERIOD(safe(rs.getString("PROB_PERIOD")));
                e.setEXP_IN_KTML(safe(rs.getString("EXP_IN_KTML")));
                e.setAPPLICATION_DATE(safe(rs.getString("APPLICATION_DATE")));
                e.setRESIGN_REASON(safe(rs.getString("RESIGN_REASON")));
                e.setRESIGN_DATE(safe(rs.getString("RESIGN_DATE")));

                // ================= ORGANIZATION =================
                e.setORG_ID(safe(rs.getString("ORG_ID")));
                e.setDIVISION(safe(rs.getString("DIVISION")));
                e.setBRANCH_CODE(safe(rs.getString("BRANCH_CODE")));
                e.setBRANCH_NAME(safe(rs.getString("BRANCH_NAME")));
                e.setDESCR(safe(rs.getString("DESCR")));

                // ================= PAYROLL =================
                e.setGROSS_SALARY(safe(rs.getString("GROSS_SALARY")));
                e.setPAY_CATEGORY(safe(rs.getString("PAY_CATEGORY")));
                e.setBASIC(safe(rs.getString("BASIC")));
                e.setCOLA1(safe(rs.getString("COLA1")));
                e.setCOLA2(safe(rs.getString("COLA2")));
                e.setCOLA3(safe(rs.getString("COLA3")));
                e.setCOLA4(safe(rs.getString("COLA4")));
                e.setCOLA5(safe(rs.getString("COLA5")));
                e.setCOLA6_7(safe(rs.getString("COLA6_7")));
                e.setCOLA8(safe(rs.getString("COLA8")));
                e.setCOLA9(safe(rs.getString("COLA9")));
                e.setCOLA10(safe(rs.getString("COLA10")));
                e.setCOLA11(safe(rs.getString("COLA11")));

                e.setPB_SPECIAL1_2(safe(rs.getString("PB_SPECIAL1_2")));
                e.setPB_SPECIAL3(safe(rs.getString("PB_SPECIAL3")));
                e.setPB_SPECIAL4(safe(rs.getString("PB_SPECIAL4")));
                e.setSPECIAL(safe(rs.getString("SPECIAL")));

                e.setOTHER1(safe(rs.getString("OTHER1")));
                e.setOTHER2(safe(rs.getString("OTHER2")));
                e.setOTHER3(safe(rs.getString("OTHER3")));

                e.setMEDICAL(safe(rs.getString("MEDICAL")));
                e.setCONVEYANCE(safe(rs.getString("CONVEYANCE")));
                e.setUTILITY(safe(rs.getString("UTILITY")));
                e.setENTERTAINMENT(safe(rs.getString("ENTERTAINMENT")));

                e.setPAY_GROUP(safe(rs.getString("PAY_GROUP")));
                e.setPAY_GROUP_DESC(safe(rs.getString("PAY_GROUP_DESC")));
                e.setPAY_AT_JOINING(safe(rs.getString("PAY_AT_JOINING")));
                e.setEXTRA_DUTY(safe(rs.getString("EXTRA_DUTY")));
                e.setPAYROLL_FLAG(safe(rs.getString("PAYROLL_FLAG")));

                // ================= BANKING =================
                e.setBANK_NAME(safe(rs.getString("BANK_NAME")));
                e.setBANK_AC_NO(safe(rs.getString("BANK_AC_NO")));
                e.setSS_NO(safe(rs.getString("SS_NO")));
                e.setEOBI_NO(safe(rs.getString("EOBI_NO")));
                e.setTAX_NO(safe(rs.getString("TAX_NO")));
                e.setPFUND_DEDUCTION(safe(rs.getString("PFUND_DEDUCTION")));
                e.setPF_INTEREST(safe(rs.getString("PF_INTEREST")));
                e.setPFUND_CODE(safe(rs.getString("PFUND_CODE")));
                e.setCLIPPER_PFUND_CODE(safe(rs.getString("CLIPPER_PFUND_CODE")));
                e.setEFU(safe(rs.getString("EFU")));
                e.setEFU_NO(safe(rs.getString("EFU_NO")));
                e.setEOBI_STATUS(safe(rs.getString("EOBI_STATUS")));

                // ================= CONTACT =================
                e.setEMP_CONTNO(safe(rs.getString("EMP_CONTNO")));
                e.setCURRENT_ADR(safe(rs.getString("CURRENT_ADR")));
                e.setPERMANENT_ADR(safe(rs.getString("PERMANENT_ADR")));
                e.setPERSONAL_EMAIL(safe(rs.getString("PERSONAL_EMAIL")));
                e.setOFFICIAL_EMAIL(safe(rs.getString("OFFICIAL_EMAIL")));
                e.setEMERGENCY_NO(safe(rs.getString("EMERGENCY_NO")));

                // ================= REPORTING =================
                e.setREP_UNT(safe(rs.getString("REP_UNT")));
                e.setREP_EMP_ID(safe(rs.getString("REP_EMP_ID")));
                e.setREP_EMP_DESIG_CODE(safe(rs.getString("REP_EMP_DESIG_CODE")));
                e.setREP_EMP_DEPT_CODE(safe(rs.getString("REP_EMP_DEPT_CODE")));
                e.setREP_EMP_TYPE(safe(rs.getString("REP_EMP_TYPE")));

                // ================= COMPLIANCE =================
                e.setFLAG(safe(rs.getString("FLAG")));
                e.setCLEARANCE_STATUS(safe(rs.getString("CLEARANCE_STATUS")));
                e.setHOD_CHECK(safe(rs.getString("HOD_CHECK")));
                e.setSEC_HEAD_CHK(safe(rs.getString("SEC_HEAD_CHK")));
                e.setNIC_VERIFY(safe(rs.getString("NIC_VERIFY")));
                e.setNIC_VERIFY_DATE(safe(rs.getString("NIC_VERIFY_DATE")));
                e.setATT_CATEG(safe(rs.getString("ATT_CATEG")));
                e.setDIS_CERTIFICATE(safe(rs.getString("DIS_CERTIFICATE")));

                // ================= BENEFITS =================
                e.setWELLNESS_CLUB(safe(rs.getString("WELLNESS_CLUB")));
                e.setWELLNESS_CARD_ISSUE(safe(rs.getString("WELLNESS_CARD_ISSUE")));
                e.setWELLNESS_CARD_NO(safe(rs.getString("WELLNESS_CARD_NO")));
                e.setWELLNESS_CLUB_VALID_DATE(safe(rs.getString("WELLNESS_CLUB_VALID_DATE")));

                // ================= VACCINATION =================
                e.setFIRST_DOSE(safe(rs.getString("FIRST_DOSE")));
                e.setSECOND_DOSE(safe(rs.getString("SECOND_DOSE")));
                e.setFIRST_VACC_DATE(safe(rs.getString("FIRST_VACC_DATE")));
                e.setSECOND_VACC_DATE(safe(rs.getString("SECOND_VACC_DATE")));

                // ================= DOCUMENTS =================
                e.setCNIC_COPY(safe(rs.getString("CNIC_COPY")));
                e.setSS_CARD_COPY(safe(rs.getString("SS_CARD_COPY")));
                e.setEOBI_CARD_COPY(safe(rs.getString("EOBI_CARD_COPY")));
                e.setFINAL_SETTLEMENT(safe(rs.getString("FINAL_SETTLEMENT")));
                e.setCLEARANCE_CERT(safe(rs.getString("CLEARANCE_CERT")));
                e.setJOB_APPOINTMENT(safe(rs.getString("JOB_APPOINTMENT")));
                e.setAPPLICATION_DOC(safe(rs.getString("APPLICATION_DOC")));
                e.setISSUANCE_DOC(safe(rs.getString("ISSUANCE_DOC")));
                e.setSETTLEMENT_DOC(safe(rs.getString("SETTLEMENT_DOC")));
                e.setTRIAL_CARD(safe(rs.getString("TRIAL_CARD")));
                e.setINTERVIEW_DOC(safe(rs.getString("INTERVIEW_DOC")));
                e.setSERVICE_LETTER(safe(rs.getString("SERVICE_LETTER")));
                e.setEXTENSION_LETTER(safe(rs.getString("EXTENSION_LETTER")));
                e.setRETIREMENT_LETTER(safe(rs.getString("RETIREMENT_LETTER")));
                e.setCOVID_CERT(safe(rs.getString("COVID_CERT")));
                e.setDISCIPLINARY_I(safe(rs.getString("DISCIPLINARY_I")));
                e.setDISCIPLINARY_II(safe(rs.getString("DISCIPLINARY_II")));
                e.setDISCIPLINARY_III(safe(rs.getString("DISCIPLINARY_III")));
                e.setEMP_IMG(safe(rs.getString("EMP_IMG")));

                return e;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }



    // ==============================
// 🔹 GENERIC OBJECT-BASED UPDATE
// ==============================
public void updateEmployeeDynamic(Employee emp) throws Exception {

    StringBuilder sql = new StringBuilder("UPDATE employees SET ");
    List<Object> values = new ArrayList<>();

    // 🔥 reflect all fields of Employee class
    java.lang.reflect.Field[] fields = Employee.class.getDeclaredFields();

    for (java.lang.reflect.Field field : fields) {

        field.setAccessible(true);
        String column = field.getName();

        // ❌ skip ID and primary key
        if (column.equalsIgnoreCase("ID") || column.equalsIgnoreCase("EMPLOYEE_CODE")) {
            continue;
        }

        Object value = field.get(emp);

        // only update if value is NOT empty
        if (value != null) {
            String strVal = value.toString().trim();

            if (!strVal.isEmpty() && !strVal.equalsIgnoreCase("N/A")) {

                sql.append(column).append(" = ?, ");
                values.add(strVal);
            }
        }
    }

    // remove last comma
    if (values.isEmpty()) {
        return; // nothing to update
    }

    sql.setLength(sql.length() - 2);

    sql.append(" WHERE EMPLOYEE_CODE = ?");
    values.add(emp.getEMPLOYEE_CODE());

    try (PreparedStatement ps = con.prepareStatement(sql.toString())) {

        for (int i = 0; i < values.size(); i++) {
            ps.setObject(i + 1, values.get(i));
        }

        ps.executeUpdate();
    }
}}
