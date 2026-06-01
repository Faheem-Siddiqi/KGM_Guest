# KGM Guest Accommodation

Java 21 Swing desktop application for guest registration, room allotment, accommodation tracking, Excel import, and PDF/Excel guest reports.

## Requirements

Install these tools before running or packaging the app:

| Requirement | Version | Notes |
| --- | --- | --- |
| Git | 2.x or newer | Required to clone the project and used by `update-exe.ps1` for `git pull`. |
| JDK | 21 | Required. The Maven compiler uses Java release `21`, and `jpackage` comes from this JDK. |
| Maven | 3.9 or newer | Required. Used to download dependencies, compile, test, and create the shaded jar. |
| MySQL Server | 8.0 or newer | The app creates and migrates the `kgm_guest` schema on startup. |
| PowerShell | Windows PowerShell 5.1 or newer | Required for `build-exe.ps1` and `update-exe.ps1`. |
| WiX Toolset | 3.x or 4.x | Optional for the current app-image flow. The scripts detect it and warn if missing; it is only needed for MSI installer packaging. |
| Desktop OS | Windows 10/11 for exe packaging; macOS/Linux can run from source | Required because this is a Swing UI application. |

Useful version commands:

```powershell
git --version
java --version
mvn --version
jpackage --version
candle -?      # WiX 3.x, if installed
wix --version  # WiX 4.x, if installed
```

Maven downloads these libraries from `pom.xml`; do not copy jars manually:

| Library | Version | Purpose |
| --- | --- | --- |
| `org.apache.poi:poi-ooxml` | `5.2.5` | Read/write Excel `.xlsx`/`.xls` workbooks. |
| `org.apache.logging.log4j:log4j-core` | `2.21.1` | Runtime logging used by POI dependencies. |
| `org.slf4j:slf4j-simple` | `2.0.13` | Simple logging for bulk/import workflows. |
| `com.mysql:mysql-connector-j` | `9.3.0` | MySQL JDBC driver. |
| `com.toedter:jcalendar` | `1.4` | Date/time picker dependency. |
| `org.junit.jupiter:junit-jupiter` | `5.10.2` | Unit tests only. |

Build plugins:

| Plugin | Version | Purpose |
| --- | --- | --- |
| `maven-compiler-plugin` | `3.13.0` | Compiles with Java release `21`. |
| `maven-surefire-plugin` | `3.2.5` | Runs JUnit tests. |
| `maven-shade-plugin` | `3.5.3` | Builds the runnable jar used by the Windows app package. |

## First-Time Setup

1. Install required tools.

At minimum for Windows app packaging, install Git, JDK 21, Maven 3.9+, MySQL 8+, and PowerShell 5.1+. WiX is optional unless you later decide to create an MSI installer.

2. Clone the project and enter the repository.

```powershell
git clone <repository-url>
cd KGM_Guest
```

3. Install and start MySQL Server.

4. Create a MySQL user. The app can create the database automatically if the user has database creation permission.

```sql
CREATE USER IF NOT EXISTS 'kgm_user'@'localhost' IDENTIFIED BY 'change_me';
GRANT CREATE ON *.* TO 'kgm_user'@'localhost';
GRANT ALL PRIVILEGES ON `kgm_guest`.* TO 'kgm_user'@'localhost';
FLUSH PRIVILEGES;
```

If you prefer not to grant `CREATE ON *.*`, create the database yourself and only grant access to it:

```sql
CREATE DATABASE IF NOT EXISTS `kgm_guest` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON `kgm_guest`.* TO 'kgm_user'@'localhost';
FLUSH PRIVILEGES;
```

5. Configure the database connection and app login.

Copy the sample env file and edit values for that PC:

```powershell
Copy-Item .env.sample .env
```

Then open `.env` and put the local MySQL and login values in:

```text
KGM_DB_HOST=127.0.0.1
KGM_DB_PORT=3306
KGM_DB_NAME=kgm_guest
KGM_DB_USER=kgm_user
KGM_DB_PASSWORD=change_me

KGM_LOGIN_USERNAME=admin
KGM_LOGIN_PASSWORD=change_this_password
```

`.env` is ignored by git, so real local credentials do not get committed. `.env.sample` is safe to commit and should contain placeholders only.

