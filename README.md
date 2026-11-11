# VideoUniq

Android app for batch video processing and exporting new MP4 files to the device gallery.

## Automatic processing

- Select multiple videos and process them sequentially in one batch.
- Re-encode each selected video to a new MP4 file.
- Apply a mild per-file visual profile: small scale/rotation and HSL color adjustments.
- Keep the original source files unchanged.
- Save completed exports to `Movies/VideoUniq`.
- Continue the batch if one input file fails.

The automatic visual adjustments are intended as lightweight creative variations. They do not guarantee that TikTok, Instagram, or any other platform will classify an export as original or bypass duplicate-content systems.

## Build

Run `gradle assembleDebug` with JDK 17 and Android SDK 35, or use the GitHub Actions workflow.
