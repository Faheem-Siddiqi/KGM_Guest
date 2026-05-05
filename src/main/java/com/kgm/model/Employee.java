package com.kgm.model;
public class Employee {
    // ================= EMPLOYEES =================
    private int ID;
    private String UNT_CODE;
    private String EMPLOYEE_CODE;
    private String EMP_NAME;
    private String FATHER_NAME;
    private String MOTHER_NAME;
    private String GENDER;
    private String DOB;
    private String CITY_OF_BIRTH;
    private String NATIONALITY;
    private String RELIGION;
    private String BLOOD_GROUP;
    private String M_STATUS;
    private String NID;
    // ================= EMPLOYMENT =================
    private String DEPARTMENT;
    private String DESIG_CODE;
    private String DESIGNATION;
    private String GRADE;
    private String JOINING_DATE;
    private String CONFIRMING_ON;
    private String EMP_STATUS;
    private String SHIFT;
    private String PROB_PERIOD;
    private String EXP_IN_KTML;
    private String APPLICATION_DATE;
    private String RESIGN_REASON;
    private String RESIGN_DATE;
    // ================= ORGANIZATION =================
    private String ORG_ID;
    private String DIVISION;
    private String BRANCH_CODE;
    private String BRANCH_NAME;
    private String DESCR;
    // ================= PAYROLL =================
    private String GROSS_SALARY;
    private String PAY_CATEGORY;
    private String BASIC;
    private String COLA1;
    private String COLA2;
    private String COLA3;
    private String COLA4;
    private String COLA5;
    private String COLA6_7;
    private String COLA8;
    private String COLA9;
    private String COLA10;
    private String COLA11;
    private String PB_SPECIAL1_2;
    private String PB_SPECIAL3;
    private String PB_SPECIAL4;
    private String SPECIAL;
    private String OTHER1;
    private String OTHER2;
    private String OTHER3;
    private String MEDICAL;
    private String CONVEYANCE;
    private String UTILITY;
    private String ENTERTAINMENT;
    private String PAY_GROUP;
    private String PAY_GROUP_DESC;
    private String PAY_AT_JOINING;
    private String EXTRA_DUTY;
    private String PAYROLL_FLAG;
    // ================= BANKING =================
    private String BANK_NAME;
    private String BANK_AC_NO;
    private String SS_NO;
    private String EOBI_NO;
    private String TAX_NO;
    private String PFUND_DEDUCTION;
    private String PF_INTEREST;
    private String PFUND_CODE;
    private String CLIPPER_PFUND_CODE;
    private String EFU;
    private String EFU_NO;
    private String EOBI_STATUS;
    // ================= CONTACT =================
    private String EMP_CONTNO;
    private String CURRENT_ADR;
    private String PERMANENT_ADR;
    private String PERSONAL_EMAIL;
    private String OFFICIAL_EMAIL;
    private String EMERGENCY_NO;
    // ================= REPORTING =================
    private String REP_UNT;
    private String REP_EMP_ID;
    private String REP_EMP_DESIG_CODE;
    private String REP_EMP_DEPT_CODE;
    private String REP_EMP_TYPE;
    // ================= COMPLIANCE =================
    private String FLAG;
    private String CLEARANCE_STATUS;
    private String HOD_CHECK;
    private String SEC_HEAD_CHK;
    private String NIC_VERIFY;
    private String NIC_VERIFY_DATE;
    private String ATT_CATEG;
    private String DIS_CERTIFICATE;
    // ================= BENEFITS =================
    private String WELLNESS_CLUB;
    private String WELLNESS_CARD_ISSUE;
    private String WELLNESS_CARD_NO;
    private String WELLNESS_CLUB_VALID_DATE;
    // ================= VACCINATION =================
    private String FIRST_DOSE;
    private String SECOND_DOSE;
    private String FIRST_VACC_DATE;
    private String SECOND_VACC_DATE;
    // ================= DOCUMENTS =================
    private String CNIC_COPY;
    private String SS_CARD_COPY;
    private String EOBI_CARD_COPY;
    private String FINAL_SETTLEMENT;
    private String CLEARANCE_CERT;
    private String JOB_APPOINTMENT;
    private String APPLICATION_DOC;
    private String ISSUANCE_DOC;
    private String SETTLEMENT_DOC;
    private String TRIAL_CARD;
    private String INTERVIEW_DOC;
    private String SERVICE_LETTER;
    private String EXTENSION_LETTER;
    private String RETIREMENT_LETTER;
    private String COVID_CERT;
    private String DISCIPLINARY_I;
    private String DISCIPLINARY_II;
    private String DISCIPLINARY_III;
    private String EMP_IMG;
    public Employee() {
    }
    // ================= BASIC GETTERS =================
    public int getID() {
        return ID;
    }
    public void setID(int ID) {
        this.ID = ID;
    }
    public String getNID() {
        return NID;
    }
    public void setNID(String NID) {
        this.NID = NID;
    }
    public String getEMPLOYEE_CODE() {
        return EMPLOYEE_CODE;
    }
    public void setEMPLOYEE_CODE(String EMPLOYEE_CODE) {
        this.EMPLOYEE_CODE = EMPLOYEE_CODE;
    }
    public String getEMP_NAME() {
        return EMP_NAME;
    }
    public void setEMP_NAME(String EMP_NAME) {
        this.EMP_NAME = EMP_NAME;
    }
    public String getGENDER() {
        return GENDER;
    }
    public void setGENDER(String GENDER) {
        this.GENDER = GENDER;
    }
    public String getFATHER_NAME() {
        return FATHER_NAME;
    }
    public void setFATHER_NAME(String FATHER_NAME) {
        this.FATHER_NAME = FATHER_NAME;
    }
    public String getDEPARTMENT() {
        return DEPARTMENT;
    }
    public void setDEPARTMENT(String DEPARTMENT) {
        this.DEPARTMENT = DEPARTMENT;
    }
    public String getDESIGNATION() {
        return DESIGNATION;
    }
    public void setDESIGNATION(String DESIGNATION) {
        this.DESIGNATION = DESIGNATION;
    }
    public String getJOINING_DATE() {
        return JOINING_DATE;
    }
    public void setJOINING_DATE(String JOINING_DATE) {
        this.JOINING_DATE = JOINING_DATE;
    }
    public String getRESIGN_DATE() {
        return RESIGN_DATE;
    }
    public void setRESIGN_DATE(String RESIGN_DATE) {
        this.RESIGN_DATE = RESIGN_DATE;
    }
    public String getRESIGN_REASON() {
        return RESIGN_REASON;
    }
    public void setRESIGN_REASON(String RESIGN_REASON) {
        this.RESIGN_REASON = RESIGN_REASON;
    }
    public String getUNT_CODE() {
        return UNT_CODE;
    }
    public void setUNT_CODE(String UNT_CODE) {
        this.UNT_CODE = UNT_CODE;
    }
    public String getEMP_STATUS() {
        return EMP_STATUS;
    }
    public void setEMP_STATUS(String EMP_STATUS) {
        this.EMP_STATUS = EMP_STATUS;
    }
    public String getGROSS_SALARY() {
        return GROSS_SALARY;
    }
    public void setGROSS_SALARY(String GROSS_SALARY) {
        this.GROSS_SALARY = GROSS_SALARY;
    }
    public String getBANK_NAME() {
        return BANK_NAME;
    }
    public void setBANK_NAME(String BANK_NAME) {
        this.BANK_NAME = BANK_NAME;
    }
    public String getBANK_AC_NO() {
        return BANK_AC_NO;
    }
    public void setBANK_AC_NO(String BANK_AC_NO) {
        this.BANK_AC_NO = BANK_AC_NO;
    }
    public String getEMP_CONTNO() {
        return EMP_CONTNO;
    }
    public void setEMP_CONTNO(String EMP_CONTNO) {
        this.EMP_CONTNO = EMP_CONTNO;
    }
    public String getPERSONAL_EMAIL() {
        return PERSONAL_EMAIL;
    }
    public void setPERSONAL_EMAIL(String PERSONAL_EMAIL) {
        this.PERSONAL_EMAIL = PERSONAL_EMAIL;
    }
    public String getOFFICIAL_EMAIL() {
        return OFFICIAL_EMAIL;
    }
    public void setOFFICIAL_EMAIL(String OFFICIAL_EMAIL) {
        this.OFFICIAL_EMAIL = OFFICIAL_EMAIL;
    }
    public String getEMERGENCY_NO() {
        return EMERGENCY_NO;
    }
    public void setEMERGENCY_NO(String EMERGENCY_NO) {
        this.EMERGENCY_NO = EMERGENCY_NO;
    }
    public String getPERMANENT_ADR() {
        return PERMANENT_ADR;
    }
    public void setPERMANENT_ADR(String PERMANENT_ADR) {
        this.PERMANENT_ADR = PERMANENT_ADR;
    }
    // ================= DOCUMENT GETTERS =================
    public String getCNIC_COPY() {
        return CNIC_COPY;
    }
    public void setCNIC_COPY(String CNIC_COPY) {
        this.CNIC_COPY = CNIC_COPY;
    }
    public String getSS_CARD_COPY() {
        return SS_CARD_COPY;
    }
    public void setSS_CARD_COPY(String SS_CARD_COPY) {
        this.SS_CARD_COPY = SS_CARD_COPY;
    }
    public String getEOBI_CARD_COPY() {
        return EOBI_CARD_COPY;
    }
    public void setEOBI_CARD_COPY(String EOBI_CARD_COPY) {
        this.EOBI_CARD_COPY = EOBI_CARD_COPY;
    }
    public String getEMP_IMG() {
        return EMP_IMG;
    }
    public void setEMP_IMG(String EMP_IMG) {
        this.EMP_IMG = EMP_IMG;
    }
    public String getFINAL_SETTLEMENT() {
        return FINAL_SETTLEMENT;
    }
    public void setFINAL_SETTLEMENT(String FINAL_SETTLEMENT) {
        this.FINAL_SETTLEMENT = FINAL_SETTLEMENT;
    }
    public String getCLEARANCE_CERT() {
        return CLEARANCE_CERT;
    }
    public void setCLEARANCE_CERT(String CLEARANCE_CERT) {
        this.CLEARANCE_CERT = CLEARANCE_CERT;
    }
    public String getJOB_APPOINTMENT() {
        return JOB_APPOINTMENT;
    }
    public void setJOB_APPOINTMENT(String JOB_APPOINTMENT) {
        this.JOB_APPOINTMENT = JOB_APPOINTMENT;
    }
    public String getAPPLICATION_DOC() {
        return APPLICATION_DOC;
    }
    public void setAPPLICATION_DOC(String APPLICATION_DOC) {
        this.APPLICATION_DOC = APPLICATION_DOC;
    }
    public String getISSUANCE_DOC() {
        return ISSUANCE_DOC;
    }
    public void setISSUANCE_DOC(String ISSUANCE_DOC) {
        this.ISSUANCE_DOC = ISSUANCE_DOC;
    }
    public String getSETTLEMENT_DOC() {
        return SETTLEMENT_DOC;
    }
    public void setSETTLEMENT_DOC(String SETTLEMENT_DOC) {
        this.SETTLEMENT_DOC = SETTLEMENT_DOC;
    }
    public String getTRIAL_CARD() {
        return TRIAL_CARD;
    }
    public void setTRIAL_CARD(String TRIAL_CARD) {
        this.TRIAL_CARD = TRIAL_CARD;
    }
    public String getINTERVIEW_DOC() {
        return INTERVIEW_DOC;
    }
    public void setINTERVIEW_DOC(String INTERVIEW_DOC) {
        this.INTERVIEW_DOC = INTERVIEW_DOC;
    }
    public String getSERVICE_LETTER() {
        return SERVICE_LETTER;
    }
    public void setSERVICE_LETTER(String SERVICE_LETTER) {
        this.SERVICE_LETTER = SERVICE_LETTER;
    }
    public String getEXTENSION_LETTER() {
        return EXTENSION_LETTER;
    }
    public void setEXTENSION_LETTER(String EXTENSION_LETTER) {
        this.EXTENSION_LETTER = EXTENSION_LETTER;
    }
    public String getRETIREMENT_LETTER() {
        return RETIREMENT_LETTER;
    }
    public void setRETIREMENT_LETTER(String RETIREMENT_LETTER) {
        this.RETIREMENT_LETTER = RETIREMENT_LETTER;
    }
    public String getCOVID_CERT() {
        return COVID_CERT;
    }
    public void setCOVID_CERT(String COVID_CERT) {
        this.COVID_CERT = COVID_CERT;
    }
    public String getDISCIPLINARY_I() {
        return DISCIPLINARY_I;
    }
    public void setDISCIPLINARY_I(String DISCIPLINARY_I) {
        this.DISCIPLINARY_I = DISCIPLINARY_I;
    }
    public String getDISCIPLINARY_II() {
        return DISCIPLINARY_II;
    }
    public void setDISCIPLINARY_II(String DISCIPLINARY_II) {
        this.DISCIPLINARY_II = DISCIPLINARY_II;
    }
    public String getDISCIPLINARY_III() {
        return DISCIPLINARY_III;
    }
    public void setDISCIPLINARY_III(String DISCIPLINARY_III) {
        this.DISCIPLINARY_III = DISCIPLINARY_III;
    }
    public String getDOB() {
        return DOB;
    }
    public void setDOB(String DOB) {
        this.DOB = DOB;
    }
    public String getCITY_OF_BIRTH() {
        return CITY_OF_BIRTH;
    }
    public void setCITY_OF_BIRTH(String CITY_OF_BIRTH) {
        this.CITY_OF_BIRTH = CITY_OF_BIRTH;
    }
    public String getNATIONALITY() {
        return NATIONALITY;
    }
    public void setNATIONALITY(String NATIONALITY) {
        this.NATIONALITY = NATIONALITY;
    }
    public String getRELIGION() {
        return RELIGION;
    }
    public void setRELIGION(String RELIGION) {
        this.RELIGION = RELIGION;
    }
    public String getBLOOD_GROUP() {
        return BLOOD_GROUP;
    }
    public void setBLOOD_GROUP(String BLOOD_GROUP) {
        this.BLOOD_GROUP = BLOOD_GROUP;
    }
    public String getM_STATUS() {
        return M_STATUS;
    }
    public void setM_STATUS(String M_STATUS) {
        this.M_STATUS = M_STATUS;
    }
    public String getMOTHER_NAME() {
        return MOTHER_NAME;
    }
    public void setMOTHER_NAME(String MOTHER_NAME) {
        this.MOTHER_NAME = MOTHER_NAME;
    }
    public String getDESIG_CODE() {
        return DESIG_CODE;
    }
    public void setDESIG_CODE(String DESIG_CODE) {
        this.DESIG_CODE = DESIG_CODE;
    }
    public String getGRADE() {
        return GRADE;
    }
    public void setGRADE(String GRADE) {
        this.GRADE = GRADE;
    }
    public String getCONFIRMING_ON() {
        return CONFIRMING_ON;
    }
    public void setCONFIRMING_ON(String CONFIRMING_ON) {
        this.CONFIRMING_ON = CONFIRMING_ON;
    }
    public String getSHIFT() {
        return SHIFT;
    }
    public void setSHIFT(String SHIFT) {
        this.SHIFT = SHIFT;
    }
    public String getPROB_PERIOD() {
        return PROB_PERIOD;
    }
    public void setPROB_PERIOD(String PROB_PERIOD) {
        this.PROB_PERIOD = PROB_PERIOD;
    }
    public String getEXP_IN_KTML() {
        return EXP_IN_KTML;
    }
    public void setEXP_IN_KTML(String EXP_IN_KTML) {
        this.EXP_IN_KTML = EXP_IN_KTML;
    }
    public String getAPPLICATION_DATE() {
        return APPLICATION_DATE;
    }
    public void setAPPLICATION_DATE(String APPLICATION_DATE) {
        this.APPLICATION_DATE = APPLICATION_DATE;
    }
    public String getORG_ID() {
        return ORG_ID;
    }
    public void setORG_ID(String ORG_ID) {
        this.ORG_ID = ORG_ID;
    }
    public String getDIVISION() {
        return DIVISION;
    }
    public void setDIVISION(String DIVISION) {
        this.DIVISION = DIVISION;
    }
    public String getBRANCH_CODE() {
        return BRANCH_CODE;
    }
    public void setBRANCH_CODE(String BRANCH_CODE) {
        this.BRANCH_CODE = BRANCH_CODE;
    }
    public String getBRANCH_NAME() {
        return BRANCH_NAME;
    }
    public void setBRANCH_NAME(String BRANCH_NAME) {
        this.BRANCH_NAME = BRANCH_NAME;
    }
    public String getDESCR() {
        return DESCR;
    }
    public void setDESCR(String DESCR) {
        this.DESCR = DESCR;
    }
    public String getREP_UNT() {
        return REP_UNT;
    }
    public void setREP_UNT(String REP_UNT) {
        this.REP_UNT = REP_UNT;
    }
    public String getREP_EMP_ID() {
        return REP_EMP_ID;
    }
    public void setREP_EMP_ID(String REP_EMP_ID) {
        this.REP_EMP_ID = REP_EMP_ID;
    }
    public String getREP_EMP_DESIG_CODE() {
        return REP_EMP_DESIG_CODE;
    }
    public void setREP_EMP_DESIG_CODE(String REP_EMP_DESIG_CODE) {
        this.REP_EMP_DESIG_CODE = REP_EMP_DESIG_CODE;
    }
    public String getREP_EMP_DEPT_CODE() {
        return REP_EMP_DEPT_CODE;
    }
    public void setREP_EMP_DEPT_CODE(String REP_EMP_DEPT_CODE) {
        this.REP_EMP_DEPT_CODE = REP_EMP_DEPT_CODE;
    }
    public String getREP_EMP_TYPE() {
        return REP_EMP_TYPE;
    }
    public void setREP_EMP_TYPE(String REP_EMP_TYPE) {
        this.REP_EMP_TYPE = REP_EMP_TYPE;
    }
    public String getFLAG() {
        return FLAG;
    }
    public void setFLAG(String FLAG) {
        this.FLAG = FLAG;
    }
    public String getCLEARANCE_STATUS() {
        return CLEARANCE_STATUS;
    }
    public void setCLEARANCE_STATUS(String CLEARANCE_STATUS) {
        this.CLEARANCE_STATUS = CLEARANCE_STATUS;
    }
    public String getHOD_CHECK() {
        return HOD_CHECK;
    }
    public void setHOD_CHECK(String HOD_CHECK) {
        this.HOD_CHECK = HOD_CHECK;
    }
    public String getSEC_HEAD_CHK() {
        return SEC_HEAD_CHK;
    }
    public void setSEC_HEAD_CHK(String SEC_HEAD_CHK) {
        this.SEC_HEAD_CHK = SEC_HEAD_CHK;
    }
    public String getNIC_VERIFY() {
        return NIC_VERIFY;
    }
    public void setNIC_VERIFY(String NIC_VERIFY) {
        this.NIC_VERIFY = NIC_VERIFY;
    }
    public String getNIC_VERIFY_DATE() {
        return NIC_VERIFY_DATE;
    }
    public void setNIC_VERIFY_DATE(String NIC_VERIFY_DATE) {
        this.NIC_VERIFY_DATE = NIC_VERIFY_DATE;
    }
    public String getATT_CATEG() {
        return ATT_CATEG;
    }
    public void setATT_CATEG(String ATT_CATEG) {
        this.ATT_CATEG = ATT_CATEG;
    }
    public String getDIS_CERTIFICATE() {
        return DIS_CERTIFICATE;
    }
    public void setDIS_CERTIFICATE(String DIS_CERTIFICATE) {
        this.DIS_CERTIFICATE = DIS_CERTIFICATE;
    }
    public String getWELLNESS_CLUB() {
        return WELLNESS_CLUB;
    }
    public void setWELLNESS_CLUB(String WELLNESS_CLUB) {
        this.WELLNESS_CLUB = WELLNESS_CLUB;
    }
    public String getWELLNESS_CARD_ISSUE() {
        return WELLNESS_CARD_ISSUE;
    }
    public void setWELLNESS_CARD_ISSUE(String WELLNESS_CARD_ISSUE) {
        this.WELLNESS_CARD_ISSUE = WELLNESS_CARD_ISSUE;
    }
    public String getWELLNESS_CARD_NO() {
        return WELLNESS_CARD_NO;
    }
    public void setWELLNESS_CARD_NO(String WELLNESS_CARD_NO) {
        this.WELLNESS_CARD_NO = WELLNESS_CARD_NO;
    }
    public String getWELLNESS_CLUB_VALID_DATE() {
        return WELLNESS_CLUB_VALID_DATE;
    }
    public void setWELLNESS_CLUB_VALID_DATE(String WELLNESS_CLUB_VALID_DATE) {
        this.WELLNESS_CLUB_VALID_DATE = WELLNESS_CLUB_VALID_DATE;
    }
    public String getCURRENT_ADR() {
        return CURRENT_ADR;
    }
    public void setCURRENT_ADR(String CURRENT_ADR) {
        this.CURRENT_ADR = CURRENT_ADR;
    }
    public String getSS_NO() {
        return SS_NO;
    }
    public void setSS_NO(String SS_NO) {
        this.SS_NO = SS_NO;
    }
    public String getEOBI_NO() {
        return EOBI_NO;
    }
    public void setEOBI_NO(String EOBI_NO) {
        this.EOBI_NO = EOBI_NO;
    }
    public String getTAX_NO() {
        return TAX_NO;
    }
    public void setTAX_NO(String TAX_NO) {
        this.TAX_NO = TAX_NO;
    }
    public String getPFUND_DEDUCTION() {
        return PFUND_DEDUCTION;
    }
    public void setPFUND_DEDUCTION(String PFUND_DEDUCTION) {
        this.PFUND_DEDUCTION = PFUND_DEDUCTION;
    }
    public String getPF_INTEREST() {
        return PF_INTEREST;
    }
    public void setPF_INTEREST(String PF_INTEREST) {
        this.PF_INTEREST = PF_INTEREST;
    }
    public String getPFUND_CODE() {
        return PFUND_CODE;
    }
    public void setPFUND_CODE(String PFUND_CODE) {
        this.PFUND_CODE = PFUND_CODE;
    }
    public String getCLIPPER_PFUND_CODE() {
        return CLIPPER_PFUND_CODE;
    }
    public void setCLIPPER_PFUND_CODE(String CLIPPER_PFUND_CODE) {
        this.CLIPPER_PFUND_CODE = CLIPPER_PFUND_CODE;
    }
    public String getEFU() {
        return EFU;
    }
    public void setEFU(String EFU) {
        this.EFU = EFU;
    }
    public String getEFU_NO() {
        return EFU_NO;
    }
    public void setEFU_NO(String EFU_NO) {
        this.EFU_NO = EFU_NO;
    }
    public String getEOBI_STATUS() {
        return EOBI_STATUS;
    }
    public void setEOBI_STATUS(String EOBI_STATUS) {
        this.EOBI_STATUS = EOBI_STATUS;
    }
    public String getPAY_CATEGORY() {
        return PAY_CATEGORY;
    }
    public void setPAY_CATEGORY(String PAY_CATEGORY) {
        this.PAY_CATEGORY = PAY_CATEGORY;
    }
    public String getBASIC() {
        return BASIC;
    }
    public void setBASIC(String BASIC) {
        this.BASIC = BASIC;
    }
    public String getCOLA1() {
        return COLA1;
    }
    public void setCOLA1(String COLA1) {
        this.COLA1 = COLA1;
    }
    public String getCOLA2() {
        return COLA2;
    }
    public void setCOLA2(String COLA2) {
        this.COLA2 = COLA2;
    }
    public String getCOLA3() {
        return COLA3;
    }
    public void setCOLA3(String COLA3) {
        this.COLA3 = COLA3;
    }
    public String getCOLA4() {
        return COLA4;
    }
    public void setCOLA4(String COLA4) {
        this.COLA4 = COLA4;
    }
    public String getCOLA5() {
        return COLA5;
    }
    public void setCOLA5(String COLA5) {
        this.COLA5 = COLA5;
    }
    public String getCOLA6_7() {
        return COLA6_7;
    }
    public void setCOLA6_7(String COLA6_7) {
        this.COLA6_7 = COLA6_7;
    }
    public String getCOLA8() {
        return COLA8;
    }
    public void setCOLA8(String COLA8) {
        this.COLA8 = COLA8;
    }
    public String getCOLA9() {
        return COLA9;
    }
    public void setCOLA9(String COLA9) {
        this.COLA9 = COLA9;
    }
    public String getCOLA10() {
        return COLA10;
    }
    public void setCOLA10(String COLA10) {
        this.COLA10 = COLA10;
    }
    public String getCOLA11() {
        return COLA11;
    }
    public void setCOLA11(String COLA11) {
        this.COLA11 = COLA11;
    }
    public String getPB_SPECIAL1_2() {
        return PB_SPECIAL1_2;
    }
    public void setPB_SPECIAL1_2(String PB_SPECIAL1_2) {
        this.PB_SPECIAL1_2 = PB_SPECIAL1_2;
    }
    public String getPB_SPECIAL3() {
        return PB_SPECIAL3;
    }
    public void setPB_SPECIAL3(String PB_SPECIAL3) {
        this.PB_SPECIAL3 = PB_SPECIAL3;
    }
    public String getPB_SPECIAL4() {
        return PB_SPECIAL4;
    }
    public void setPB_SPECIAL4(String PB_SPECIAL4) {
        this.PB_SPECIAL4 = PB_SPECIAL4;
    }
    public String getSPECIAL() {
        return SPECIAL;
    }
    public void setSPECIAL(String SPECIAL) {
        this.SPECIAL = SPECIAL;
    }
    public String getOTHER1() {
        return OTHER1;
    }
    public void setOTHER1(String OTHER1) {
        this.OTHER1 = OTHER1;
    }
    public String getOTHER2() {
        return OTHER2;
    }
    public void setOTHER2(String OTHER2) {
        this.OTHER2 = OTHER2;
    }
    public String getOTHER3() {
        return OTHER3;
    }
    public void setOTHER3(String OTHER3) {
        this.OTHER3 = OTHER3;
    }
    public String getMEDICAL() {
        return MEDICAL;
    }
    public void setMEDICAL(String MEDICAL) {
        this.MEDICAL = MEDICAL;
    }
    public String getCONVEYANCE() {
        return CONVEYANCE;
    }
    public void setCONVEYANCE(String CONVEYANCE) {
        this.CONVEYANCE = CONVEYANCE;
    }
    public String getUTILITY() {
        return UTILITY;
    }
    public void setUTILITY(String UTILITY) {
        this.UTILITY = UTILITY;
    }
    public String getENTERTAINMENT() {
        return ENTERTAINMENT;
    }
    public void setENTERTAINMENT(String ENTERTAINMENT) {
        this.ENTERTAINMENT = ENTERTAINMENT;
    }
    public String getPAY_GROUP() {
        return PAY_GROUP;
    }
    public void setPAY_GROUP(String PAY_GROUP) {
        this.PAY_GROUP = PAY_GROUP;
    }
    public String getPAY_GROUP_DESC() {
        return PAY_GROUP_DESC;
    }
    public void setPAY_GROUP_DESC(String PAY_GROUP_DESC) {
        this.PAY_GROUP_DESC = PAY_GROUP_DESC;
    }
    public String getPAY_AT_JOINING() {
        return PAY_AT_JOINING;
    }
    public void setPAY_AT_JOINING(String PAY_AT_JOINING) {
        this.PAY_AT_JOINING = PAY_AT_JOINING;
    }
    public String getEXTRA_DUTY() {
        return EXTRA_DUTY;
    }
    public void setEXTRA_DUTY(String EXTRA_DUTY) {
        this.EXTRA_DUTY = EXTRA_DUTY;
    }
    public String getPAYROLL_FLAG() {
        return PAYROLL_FLAG;
    }
    public void setPAYROLL_FLAG(String PAYROLL_FLAG) {
        this.PAYROLL_FLAG = PAYROLL_FLAG;
    }



    public String getFIRST_DOSE() { return FIRST_DOSE; }
public void setFIRST_DOSE(String FIRST_DOSE) { this.FIRST_DOSE = FIRST_DOSE; }

public String getSECOND_DOSE() { return SECOND_DOSE; }
public void setSECOND_DOSE(String SECOND_DOSE) { this.SECOND_DOSE = SECOND_DOSE; }

public String getFIRST_VACC_DATE() { return FIRST_VACC_DATE; }
public void setFIRST_VACC_DATE(String FIRST_VACC_DATE) { this.FIRST_VACC_DATE = FIRST_VACC_DATE; }

public String getSECOND_VACC_DATE() { return SECOND_VACC_DATE; }
public void setSECOND_VACC_DATE(String SECOND_VACC_DATE) { this.SECOND_VACC_DATE = SECOND_VACC_DATE; }
}