When using `build-exe.ps1`, the script copies this root `.env` into the app output as `config\.env`.

You can also use environment variables:

```powershell
$env:KGM_DB_HOST="127.0.0.1"
$env:KGM_DB_PORT="3306"
$env:KGM_DB_NAME="kgm_guest"
$env:KGM_DB_USER="kgm_user"
$env:KGM_DB_PASSWORD="change_me"
```

You can also pass JVM properties instead:

```powershell
-Dkgm.db.host=127.0.0.1 -Dkgm.db.port=3306 -Dkgm.db.name=kgm_guest -Dkgm.db.user=kgm_user -Dkgm.db.password=change_me
```

Priority order is JVM properties, OS environment variables, `.env` / `config\.env`, then safe defaults from code.

6. Compile and run tests.

```powershell
mvn test
```

7. Build the Windows app package.

Run this from the project root:

```powershell
.\build-exe.ps1 -OutputDir "D:\KGM-App"
```

This creates:

```text
D:\KGM-App\
├─ KGM.exe
├─ app\
├─ runtime\
├─ images\
├─ employees\
├─ config\.env
├─ logs\
└─ backups\
```

8. Run the packaged app.

```powershell
D:\KGM-App\KGM.exe
```

9. Update an existing packaged app after code changes are pushed to Git.

Close `D:\KGM-App\KGM.exe` first, then run:

```powershell
.\update-exe.ps1 -OutputDir "D:\KGM-App"
```

