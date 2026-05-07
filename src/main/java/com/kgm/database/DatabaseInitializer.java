package com.kgm.database;

import com.kgm.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void init() {

        String employees = """
            CREATE TABLE IF NOT EXISTS employees (
                ID INT PRIMARY KEY AUTO_INCREMENT,

                -- CORE
                UNT_CODE VARCHAR(255) DEFAULT '',
                EMPLOYEE_CODE VARCHAR(255) UNIQUE DEFAULT '',
                EMP_NAME VARCHAR(255) DEFAULT '',
                FATHER_NAME VARCHAR(255) DEFAULT '',
                MOTHER_NAME VARCHAR(255) DEFAULT '',
                GENDER VARCHAR(255) DEFAULT '',
                DOB VARCHAR(255) DEFAULT '',
                CITY_OF_BIRTH VARCHAR(255) DEFAULT '',
                NATIONALITY VARCHAR(255) DEFAULT '',
                RELIGION VARCHAR(255) DEFAULT '',
                BLOOD_GROUP VARCHAR(255) DEFAULT '',
                M_STATUS VARCHAR(255) DEFAULT '',
                NID VARCHAR(255) DEFAULT '',

                -- EMPLOYMENT
                DEPARTMENT VARCHAR(255) DEFAULT '',
                DESIG_CODE VARCHAR(255) DEFAULT '',
                DESIGNATION VARCHAR(255) DEFAULT '',
                GRADE VARCHAR(255) DEFAULT '',
                JOINING_DATE VARCHAR(255) DEFAULT '',
                CONFIRMING_ON VARCHAR(255) DEFAULT '',
                EMP_STATUS VARCHAR(255) DEFAULT '',
                SHIFT VARCHAR(255) DEFAULT '',
                PROB_PERIOD VARCHAR(255) DEFAULT '',
                EXP_IN_KTML VARCHAR(255) DEFAULT '',
                APPLICATION_DATE VARCHAR(255) DEFAULT '',
                RESIGN_REASON VARCHAR(255) DEFAULT '',
                RESIGN_DATE VARCHAR(255) DEFAULT '',

                -- ORGANIZATION
                ORG_ID VARCHAR(255) DEFAULT '',
                DIVISION VARCHAR(255) DEFAULT '',
                BRANCH_CODE VARCHAR(255) DEFAULT '',
                BRANCH_NAME VARCHAR(255) DEFAULT '',
                DESCR VARCHAR(255) DEFAULT '',

                -- PAYROLL
                GROSS_SALARY VARCHAR(255) DEFAULT '',
                PAY_CATEGORY VARCHAR(255) DEFAULT '',
                BASIC VARCHAR(255) DEFAULT '',
                COLA1 VARCHAR(255) DEFAULT '',
                COLA2 VARCHAR(255) DEFAULT '',
                COLA3 VARCHAR(255) DEFAULT '',
                COLA4 VARCHAR(255) DEFAULT '',
                COLA5 VARCHAR(255) DEFAULT '',
                COLA6_7 VARCHAR(255) DEFAULT '',
                COLA8 VARCHAR(255) DEFAULT '',
                COLA9 VARCHAR(255) DEFAULT '',
                COLA10 VARCHAR(255) DEFAULT '',
                COLA11 VARCHAR(255) DEFAULT '',
                PB_SPECIAL1_2 VARCHAR(255) DEFAULT '',
                PB_SPECIAL3 VARCHAR(255) DEFAULT '',
                PB_SPECIAL4 VARCHAR(255) DEFAULT '',
                SPECIAL VARCHAR(255) DEFAULT '',
                OTHER1 VARCHAR(255) DEFAULT '',
                OTHER2 VARCHAR(255) DEFAULT '',
                OTHER3 VARCHAR(255) DEFAULT '',
                MEDICAL VARCHAR(255) DEFAULT '',
                CONVEYANCE VARCHAR(255) DEFAULT '',
                UTILITY VARCHAR(255) DEFAULT '',
                ENTERTAINMENT VARCHAR(255) DEFAULT '',
                PAY_GROUP VARCHAR(255) DEFAULT '',
                PAY_GROUP_DESC VARCHAR(255) DEFAULT '',
                PAY_AT_JOINING VARCHAR(255) DEFAULT '',
                EXTRA_DUTY VARCHAR(255) DEFAULT '',
                PAYROLL_FLAG VARCHAR(255) DEFAULT '',

                -- BANKING
                BANK_NAME VARCHAR(255) DEFAULT '',
                BANK_AC_NO VARCHAR(255) DEFAULT '',
                SS_NO VARCHAR(255) DEFAULT '',
                EOBI_NO VARCHAR(255) DEFAULT '',
                TAX_NO VARCHAR(255) DEFAULT '',
                PFUND_DEDUCTION VARCHAR(255) DEFAULT '',
                PF_INTEREST VARCHAR(255) DEFAULT '',
                PFUND_CODE VARCHAR(255) DEFAULT '',
                CLIPPER_PFUND_CODE VARCHAR(255) DEFAULT '',
                EFU VARCHAR(255) DEFAULT '',
                EFU_NO VARCHAR(255) DEFAULT '',
                EOBI_STATUS VARCHAR(255) DEFAULT '',

                -- CONTACT
                EMP_CONTNO VARCHAR(255) DEFAULT '',
                CURRENT_ADR VARCHAR(255) DEFAULT '',
                PERMANENT_ADR VARCHAR(255) DEFAULT '',
                PERSONAL_EMAIL VARCHAR(255) DEFAULT '',
                OFFICIAL_EMAIL VARCHAR(255) DEFAULT '',
                EMERGENCY_NO VARCHAR(255) DEFAULT '',

                -- REPORTING
                REP_UNT VARCHAR(255) DEFAULT '',
                REP_EMP_ID VARCHAR(255) DEFAULT '',
                REP_EMP_DESIG_CODE VARCHAR(255) DEFAULT '',
                REP_EMP_DEPT_CODE VARCHAR(255) DEFAULT '',
                REP_EMP_TYPE VARCHAR(255) DEFAULT '',

                -- COMPLIANCE
                FLAG VARCHAR(255) DEFAULT '',
                CLEARANCE_STATUS VARCHAR(255) DEFAULT '',
                HOD_CHECK VARCHAR(255) DEFAULT '',
                SEC_HEAD_CHK VARCHAR(255) DEFAULT '',
                NIC_VERIFY VARCHAR(255) DEFAULT '',
                NIC_VERIFY_DATE VARCHAR(255) DEFAULT '',
                ATT_CATEG VARCHAR(255) DEFAULT '',
                DIS_CERTIFICATE VARCHAR(255) DEFAULT '',

                -- BENEFITS
                WELLNESS_CLUB VARCHAR(255) DEFAULT '',
                WELLNESS_CARD_ISSUE VARCHAR(255) DEFAULT '',
                WELLNESS_CARD_NO VARCHAR(255) DEFAULT '',
                WELLNESS_CLUB_VALID_DATE VARCHAR(255) DEFAULT '',

                -- VACCINATION
                FIRST_DOSE VARCHAR(255) DEFAULT '',
                SECOND_DOSE VARCHAR(255) DEFAULT '',
                FIRST_VACC_DATE VARCHAR(255) DEFAULT '',
                SECOND_VACC_DATE VARCHAR(255) DEFAULT '',

                -- DOCUMENTS
                CNIC_COPY VARCHAR(255) DEFAULT '',
                SS_CARD_COPY VARCHAR(255) DEFAULT '',
                EOBI_CARD_COPY VARCHAR(255) DEFAULT '',
                FINAL_SETTLEMENT VARCHAR(255) DEFAULT '',
                CLEARANCE_CERT VARCHAR(255) DEFAULT '',
                JOB_APPOINTMENT VARCHAR(255) DEFAULT '',
                APPLICATION_DOC VARCHAR(255) DEFAULT '',
                ISSUANCE_DOC VARCHAR(255) DEFAULT '',
                SETTLEMENT_DOC VARCHAR(255) DEFAULT '',
                TRIAL_CARD VARCHAR(255) DEFAULT '',
                INTERVIEW_DOC VARCHAR(255) DEFAULT '',
                SERVICE_LETTER VARCHAR(255) DEFAULT '',
                EXTENSION_LETTER VARCHAR(255) DEFAULT '',
                RETIREMENT_LETTER VARCHAR(255) DEFAULT '',
                COVID_CERT VARCHAR(255) DEFAULT '',
                DISCIPLINARY_I VARCHAR(255) DEFAULT '',
                DISCIPLINARY_II VARCHAR(255) DEFAULT '',
                DISCIPLINARY_III VARCHAR(255) DEFAULT '',
                EMP_IMG VARCHAR(255) DEFAULT ''
            );
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name = 'employees'"
            );
            boolean exists = rs.next() && rs.getInt(1) > 0;
            rs.close();

            stmt.execute(employees);

            System.out.println(exists ? "=> Schema already exists." : "=> Schema created.");

        } catch (SQLException | IllegalStateException e) {
            System.out.println("=> Schema failed!");
            e.printStackTrace();
        }
    }
}
