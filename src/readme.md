


# Employee Management System

A Java Swing-based desktop application for managing employee records, onboarding (induction), authentication, and document handling with a layered architecture (DAO + UI separation).

---

## 📌 Project Overview

This system provides a complete employee workflow including:

- Database initialization and connection management
- User authentication and session handling
- Employee CRUD operations
- Employee induction (form + document handling)
- Table-based employee listing
- Detailed employee view screen

---

## 🏗️ Architecture

### 1. Database Layer
- **Database Initialization**
  - Creates required tables on startup
  - Ensures schema consistency

- **DB Connection Manager**
  - Manages database connections

---

### 2. Model Layer
- **Employee Model**
  - Stores employee attributes (id, name, department, etc.)

- **User Model**
  - Stores authenticated user/session details

---

### 3. Authentication & Session Layer
- **Auth Service**
  - Handles login/logout
  - Validates credentials

- **User Session**
  - Maintains logged-in user state

---

### 4. DAO Layer
- **EmployeeDao**
  - Handles basic database operations (insert, save)

- **EmployeeRepositoryDao**
  - Handles:
    - Fetching employee data
    - Updating employee records
    - Search/filter operations

---

### 5. UI Layer

#### 🏠 Home Module
- **HomeView**
  - Main dashboard
  - Loads employee table data

- **EmployeeTablePanel**
  - Displays employee list in table format
  - Supports row selection

---

#### 👤 Employee Detail Module
- **EmployeeDetailView**
  - Opens on employee selection

  **Includes:**
  - FormViewPanel (employee details)
  - DocumentViewPanel (employee documents)

---

#### 🧾 Employee Induction Module
- **EmployeeInduction Panel**
  - Handles onboarding process

  **Components:**
  - Form Panel (data entry)
  - Document Panel (upload/view documents)

---

## 🔄 Application Flow

1. Application starts
2. Database initializes
3. User logs in via Auth Service
4. HomeView loads employee data
5. Employee list displayed in table
6. On row click → EmployeeDetailView opens
7. User can view/edit details and documents
8. Induction handled via separate module
9. Employ induction leads to uplof from files FormPanel and DocumentPanel

---

## 📁 Suggested Package Structure
install Maven
• Java Swing→ simple desktop UI 
• Folder storage → for images 
 SQLite  → for data

 Java 21 (JDK): 
 https://download.oracle.com/java/21/archive/jdk-21.0.10_windows-x64_bin.exe (sha256)


 https://downloads.apache.org/maven/maven-3/3.9.14/binaries/
 Set Environment Variable via youtube 
 apache-maven-3.9.14-bin.zip
 mvn clean install