The update command runs `git pull`, rebuilds the jar, creates a new app image, backs up old generated files, and replaces only `KGM.exe`, `app\`, and `runtime\`. It keeps `config\.env`, `images\`, `employees\`, `logs\`, and `backups\` untouched.

10. Run the application from source during development.

From an IDE, run:

```text
com.kgm.Main
```

From PowerShell:

```powershell
mvn -q -DskipTests compile dependency:copy-dependencies
java -cp "target/classes;target/dependency/*" com.kgm.Main
```

On macOS/Linux, use `:` instead of `;` in the Java classpath:

```bash
java -cp "target/classes:target/dependency/*" com.kgm.Main
```

On first startup, `DatabaseInitializer.init()` creates the database if needed, applies `src/main/resources/db/schema.sql`, runs migrations, creates performance indexes, and seeds default categories/users.

## Login

Default login, unless overridden in `.env`:

```text
Username: admin
Password: 1234
```

The schema has a `users` table, but `src/main/java/com/kgm/service/AuthService.java` currently authenticates against `KGM_LOGIN_USERNAME` and `KGM_LOGIN_PASSWORD` from env configuration.

## Daily Run Commands

```powershell
mvn test
mvn -q -DskipTests compile dependency:copy-dependencies
java -cp "target/classes;target/dependency/*" com.kgm.Main
```

## Windows App Package Commands

Create the app package:

```powershell
.\build-exe.ps1 -OutputDir "D:\KGM-App"
```

Optional clean build:

```powershell
.\build-exe.ps1 -OutputDir "D:\KGM-App" -CleanTarget
```

Update an existing app package:

```powershell
.\update-exe.ps1 -OutputDir "D:\KGM-App"
```

Optional clean update:

```powershell
.\update-exe.ps1 -OutputDir "D:\KGM-App" -CleanTarget
```

Best rule: update app files only, never user data folders.

Both scripts show live progress with `Write-Progress` and percentage messages in the terminal.

Dynamic tool check behavior:

- `build-exe.ps1` and `update-exe.ps1` use a data-driven `$requiredTools` list inside `Check-RequiredTools`.
- Each required command is checked with `Get-Command`, so the scripts find tools from the user's `PATH` instead of hardcoded install folders.
- To require another tool in the future, add one entry to `$requiredTools`; the same check loop will include it automatically for first-time build and update flows.
- WiX is checked separately because it is optional for app-image builds but useful for future installer work.

Build/update behavior:

- Checks Git, Maven, Java, `jpackage`, and WiX presence.
- Requires Java 21 or newer.
- Checks `.env`.
- Checks MySQL with a 3000ms TCP timeout and continues with a warning if MySQL is not reachable.
- Skips `target\` cleanup by default to avoid terminating the VS Code PowerShell terminal. Use `-CleanTarget` only when a clean target folder is specifically needed.
- Runs `mvn package` with a 15 minute timeout.
- Copies only `target\my-java-app-1.0.0.jar` into a fresh temporary `jpackage` input folder; it never packages from `target\classes`.
- Runs `jpackage` with a 10 minute timeout to create `KGM.exe`.
- Runs `git pull` during update with a 3 minute timeout.
- If a command fails or times out, the script explains the likely reason, such as locked files, Maven dependency download failure, Git authentication, or jpackage/JDK packaging issues.
- Backs up old `KGM.exe`, `app\`, and `runtime\`.
- Replaces only generated app files.
- Never deletes `config\.env`, `employees\`, `images\uploads\`, `logs\`, or `backups\`.

Script helper functions:

| Function | Purpose |
| --- | --- |
| `Write-Step` | Shows the current step with `Write-Progress` and a terminal percentage. |
| `Complete-Progress` | Clears the PowerShell progress bar at the end or on failure. |
| `Resolve-ProjectRoot` | Finds the folder where the script is located. |
| `Require-Command` | Checks that a required command exists on `PATH`. |
| `Check-RequiredTools` | Checks Git, Maven, Java, and `jpackage`. |
| `Check-Wix` | Checks WiX tools and warns if missing. |
| `Require-Java21` | Confirms the installed Java version is 21 or newer. |
| `Get-EnvFileValue` | Reads a value such as `KGM_DB_HOST` or `KGM_DB_PORT` from `.env`. |
| `Test-MySqlPort` | Checks the MySQL port with `TcpClient` and a 3000ms timeout. |
| `Check-MySqlReachable` | Reads DB host/port from `.env` and calls `Test-MySqlPort`. |
| `ConvertTo-ArgumentString` | Quotes command arguments safely for external tools. |
| `Invoke-ExternalCommand` | Runs external tools with live output and a timeout so long steps do not hang forever. |
| `Get-TerminationReason` | Explains why a process failed or was terminated, based on timeout, exit code, and captured output. |
| `Clear-TargetFolder` | Tries to remove `target\` in a short background job and continues if it is locked. It does not stop VS Code, Java, or app processes. |
| `Invoke-WithRetry` | Retries file/folder replacement when Windows temporarily locks generated app files. |
| `Copy-FileWithRetry` | Replaces `KGM.exe` with retry and a clear close-the-app message if locked. |
| `Copy-DirectoryFresh` | Replaces generated folders such as `app\` and `runtime\`. |
| `Copy-DirectoryIfMissing` | Copies data folders only when missing, so user files are preserved. |
| `Backup-AppFiles` | Backs up old `KGM.exe`, `app\`, and `runtime\` before replacement. |

## Main Workflows

- Dashboard: view KPI cards, occupancy charts, department chart, guest list, filters, Excel services, and report export.
- Add Guest: enter guest, request, approval, stay, accommodation, room, and remarks.
- Accommodations: create categories, create/update rooms, manage status/capacity/staff/amenities.
- Guest Details: view guest details, update departure date when allowed, and add remarks when allowed.
- Excel Services: download sample workbook or import completed workbook as New/Standard or Legacy/Historical data.
- Reports: export guest reports as PDF, Excel, or both for a selected period.

## Implemented Rules

### Frontend Rules

- Add Guest initializes the database before loading guest form values.
- Guest nationality defaults to `Pakistani`.
- CNIC / Passport label is dynamic:
  - `Guest CNIC` for nationality `Pakistan` or `Pakistani`, case-insensitive.
  - `Guest Passport` for every other nationality.
- Pakistani CNIC input is formatted on screen as `#####-#######-#`; dashes are only frontend display and are not stored.
- Pakistani/Pakistan CNIC must be exactly 13 digits after removing dashes.
- Non-Pakistani passport must be 4 to 30 letters/digits, include at least one letter, and may only use spaces or hyphens as separators.
- Add Guest required values include guest name, CNIC/passport, nationality, guest category, company name, visit type, guest address, requested by, requested department, approved by, accommodated by, arrival, departure, accommodation category, and room.
- Visit Type is limited to `Official Visit` or `Personal Visit`.
- Requested Department values shown in the UI are `HR`, `Admin`, `Finance`, `Spinning`, `Power House`, `IT`, `Security`, and `Others (speficy)`.
- Add Guest hides the `Security Block` accommodation category from the normal accommodation list.
- Room dropdown only lists rooms ready for assignment. If no ready rooms exist it shows disabled helper values such as `All rooms occupied` or `No rooms available`.
- Arrival and departure pickers update tenure live.
- Departure must be after arrival.
- Remarks default to `N/A` when blank.
- Guest Details shows the same dynamic identifier label rule as Add Guest.
- Guest Details locks arrival and most guest fields.
- Departed guest records cannot have departure changed. Remarks can only be edited when the original remarks are blank or `N/A`.
- Guest status is calculated as `Upcoming`, `Currently Staying`, `Departed`, or `Invalid Dates`.
- Excel Services disables the button while importing or downloading the sample and shows progress dialogs for long work.
- Sample download shows success on completion and an error if the file cannot be saved.
- Excel import only accepts `.xlsx` or `.xls`; temporary Excel lock files beginning with `~$` are rejected.

