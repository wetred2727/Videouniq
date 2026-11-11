# VideoUniq

Android app for local batch video processing and MP4 export.

## Automatic processing

- Select multiple videos from the Android media picker.
- Process the selected files sequentially in one batch.
- Apply an independently randomized, adjustable-strength profile to each file:
  - a small crop,
  - a very small rotation,
  - mild hue, saturation and lightness adjustments.
- Re-encode the video and audio into a new MP4 file.
- Keep the source files unchanged.
- Save completed exports to the device gallery under `Movies/VideoUniq`.
- Continue with the next file if one export fails.

The intensity slider controls the strength of the visual adjustments. At lower values, the changes are more subtle. At higher values, crop and color changes become more noticeable.

These are creative editing and re-encoding operations. They do not guarantee that TikTok, Instagram, or another service will classify a video as original or bypass duplicate-content detection. Use the app only for videos you have rights to publish.

## Build

Run `gradle assembleDebug` with JDK 17 and Android SDK 35, or use the GitHub Actions workflow.
