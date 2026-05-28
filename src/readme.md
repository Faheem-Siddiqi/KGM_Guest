# Source Quick Reference

The full setup, dependency versions, first-run steps, business rules, Excel import rules, and complete file map are documented in the root `README.md`.

Use this file as the short source-directory guide while scanning code.

## Main Flow

- `main/java/com/kgm/Main.java` starts the app, initializes the database, and opens login.
- `main/java/com/kgm/config/EnvironmentConfig.java` reads JVM properties, OS environment variables, and the root `.env` file.
- `main/java/com/kgm/database/DatabaseInitializer.java` creates/migrates the MySQL database from `main/resources/db/schema.sql`.
- `main/java/com/kgm/ui/LoginView.java` handles login.
- `main/java/com/kgm/ui/HomeView.java` owns the dashboard, tabs, Excel import/sample download, guest list, guest details navigation, and report download.
- `main/java/com/kgm/ui/AddGuest.java` owns the Add Guest form and dynamic CNIC/passport frontend behavior.
- `main/java/com/kgm/ui/panel/GuestDetailsPanel.java` owns guest detail viewing and departure/remarks update behavior.
- `main/java/com/kgm/service/ExcelImportService.java` imports guest Excel files.
- `main/java/com/kgm/service/ExcelSampleGenerator.java` generates the two-sheet sample workbook.
- `main/java/com/kgm/dao/GuestDao.java` enforces backend guest save/update rules.

## Key Rules To Remember

- Pakistan/Pakistani nationality shows `Guest CNIC`; all other nationalities show `Guest Passport`.
- Add Guest may display CNIC with dashes, but the database stores the compact value.
- CNIC must be exactly 13 digits.
- Passport must be 4 to 30 letters/digits, include at least one letter, and only use spaces or hyphens as separators.
- Standard Add Guest and Standard Excel import block overlapping stays for the same normalized CNIC / Passport.
- Legacy Excel import allows blank/repeated old CNIC / Passport values, but provided values must still be valid CNIC/passport format.
- Legacy duplicate check against existing DB records is `Guest Name + exact Arrival Date Time + exact Departure Date Time + Guest Category`.
- Legacy duplicate check does not require CNIC / Passport uniqueness.
- Standard import requires room ready status and available capacity.
- Legacy import requires the room to exist but bypasses ready-status, capacity, and overlap checks.

## Source Layout

- `config`: database configuration and JDBC connection helpers.
- `database`: schema initialization and migrations.
- `dao`: database reads/writes plus backend rule enforcement.
- `model`: simple data objects.
- `service`: validation, Excel import/sample generation, reports, and auth.
- `ui`: top-level Swing windows.
- `ui.component`: reusable UI widgets.
- `ui.dialog`: reusable progress and option dialogs.
- `ui.panel`: dashboard, guest, accommodation, and room panels.
- `ui.styling`: shared Swing styling helpers.
- `ui.util`: native file dialog helpers.
- `util`: session tracking.
- `test`: JUnit tests for import/sample/identifier behavior.

Note: in backend naming, beds and seats refer to the same accommodation capacity concept.