### Backend And Database Rules

- The app uses MySQL through JDBC and stores data in `kgm_guest` by default.
- `DatabaseInitializer` creates the database, applies schema, adds missing legacy columns, adds indexes, and removes unused seed accommodation data.
- Active room names must start with `Room-` or equal `Rear Wing`.
- Room names entered without `Room-` are normalized to `Room-<value>`.
- Accommodation capacity cannot be negative.
- Only `Rear Wing` can have capacity `0`.
- Accommodation uniqueness is by category plus room name.
- Accommodation categories cannot be deleted while accommodations still reference them.
- Guest categories are found or created when saving guests.
- Standard guest save rejects missing arrival/departure and rejects departure before arrival.
- Standard guest save rejects overlapping stays for the same normalized CNIC / Passport.
- CNIC / Passport matching for overlap checks ignores spaces, hyphens, and letter case.
- Overlap means existing `arrival_at < new departure` and existing `departure_at > new arrival`.
- The overlap block message is: `This guest already has a booking or stay during the selected period. One guest can only be allotted one room at a time`.
- Standard guest save requires the selected room to be active and `Ready for Assignment`.
- Standard guest save checks room capacity for the selected date range.
- Future bookings do not mark the room `Reserved`; current/past bookings refresh room status.
- A room becomes `Reserved` only when current occupancy reaches capacity.
- A `Reserved` room can be released back to `Ready for Assignment` when no current guests remain.
- Updating a guest departure date also checks date order and same-identifier overlap, excluding the current guest record.
- Deleting an upcoming guest removes the booking. Deleting current/past guests also attempts to release the room if appropriate.

### Excel Import Rules

Excel import has two modes selected by the user before import.

| Mode | Purpose | Main behavior |
| --- | --- | --- |
| Import New / Standard Data | New bookings and normal guest entry | Applies Add Guest validation, CNIC / Passport checks, overlap checks, room status checks, and capacity checks. |
| Import Legacy / Historical Data | Old records where data may be incomplete or duplicated | Allows blank/repeated old CNIC / Passport values and bypasses overlap, capacity, and room-status checks, but still requires valid dates, company, visit type, accommodation category, and room. |

Workbook and header rules:

- Import reads the first worksheet only.
- Row 1 must contain headers; guest rows start on Row 2.
- Blank rows are ignored.
- If no guest rows exist, import stops with a no-rows message.
- Bad rows are skipped with a row reason; other valid rows continue importing.
- Accepted header aliases include old forms like `CNIC`, `CNIC/Passport`, `Guest CNIC`, `Passport`, and `Guest Passport`.
- Standard import required headers: `Guest Name`, `CNIC / Passport`, `Nationality`, `Guest Category`, `Company Name`, `Visit Type`, `Address`, `Requested By`, `Requested Department`, `Approved By`, `Accommodated By`, `Arrival Date Time`, `Departure Date Time`, `Accommodation Category`, and `Room`.
- Legacy import required headers: `Company Name`, `Visit Type`, `Arrival Date Time`, `Departure Date Time`, `Accommodation Category`, and `Room`.

CNIC / Passport rules:

