# KGM Guest and Accommodation Project

Java Swing desktop application for guest records, guest details, and accommodation management.

## Current Structure

- `ui` and `ui.panel`: guest dashboard, add guest, details, and accommodation screens.
- `ui.styling`: shared Swing styling helpers.
- `service` and `util`: authentication, session, validation, filtering, and file helpers.
- `dao`, `model`, `database`, `config`, and `resources`: kept for future guest/accommodation persistence work.

## Database Status

The old employee MySQL/SQLite database layer has been removed. Add new guest and accommodation DAO, model, schema, and connection files in the preserved folders when the new data layer is ready.


In bakcend for Beds , Seats are used 