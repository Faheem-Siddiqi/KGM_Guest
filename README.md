# KGM Guest Accommodation

Java Swing desktop application for guest registration, accommodation tracking, Excel import, and guest report exports.

## Requirements

- JDK 21
- Maven 3.9 or newer
- MySQL Server 8.0 or newer, running locally or on a reachable server
- Windows 10/11, macOS, or Linux with a desktop session for the Swing UI

Project dependency versions are managed by `pom.xml`:

- Apache POI `5.2.5` for Excel import/export
- MySQL Connector/J `9.3.0`
- JCalendar `1.4`
- JUnit Jupiter `5.10.2` for tests

## Get The Code

```powershell
git clone <repository-url>
cd KGM_Guest
```

## Configure Database

The app creates and migrates its MySQL schema on startup. Configure the database with environment variables, or let the app use the defaults in `DatabaseConfig`.

```powershell
$env:KGM_DB_HOST="127.0.0.1"
$env:KGM_DB_PORT="3306"
$env:KGM_DB_NAME="kgm_guest"
$env:KGM_DB_USER="your_mysql_user"
$env:KGM_DB_PASSWORD="your_mysql_password"
```

## Build And Test

```powershell
mvn test
```

## Run The App

From an IDE, run:

```text
com.kgm.Main
```

From PowerShell:

```powershell
mvn -q -DskipTests compile dependency:copy-dependencies
java -cp "target/classes;target/dependency/*" com.kgm.Main
```

On macOS or Linux, replace `;` with `:` in the Java classpath.

## Login

Default login:

```text
Username: admin
Password: 1234
```

## Main Workflows

- Add guests from the Add Guest screen.
- Manage accommodation categories and rooms from Accommodation Management.
- Use Excel Services on the Home dashboard to download the sample workbook or import guest Excel data.
- Use Download Report to select a period and format. The app then opens the native save dialog. Single-format exports save directly as PDF or Excel; selecting both creates a report folder such as `Monthly Guest Report KGM`.

## Notes

- Excel imports require `Company Name` and `Visit Type`.
- `Visit Type` must be `Official Visit` or `Personal Visit`.
- In code and reports, beds and seats refer to the same room capacity concept.