- Standard import uses nationality to choose the rule:
  - `Pakistan` or `Pakistani`: CNIC must be exactly 13 digits with no dashes in Excel.
  - Any other nationality: passport must be 4 to 30 letters/digits, include at least one letter, and use only spaces or hyphens as separators.
- Legacy import allows blank CNIC / Passport.
- Legacy import allows repeated old placeholders such as `9999999999999`.
- If a legacy row provides CNIC / Passport, the value must still be either a 13-digit CNIC or a valid passport format.
- Invalid examples include CNIC with dashes, passport with slash/underscore, repeated separators such as `AB--12345`, and passport values containing only numbers.
- Standard import stores compact identifiers after validation.

Date and visit rules:

- Arrival and Departure are required in both modes.
- Departure must be after Arrival.
- Native Excel date cells are accepted.
- Accepted text date/time formats include `yyyy-MM-dd HH:mm`, `yyyy-MM-dd H:mm`, `yyyy/MM/dd HH:mm`, `yyyy/MM/dd H:mm`, `dd-MM-yyyy HH:mm`, `dd-MM-yyyy H:mm`, `dd/MM/yyyy HH:mm`, `dd/MM/yyyy H:mm`, `M/d/yyyy HH:mm`, `M/d/yyyy H:mm`, `M/d/yy HH:mm`, and `M/d/yy H:mm`.
- Accepted date-only formats include `yyyy-MM-dd`, `yyyy/MM/dd`, `dd-MM-yyyy`, `dd/MM/yyyy`, `M/d/yyyy`, and `M/d/yy`.
- Date-only values are imported at midnight.
- Visit Type must resolve to `Official Visit` or `Personal Visit`.

Accommodation and room rules:

- Accommodation Category is resolved against the database.
- Room names are normalized with the `Room-` prefix when possible.
- Standard import requires the room to exist, be active, be `Ready for Assignment`, and have capacity for the selected dates.
- Standard import blocks a row when the same CNIC / Passport already has an overlapping booking or stay.
- Legacy import requires the room to exist in the database but does not require ready status and does not check capacity.
- Legacy import bypasses same-identifier overlap checks.
- Legacy blank optional guest fields are stored as `N/A`.
- Legacy remarks include `Old record - validation bypassed`.

Duplicate rules:

- Standard import does not use the old name/date duplicate rule; it uses the live Add Guest rules, especially CNIC / Passport overlap and room capacity.
- Legacy duplicate check against existing DB records uses exactly: `Guest Name + exact Arrival Date Time + exact Departure Date Time + Guest Category`.
- Legacy duplicate check does not require CNIC / Passport to be unique.
- Within the same legacy workbook, an exact duplicate row is skipped when all guest, request, stay, room, CNIC / Passport, and remarks values match a row already imported from that workbook.

Sample workbook rules:

- Download Sample creates two sheets: `Guest Import` and `Valid Values`.
- `Guest Import` contains the import headers and sample rows.
- `Valid Values` documents standard and legacy rules, CNIC/passport examples, date formats, departments, and live DB values.
- Sample rows include Pakistani CNIC examples and at least two non-Pakistani passport examples.
- The sample uses current DB accommodation categories, rooms, room status, capacity, available beds, and guest categories.

## Reports

- Reports can be generated as PDF, Excel, or both.
- Selecting both creates a report folder using the report period label.
- Excel and PDF report generators are separate files.
- Report guest tables use `CNIC / Passport` as the identifier column label.

## Source File Map

### Entry, Config, Database, Models

| File | Main function |
| --- | --- |
| `src/main/java/com/kgm/Main.java` | Application entry point; initializes DB and opens `LoginView`. |
| `src/main/java/com/kgm/config/DatabaseConfig.java` | Reads DB host, port, name, user, and password from JVM properties/env/defaults. |
| `src/main/java/com/kgm/config/DatabaseConnection.java` | Opens server/database JDBC connections and loads MySQL driver. |
| `src/main/java/com/kgm/config/EnvironmentConfig.java` | Shared config loader for JVM properties, OS environment variables, root `.env`, and packaged `config\.env`. |
| `.env.sample` | Safe copy template for local ignored `.env` database and login values. |
| `src/main/java/com/kgm/database/DatabaseInitializer.java` | Creates DB/schema, migrations, indexes, constraints, and cleanup. |
| `src/main/resources/db/schema.sql` | MySQL schema for users, categories, accommodations, amenities, and guests. |
| `src/main/java/com/kgm/model/Guest.java` | Guest data model used by UI, DAO, import, and reports. |
| `src/main/java/com/kgm/model/UserSession.java` | Session value object with login and expiry time. |
| `src/main/java/com/kgm/controller/UserSessions.java` | Placeholder/legacy session controller. |

