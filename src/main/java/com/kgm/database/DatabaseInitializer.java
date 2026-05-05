package com.kgm.database;
import com.kgm.config.DatabaseConnection;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;

public class DatabaseInitializer {

    public static void init() {

        String employees = """
            CREATE TABLE IF NOT EXISTS employees (
                id INTEGER PRIMARY KEY AUTOINCREMENT,

                -- CORE
                UNT_CODE TEXT DEFAULT '',
                EMPLOYEE_CODE TEXT UNIQUE DEFAULT '',
                EMP_NAME TEXT DEFAULT '',
                FATHER_NAME TEXT DEFAULT '',
                MOTHER_NAME TEXT DEFAULT '',
                GENDER TEXT DEFAULT '',
                DOB TEXT DEFAULT '',
                CITY_OF_BIRTH TEXT DEFAULT '',
                NATIONALITY TEXT DEFAULT '',
                RELIGION TEXT DEFAULT '',
                BLOOD_GROUP TEXT DEFAULT '',
                M_STATUS TEXT DEFAULT '',
                NID TEXT DEFAULT '',

                -- EMPLOYMENT
                DEPARTMENT TEXT DEFAULT '',
                DESIG_CODE TEXT DEFAULT '',
                DESIGNATION TEXT DEFAULT '',
                GRADE TEXT DEFAULT '',
                JOINING_DATE TEXT DEFAULT '',
                CONFIRMING_ON TEXT DEFAULT '',
                EMP_STATUS TEXT DEFAULT '',
                SHIFT TEXT DEFAULT '',
                PROB_PERIOD TEXT DEFAULT '',
                EXP_IN_KTML TEXT DEFAULT '',
                APPLICATION_DATE TEXT DEFAULT '',
                RESIGN_REASON TEXT DEFAULT '',
                RESIGN_DATE TEXT DEFAULT '',

                -- ORGANIZATION
                ORG_ID TEXT DEFAULT '',
                DIVISION TEXT DEFAULT '',
                BRANCH_CODE TEXT DEFAULT '',
                BRANCH_NAME TEXT DEFAULT '',
                DESCR TEXT DEFAULT '',

                -- PAYROLL
                GROSS_SALARY TEXT DEFAULT '',
                PAY_CATEGORY TEXT DEFAULT '',
                BASIC TEXT DEFAULT '',
                COLA1 TEXT DEFAULT '',
                COLA2 TEXT DEFAULT '',
                COLA3 TEXT DEFAULT '',
                COLA4 TEXT DEFAULT '',
                COLA5 TEXT DEFAULT '',
                COLA6_7 TEXT DEFAULT '',
                COLA8 TEXT DEFAULT '',
                COLA9 TEXT DEFAULT '',
                COLA10 TEXT DEFAULT '',
                COLA11 TEXT DEFAULT '',
                PB_SPECIAL1_2 TEXT DEFAULT '',
                PB_SPECIAL3 TEXT DEFAULT '',
                PB_SPECIAL4 TEXT DEFAULT '',
                SPECIAL TEXT DEFAULT '',
                OTHER1 TEXT DEFAULT '',
                OTHER2 TEXT DEFAULT '',
                OTHER3 TEXT DEFAULT '',
                MEDICAL TEXT DEFAULT '',
                CONVEYANCE TEXT DEFAULT '',
                UTILITY TEXT DEFAULT '',
                ENTERTAINMENT TEXT DEFAULT '',
                PAY_GROUP TEXT DEFAULT '',
                PAY_GROUP_DESC TEXT DEFAULT '',
                PAY_AT_JOINING TEXT DEFAULT '',
                EXTRA_DUTY TEXT DEFAULT '',
                PAYROLL_FLAG TEXT DEFAULT '',

                -- BANKING
                BANK_NAME TEXT DEFAULT '',
                BANK_AC_NO TEXT DEFAULT '',
                SS_NO TEXT DEFAULT '',
                EOBI_NO TEXT DEFAULT '',
                TAX_NO TEXT DEFAULT '',
                PFUND_DEDUCTION TEXT DEFAULT '',
                PF_INTEREST TEXT DEFAULT '',
                PFUND_CODE TEXT DEFAULT '',
                CLIPPER_PFUND_CODE TEXT DEFAULT '',
                EFU TEXT DEFAULT '',
                EFU_NO TEXT DEFAULT '',
                EOBI_STATUS TEXT DEFAULT '',

                -- CONTACT
                EMP_CONTNO TEXT DEFAULT '',
                CURRENT_ADR TEXT DEFAULT '',
                PERMANENT_ADR TEXT DEFAULT '',
                PERSONAL_EMAIL TEXT DEFAULT '',
                OFFICIAL_EMAIL TEXT DEFAULT '',
                EMERGENCY_NO TEXT DEFAULT '',

                -- REPORTING
                REP_UNT TEXT DEFAULT '',
                REP_EMP_ID TEXT DEFAULT '',
                REP_EMP_DESIG_CODE TEXT DEFAULT '',
                REP_EMP_DEPT_CODE TEXT DEFAULT '',
                REP_EMP_TYPE TEXT DEFAULT '',

                -- COMPLIANCE
                FLAG TEXT DEFAULT '',
                CLEARANCE_STATUS TEXT DEFAULT '',
                HOD_CHECK TEXT DEFAULT '',
                SEC_HEAD_CHK TEXT DEFAULT '',
                NIC_VERIFY TEXT DEFAULT '',
                NIC_VERIFY_DATE TEXT DEFAULT '',
                ATT_CATEG TEXT DEFAULT '',
                DIS_CERTIFICATE TEXT DEFAULT '',

                -- BENEFITS
                WELLNESS_CLUB TEXT DEFAULT '',
                WELLNESS_CARD_ISSUE TEXT DEFAULT '',
                WELLNESS_CARD_NO TEXT DEFAULT '',
                WELLNESS_CLUB_VALID_DATE TEXT DEFAULT '',

                -- VACCINATION
                FIRST_DOSE TEXT DEFAULT '',
                SECOND_DOSE TEXT DEFAULT '',
                FIRST_VACC_DATE TEXT DEFAULT '',
                SECOND_VACC_DATE TEXT DEFAULT '',

                -- DOCUMENTS
                CNIC_COPY TEXT DEFAULT '',
                SS_CARD_COPY TEXT DEFAULT '',
                EOBI_CARD_COPY TEXT DEFAULT '',
                FINAL_SETTLEMENT TEXT DEFAULT '',
                CLEARANCE_CERT TEXT DEFAULT '',
                JOB_APPOINTMENT TEXT DEFAULT '',
                APPLICATION_DOC TEXT DEFAULT '',
                ISSUANCE_DOC TEXT DEFAULT '',
                SETTLEMENT_DOC TEXT DEFAULT '',
                TRIAL_CARD TEXT DEFAULT '',
                INTERVIEW_DOC TEXT DEFAULT '',
                SERVICE_LETTER TEXT DEFAULT '',
                EXTENSION_LETTER TEXT DEFAULT '',
                RETIREMENT_LETTER TEXT DEFAULT '',
                COVID_CERT TEXT DEFAULT '',
                DISCIPLINARY_I TEXT DEFAULT '',
                DISCIPLINARY_II TEXT DEFAULT '',
                DISCIPLINARY_III TEXT DEFAULT '',
                EMP_IMG TEXT DEFAULT ''
            );
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='employees'"
            );
            boolean exists = rs.next();
            rs.close();

            stmt.execute(employees);

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_emp_code ON employees(EMPLOYEE_CODE);");

            System.out.println(exists ? "=> Schema already exists." : "=> Schema created.");

        } catch (SQLException e) {
            System.out.println("=> Schema failed!");
            e.printStackTrace();
        }
    }
}