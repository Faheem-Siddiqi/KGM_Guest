-- =========================
-- EMPLOYEES (CORE)
-- =========================
CREATE TABLE employees (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_code VARCHAR(50),
    emp_name VARCHAR(150),
    father_name VARCHAR(150),
    mother_name VARCHAR(150),
    nid VARCHAR(50),
    dob VARCHAR(30),
    gender VARCHAR(20),
    nationality VARCHAR(50),
    religion VARCHAR(50),
    blood_group VARCHAR(20),
    m_status VARCHAR(20),
    dept_code VARCHAR(50),
    department VARCHAR(150),
    desig_code VARCHAR(50),
    designation VARCHAR(150),
    org_id VARCHAR(50),
    division VARCHAR(100),
    joining_date VARCHAR(30),
    emp_status VARCHAR(50),
    shift VARCHAR(50),
    emp_contno VARCHAR(50),
    personal_email VARCHAR(150),
    official_email VARCHAR(150)
);

-- =========================
-- SALARY
-- =========================
CREATE TABLE employee_salary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id BIGINT,
    gross_salary VARCHAR(50),
    pay_sheet VARCHAR(50),
    pay_category VARCHAR(50),
    basic VARCHAR(50),
    cola1 VARCHAR(50),
    cola2 VARCHAR(50),
    cola3 VARCHAR(50),
    cola4 VARCHAR(50),
    cola5 VARCHAR(50),
    cola6_7 VARCHAR(50),
    cola8 VARCHAR(50),
    cola9 VARCHAR(50),
    cola10 VARCHAR(50),
    cola11 VARCHAR(50),
    h_rent VARCHAR(50),
    h_maintenance VARCHAR(50),
    pb_special1_2 VARCHAR(50),
    pb_special3 VARCHAR(50),
    pb_special4 VARCHAR(50),
    special VARCHAR(50),
    other1 VARCHAR(50),
    other2 VARCHAR(50),
    other3 VARCHAR(50),
    medical VARCHAR(50),
    conveyance VARCHAR(50),
    utility VARCHAR(50),
    entertainment VARCHAR(50),
    FOREIGN KEY (employee_id) REFERENCES employees(id)
);

-- =========================
-- PROFILE / HR DATA
-- =========================
CREATE TABLE employee_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id BIGINT,
    city_village VARCHAR(150),
    district VARCHAR(150),
    current_adr VARCHAR(255),
    permanent_adr VARCHAR(255),
    bank_name VARCHAR(150),
    bank_ac_no VARCHAR(50),
    ss_no VARCHAR(50),
    eobi_no VARCHAR(50),
    tax_no VARCHAR(50),
    pfund_code VARCHAR(50),
    pfund_deduction VARCHAR(50),
    efu VARCHAR(50),
    efu_no VARCHAR(50),
    emergency_no VARCHAR(50),
    rest_day VARCHAR(50),
    staff VARCHAR(50),
    reference VARCHAR(150),
    resign_reason VARCHAR(255),
    resign_date VARCHAR(50),
    FOREIGN KEY (employee_id) REFERENCES employees(id)
);

-- =========================
-- IT MODULE
-- =========================
CREATE TABLE employee_it (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id BIGINT,
    it_equipment VARCHAR(50),
    it_email VARCHAR(150),
    it_internet VARCHAR(50),
    internet_justify VARCHAR(255),
    FOREIGN KEY (employee_id) REFERENCES employees(id)
);

-- =========================
-- HEALTH / VACCINATION
-- =========================
CREATE TABLE employee_health (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id BIGINT,
    first_dose VARCHAR(50),
    second_dose VARCHAR(50),
    first_vacc_date VARCHAR(50),
    second_vacc_date VARCHAR(50),
    covid_cert VARCHAR(50),
    FOREIGN KEY (employee_id) REFERENCES employees(id)
);

-- =========================
-- DOCUMENTS
-- =========================
CREATE TABLE employee_documents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id BIGINT,
    document_type VARCHAR(100),
    file_path VARCHAR(255),
    FOREIGN KEY (employee_id) REFERENCES employees(id)
);

-- =========================
-- RAW BACKUP (OPTIONAL)
-- =========================
CREATE TABLE employee_raw (
    employee_id BIGINT PRIMARY KEY,
    data JSON,
    FOREIGN KEY (employee_id) REFERENCES employees(id)
);