### DAO Layer

| File | Main function |
| --- | --- |
| `src/main/java/com/kgm/dao/GuestDao.java` | Saves, updates, deletes, queries guests; enforces overlap, room, and capacity rules. |
| `src/main/java/com/kgm/dao/AccommodationDao.java` | Reads/saves rooms, validates room names/capacity, computes available seats and KPI drilldowns. |
| `src/main/java/com/kgm/dao/AccommodationCategoryDao.java` | Creates, updates, deletes, and lists accommodation categories. |
| `src/main/java/com/kgm/dao/DashboardDao.java` | Loads dashboard KPI, occupancy chart, department chart, and category KPI data. |

### Service Layer

| File | Main function |
| --- | --- |
| `src/main/java/com/kgm/service/AuthService.java` | Temporary hardcoded login check for `admin` / `1234`. |
| `src/main/java/com/kgm/services/AuthService.java` | Empty legacy duplicate class; not used by the current app flow. |
| `src/main/java/com/kgm/service/GuestValidationService.java` | Shared standard/legacy guest validation and row/dialog messages. |
| `src/main/java/com/kgm/service/GuestIdentifierRules.java` | CNIC/passport format and normalization helpers. |
| `src/main/java/com/kgm/service/ExcelImportService.java` | Parses Excel files and imports rows as standard or legacy guests. |
| `src/main/java/com/kgm/service/ExcelSampleGenerator.java` | Generates the two-sheet Excel sample workbook and import guide. |
| `src/main/java/com/kgm/service/GuestReportService.java` | Coordinates report range data and PDF/Excel export requests. |
| `src/main/java/com/kgm/service/GuestPdfReportGenerator.java` | Builds the PDF guest report. |
| `src/main/java/com/kgm/service/GuestExcelReportGenerator.java` | Builds the Excel guest report with summaries/charts/tables. |

### UI Screens

| File | Main function |
| --- | --- |
| `src/main/java/com/kgm/ui/LoginView.java` | Login screen and session start. |
| `src/main/java/com/kgm/ui/HomeView.java` | Main dashboard, tabs, Excel services, guest records, details navigation, reports. |
| `src/main/java/com/kgm/ui/AddGuest.java` | Add Guest form, dynamic CNIC/passport label, frontend validation, save action. |
| `src/main/java/com/kgm/ui/AccommodationManagement.java` | Standalone accommodation management frame. |

### UI Components And Dialogs

| File | Main function |
| --- | --- |
| `src/main/java/com/kgm/ui/component/UniversalDatePicker.java` | Reusable date/time picker. |
| `src/main/java/com/kgm/ui/component/UniversalDateRangePicker.java` | Reusable date range picker for filters/reports. |
| `src/main/java/com/kgm/ui/component/UploadCard.java` | Reusable upload/drop-style card component. |
| `src/main/java/com/kgm/ui/dialog/UniversalDialog.java` | Common message/option dialog helper UI. |
| `src/main/java/com/kgm/ui/dialog/DelayedProgressDialog.java` | Generic delayed loader for slow tasks. |
| `src/main/java/com/kgm/ui/dialog/ImportProgressDialog.java` | Progress dialog for Excel import. |
| `src/main/java/com/kgm/ui/dialog/ReportPeriodDialog.java` | Report period/format selection dialog. |
| `src/main/java/com/kgm/ui/dialog/ReportProgressDialog.java` | Progress dialog for report generation. |
| `src/main/java/com/kgm/ui/util/FileDialogHandler.java` | Native open/save dialog wrapper with file type helpers. |

### UI Panels

