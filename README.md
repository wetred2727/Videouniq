# VideoUniq

Android app for batch video re-encoding and exporting copies to the device gallery.

## Build
Run `gradle assembleDebug` with JDK 17 and Android SDK 35, or use the GitHub Actions workflow.

The first version uses AndroidX Media3 Transformer to re-encode selected videos locally. It does not modify originals and does not promise that social platforms will treat exports as new content.
