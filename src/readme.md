# KGM Guest Source Notes

Source code is organized by application layer:

- `ui` and `ui.panel`: dashboard, add guest, guest details, accommodation screens, and report actions.
- `ui.dialog`: modal dialogs and progress dialogs.
- `ui.styling`: shared Swing styling helpers.
- `service`: Excel import/sample generation, report export, validation, and authentication.
- `dao`, `model`, `database`, `config`, and `resources`: MySQL persistence, schema initialization, and application models.

For clone, setup, build, and run instructions, see the root `README.md`.

Note: in backend naming, beds and seats refer to the same accommodation capacity concept.
