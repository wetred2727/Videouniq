package com.videouniq.app

import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Effects
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.Crop
import androidx.media3.effect.ScaleAndRotateTransformation
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

class MainActivity : ComponentActivity() {
    private var transformer: Transformer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VideoUniqScreen() }
    }

    @Composable
    private fun VideoUniqScreen() {
        var selected by remember { mutableStateOf<List<Uri>>(emptyList()) }
        var status by remember { mutableStateOf("Додай відео, щоб почати") }
        var busy by remember { mutableStateOf(false) }
        var progress by remember { mutableFloatStateOf(0f) }
        var intensity by remember { mutableFloatStateOf(0.65f) }

        val picker = rememberLauncherForActivityResult(
            ActivityResultContracts.GetMultipleContents()
        ) { uris ->
            if (!busy) {
                selected = uris
                status = if (uris.isEmpty()) "Відео не вибрано" else "Вибрано відео: ${uris.size}"
                progress = 0f
            }
        }

        fun startBatch() {
            if (selected.isEmpty() || busy) return
            val batch = selected.toList()
            busy = true
            progress = 0f
            status = "Підготовка пакетної обробки…"

            processNext(
                selected = batch,
                index = 0,
                successful = 0,
                intensity = intensity,
                onStatus = { status = it },
                onProgress = { progress = it },
                onFinished = { successCount ->
                    busy = false
                    progress = 1f
                    status = "Завершено. Успішно: $successCount з ${batch.size}"
                }
            )
        }

        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = Color(0xFF8B7CFF),
                background = Color(0xFF101116),
                surface = Color(0xFF1B1D27)
            )
        ) {
            Surface(Modifier.fillMaxSize(), color = Color(0xFF101116)) {
                Column(
                    Modifier.fillMaxSize().padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text("VIDEOUNIQ", color = Color.White, fontSize = 27.sp)
                    Text("Автоматична обробка відео", color = Color(0xFFAAAABD))

                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1D27))) {
                        Column(
                            Modifier.fillMaxWidth().padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("ТВОЇ ВІДЕО", color = Color.White)
                            Text(
                                "Вибери один або декілька роликів для пакетного експорту.",
                                color = Color(0xFFB9B9C8)
                            )
                            Button(
                                onClick = { picker.launch("video/*") },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("ВИБРАТИ ВІДЕО")
                            }
                            Text("Вибрано: ${selected.size}", color = Color(0xFFB9B9C8))
                        }
                    }

                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1D27))) {
                        Column(
                            Modifier.fillMaxWidth().padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("РЕЖИМ AUTO", color = Color.White)
                            Text(
                                "Кожен ролик отримує власні налаштування кадру та кольору. Відео й аудіо перекодовуються в MP4, оригінал не змінюється.",
                                color = Color(0xFFB9B9C8)
                            )
                            Text("СИЛА ОБРОБКИ: ${(intensity * 100).toInt()}%", color = Color.White)
                            Slider(
                                value = intensity,
                                onValueChange = { intensity = it },
                                valueRange = 0.2f..1f,
                                enabled = !busy
                            )
                            Text("Нижче — делікатні зміни. Вище — помітніші зміни кадру та кольору.", color = Color(0xFFB9B9C8), fontSize = 12.sp)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(status, color = Color.White)
                            Button(
                                onClick = { startBatch() },
                                enabled = selected.isNotEmpty() && !busy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (busy) "ОБРОБКА…" else "ПОЧАТИ ОБРОБКУ")
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("ВИБРАНІ ФАЙЛИ", color = Color(0xFFAAAABD))
                        TextButton(onClick = { if (!busy) { selected = emptyList(); status = "Додай відео, щоб почати"; progress = 0f } }, enabled = !busy && selected.isNotEmpty()) { Text("Очистити") }
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(selected) { uri ->
                            Text(
                                uri.lastPathSegment ?: "Відеофайл",
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                    Text(
                        "VideoUniq 1.2.0 • Обробка на пристрої",
                        color = Color(0xFF777887),
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun processNext(
        selected: List<Uri>,
        index: Int,
        successful: Int,
        intensity: Float,
        onStatus: (String) -> Unit,
        onProgress: (Float) -> Unit,
        onFinished: (Int) -> Unit
    ) {
        if (index >= selected.size) {
            onFinished(successful)
            return
        }

        val outputDirectory = File(cacheDir, "exports").apply { mkdirs() }
        val output = File(
            outputDirectory,
            "videouniq_${System.currentTimeMillis()}_$index.mp4"
        )
        onStatus("Обробка ${index + 1} з ${selected.size}…")

        fun finishCurrent(success: Boolean, errorMessage: String? = null) {
            output.delete()
            if (errorMessage != null) onStatus(errorMessage)
            val nextSuccessful = successful + if (success) 1 else 0
            onProgress((index + 1).toFloat() / selected.size)
            processNext(
                selected = selected,
                index = index + 1,
                successful = nextSuccessful,
                intensity = intensity,
                onStatus = onStatus,
                onProgress = onProgress,
                onFinished = onFinished
            )
        }

        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                try {
                    saveToGallery(output, index)
                    finishCurrent(
                        success = true,
                        errorMessage = "Збережено відео ${index + 1} з ${selected.size}"
                    )
                } catch (e: Exception) {
                    finishCurrent(
                        success = false,
                        errorMessage = "Не вдалося зберегти відео ${index + 1}: ${e.message ?: "невідома помилка"}"
                    )
                }
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException
            ) {
                finishCurrent(
                    success = false,
                    errorMessage = "Помилка відео ${index + 1}: ${exportException.message ?: "експорт не вдався"}"
                )
            }
        }

        try {
            transformer = Transformer.Builder(this).addListener(listener).build()
            val editedItem = EditedMediaItem.Builder(MediaItem.fromUri(selected[index]))
                .setEffects(Effects(emptyList(), createAutoVideoEffects(index, intensity)))
                .build()
            transformer?.start(editedItem, output.absolutePath)
        } catch (e: Exception) {
            finishCurrent(
                success = false,
                errorMessage = "Не вдалося почати обробку відео ${index + 1}: ${e.message ?: "невідома помилка"}"
            )
        }
    }


    @OptIn(UnstableApi::class)
    private fun createAutoVideoEffects(index: Int, intensity: Float): List<androidx.media3.common.Effect> {
        // Gentle creative adjustments. Each file receives its own deterministic profile.
        val random = Random(System.nanoTime() xor (index.toLong() shl 32))
        val crop = random.nextFloat() * intensity * 0.025f
        val rotation = (random.nextFloat() - 0.5f) * intensity * 0.8f
        val hue = (random.nextFloat() - 0.5f) * intensity * 12.0f
        val saturation = (random.nextFloat() - 0.5f) * intensity * 18.0f
        val lightness = (random.nextFloat() - 0.5f) * intensity * 8.0f

        return listOf(
            Crop(-1f + crop, 1f - crop, -1f + crop, 1f - crop),
            ScaleAndRotateTransformation.Builder()
                .setRotationDegrees(rotation)
                .build(),
            HslAdjustment.Builder()
                .adjustHue(hue)
                .adjustSaturation(saturation)
                .adjustLightness(lightness)
                .build()
        )
    }

    private fun saveToGallery(file: File, index: Int) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val name = "VideoUniq_${timestamp}_${index + 1}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(
                MediaStore.Video.Media.RELATIVE_PATH,
                Environment.DIRECTORY_MOVIES + "/VideoUniq"
            )
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Не вдалося створити файл у галереї")

        try {
            contentResolver.openOutputStream(uri)?.use { output ->
                FileInputStream(file).use { input -> input.copyTo(output) }
            } ?: throw IllegalStateException("Не вдалося записати відео")

            val publishedValues = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            contentResolver.update(uri, publishedValues, null, null)
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null)
            throw e
        }
    }
}
