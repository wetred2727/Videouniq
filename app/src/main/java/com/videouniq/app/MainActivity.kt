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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        var progress by remember { mutableStateOf(0f) }
        var current by remember { mutableIntStateOf(0) }
        var completed by remember { mutableIntStateOf(0) }

        val picker = rememberLauncherForActivityResult(
            ActivityResultContracts.GetMultipleContents()
        ) { uris ->
            selected = uris
            status = if (uris.isEmpty()) "Відео не вибрано" else "Вибрано відео: ${uris.size}"
            progress = 0f
        }

        fun startBatch() {
            if (selected.isEmpty() || busy) return
            busy = true
            current = 0
            completed = 0
            processNext(
                selected = selected,
                index = 0,
                onStatus = { status = it },
                onProgress = { progress = it },
                onDone = {
                    completed++
                    current = completed
                    if (completed >= selected.size) {
                        busy = false
                        status = "Готово! Оброблено: $completed"
                        progress = 1f
                    }
                },
                onError = { message ->
                    completed++
                    current = completed
                    status = message
                    if (completed >= selected.size) {
                        busy = false
                    } else {
                        processNext(selected, completed, { status = it }, { progress = it }, {
                            completed++
                            current = completed
                            if (completed >= selected.size) {
                                busy = false
                                status = "Завершено. Успішно: ${completed - 1} з ${selected.size}"
                            }
                        }, { err ->
                            status = err
                            completed++
                            if (completed >= selected.size) busy = false
                        })
                    }
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
                Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text("VIDEOUNIQ", color = Color.White, fontSize = 27.sp)
                    Text("Автоматична обробка відео", color = Color(0xFFAAAABD))
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1D27))) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("ТВОЇ ВІДЕО", color = Color.White)
                            Text("Вибери один або декілька роликів для пакетного експорту.", color = Color(0xFFB9B9C8))
                            Button(onClick = { picker.launch("video/*") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                                Text("ВИБРАТИ ВІДЕО")
                            }
                            Text("Вибрано: ${selected.size}", color = Color(0xFFB9B9C8))
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1D27))) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("РЕЖИМ AUTO", color = Color.White)
                            Text("Перекодування відео у новий MP4-файл зі збереженням візуального вмісту та звуку. Оригінали не змінюються.", color = Color(0xFFB9B9C8))
                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                            Text(status, color = Color.White)
                            Button(onClick = { startBatch() }, enabled = selected.isNotEmpty() && !busy, modifier = Modifier.fillMaxWidth()) {
                                Text(if (busy) "ОБРОБКА…" else "ПОЧАТИ ОБРОБКУ")
                            }
                        }
                    }
                    Text("ФАЙЛИ ДОДАНО", color = Color(0xFFAAAABD))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(selected) { uri ->
                            Text(uri.lastPathSegment ?: "Відеофайл", color = Color.White, maxLines = 1)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text("VideoUniq 1.0 • Обробка на пристрої", color = Color(0xFF777887), fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }

    private fun processNext(
        selected: List<Uri>,
        index: Int,
        onStatus: (String) -> Unit,
        onProgress: (Float) -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (index >= selected.size) return
        val uri = selected[index]
        val dir = File(cacheDir, "exports").apply { mkdirs() }
        val output = File(dir, "videouniq_${System.currentTimeMillis()}_$index.mp4")
        onStatus("Обробка ${index + 1} з ${selected.size}…")
        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                try {
                    saveToGallery(output)
                    output.delete()
                    onProgress((index + 1).toFloat() / selected.size)
                    onDone()
                    if (index + 1 < selected.size) {
                        processNext(selected, index + 1, onStatus, onProgress, onDone, onError)
                    }
                } catch (e: Exception) {
                    onError("Не вдалося зберегти файл: ${e.message}")
                }
            }

            override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                output.delete()
                onError("Помилка відео ${index + 1}: ${exportException.message}")
            }
        }
        try {
            transformer = Transformer.Builder(this).addListener(listener).build()
            transformer?.start(EditedMediaItem.Builder(MediaItem.fromUri(uri)).build(), output.absolutePath)
        } catch (e: Exception) {
            output.delete()
            onError("Не вдалося почати обробку: ${e.message}")
        }
    }

    private fun saveToGallery(file: File) {
        val name = "VideoUniq_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/VideoUniq")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Не вдалося створити файл у галереї")
        try {
            contentResolver.openOutputStream(uri)?.use { out ->
                FileInputStream(file).use { input -> input.copyTo(out) }
            } ?: throw IllegalStateException("Не вдалося записати відео")
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            contentResolver.update(uri, values, null, null)
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null)
            throw e
        }
    }
}