| File | Main function |
| --- | --- |
| `src/main/java/com/kgm/ui/panel/HeaderPanel.java` | Shared top header. |
| `src/main/java/com/kgm/ui/panel/FooterPanel.java` | Shared footer. |
| `src/main/java/com/kgm/ui/panel/HomeKpiPanel.java` | Dashboard KPI container. |
| `src/main/java/com/kgm/ui/panel/KPICategoryPanel.java` | Per-category room/bed KPI cards and drilldown selections. |
| `src/main/java/com/kgm/ui/panel/HouseOccupancyGraphPanel.java` | Occupancy graph with category tabs. |
| `src/main/java/com/kgm/ui/panel/DepartmentAnalysisGraphPanel.java` | Department guest request graph. |
| `src/main/java/com/kgm/ui/panel/UniversalGraphPanel.java` | Reusable bar graph base panel. |
| `src/main/java/com/kgm/ui/panel/GuestFilterPanel.java` | Guest table search/status/date filters. |
| `src/main/java/com/kgm/ui/panel/GuestRecordPanel.java` | Guest list table, refresh, search, and record mapping. |
| `src/main/java/com/kgm/ui/panel/GuestDetailsPanel.java` | Guest detail view and departure/remarks update. |
| `src/main/java/com/kgm/ui/panel/AccommodationManagementPanel.java` | Accommodation page combining category, form, and list panels. |
| `src/main/java/com/kgm/ui/panel/AccommodationCategoryPanel.java` | Accommodation category CRUD. |
| `src/main/java/com/kgm/ui/panel/AccommodationFormPanel.java` | Room form validation and amenity entry. |
| `src/main/java/com/kgm/ui/panel/AccommodationTablePanel.java` | Accommodation table, category tabs, and edit actions. |
| `src/main/java/com/kgm/ui/panel/AccommodationTableModel.java` | Table model for accommodations. |
| `src/main/java/com/kgm/ui/panel/AccommodationRecord.java` | Accommodation view/model object. |
| `src/main/java/com/kgm/ui/panel/AccommodationListViewPanel.java` | KPI drilldown list of accommodations. |
| `src/main/java/com/kgm/ui/panel/RoomDetailPagePanel.java` | Room detail page wrapper. |
| `src/main/java/com/kgm/ui/panel/RoomDetailKpiPanel.java` | Room-level KPI cards. |
| `src/main/java/com/kgm/ui/panel/RoomDetailGuestActivityPanel.java` | Guest activity table for a room. |
| `src/main/java/com/kgm/ui/panel/UniversalTablePanel.java` | Reusable styled table with actions and pagination options. |
| `src/main/java/com/kgm/ui/panel/UpdateAccomodation.java` | Legacy/unused accommodation update placeholder. |

### Styling Helpers

| File | Main function |
| --- | --- |
| `src/main/java/com/kgm/ui/styling/AddGuestHelper.java` | Shared Add Guest/card/form/button styles. |
| `src/main/java/com/kgm/ui/styling/AccommodationManagementHelper.java` | Shared accommodation management styles. |
| `src/main/java/com/kgm/ui/styling/HomeViewHelper.java` | Dashboard colors, tabs, cards, and layout helpers. |
| `src/main/java/com/kgm/ui/styling/RoomDetailHelper.java` | Room detail page style helpers. |
| `src/main/java/com/kgm/ui/styling/DialogHelper.java` | Success, warning, error, sectioned dialog helpers. |

### Session Utilities

| File | Main function |
| --- | --- |
| `src/main/java/com/kgm/util/SessionManager.java` | Starts, checks, and clears the 30-minute session. |
| `src/main/java/com/kgm/util/SessionWatcher.java` | Watches session expiry and returns users to login. |

### Tests

| File | Main function |
| --- | --- |
| `src/test/java/com/kgm/service/GuestIdentifierRulesTest.java` | Tests CNIC/passport validation rules. |
| `src/test/java/com/kgm/service/ExcelImportServiceTest.java` | Tests Excel import behavior. |
| `src/test/java/com/kgm/service/ExcelSampleGeneratorTest.java` | Tests sample workbook headers, examples, and guide sheet. |

## Notes

- In code and reports, beds and seats refer to the same accommodation capacity concept.
- `Security Block` is hidden from normal dashboard/add-guest accommodation flows.
- Keep generated files such as `target/`, report PDFs, and local uploads out of commits unless intentionally needed.
