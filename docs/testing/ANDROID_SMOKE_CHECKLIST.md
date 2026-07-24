# SASE Light — Android smoke test

## Scope

This lane validates a minimum Android path without changing application behavior. It does not automate credentials, capture raw UI XML, store recordings, or commit device identifiers.

## Preconditions

- Windows terminal at the repository root.
- JDK 21 active in `PATH`.
- Android SDK platform tools (`adb`) active in `PATH`.
- Exactly one authorized Android device connected.
- Test or mock data only. Never use real student information.

## Deterministic technical run

```bat
tools\android\sase-smoke.bat
```

The script must:

- verify JDK 21;
- verify one authorized device;
- run `gradlew.bat :composeApp:assembleDebug --no-daemon`;
- install the expected debug APK with `adb install -r`;
- launch `com.aistudio.labvirtual.kvmpx/.MainActivity`.

A technical PASS means only that the app built, installed and opened. It does not replace the manual interaction checks below.

## Minimum manual smoke path

Record `PASS`, `FAIL`, or `BLOCKED` for every item.

| Step | Expected result | Result |
|---|---|---|
| 1. App opens | SASE-310 reaches its initial screen without crashing. | |
| 2. Enter Secretaría | The Secretaría workspace is reachable using the current test path. | |
| 3. Open institutional record | A mock/institutional student record opens. | |
| 4. Review identity header | Name, group and matrícula area render without overlap or truncation that blocks use. | |
| 5. Open Documents | The documents area opens and its controls are visible. | |
| 6. Return safely | Back navigation returns to the previous record/workspace without losing the session unexpectedly. | |

## PASS/FAIL rule

- **PASS:** technical run succeeds and all six manual steps pass.
- **FAIL:** any reproducible build, install, launch, crash, navigation or rendering defect occurs.
- **BLOCKED:** the environment or test data prevents completion; this is not a product PASS.

## Sanitized evidence

Allowed:

- app version or commit SHA;
- Android major version;
- generic device class such as `phone` or `tablet`;
- checklist result;
- concise reproduction steps;
- cropped screenshot only when it contains no names, CURP, matrícula, email, account, serial number, IP address or notification content.

Never commit:

- `adb devices -l` output;
- device serials;
- raw `uiautomator` XML;
- full logcat dumps;
- screen recordings;
- real student or staff data;
- credentials or tokens.

Use `docs/testing/ANDROID_SMOKE_RESULT_TEMPLATE.md` for the final record.
