[![](https://jitpack.io/v/smarshall561/Cardalog.svg)](https://jitpack.io/#smarshall561/Cardalog)

# Cardalog

Cardalog is a prototype Android application that scans business cards, extracts
information using OCR and allows the user to confirm the details before creating
or updating a contact on the device.

## Building the project

This project uses Gradle. Ensure you have a recent Android SDK installed. Then
run:

```bash
./gradlew assembleDebug
```

The build expects two modules: `:app` and `:opencvlib`. Prebuilt OpenCV
binaries are included via Gradle dependencies so no additional setup is
required.

## Modules

- **app** – Main Android application containing activities for scanning and
  confirming contact details.
- **opencvlib** – Minimal wrapper library that exposes the OpenCV native
  bindings used by the app.


