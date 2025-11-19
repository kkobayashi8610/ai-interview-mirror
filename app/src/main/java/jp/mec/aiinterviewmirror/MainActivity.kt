package com.example.ai-interview-mirror  // ←あなたのパッケージ名に合わせてください

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    InterviewRecorderScreen()
                }
            }
        }
    }
}

@Composable
fun InterviewRecorderScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // パーミッション
    val cameraPermissionState = remember { mutableStateOf(false) }
    val audioPermissionState = remember { mutableStateOf(false) }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            cameraPermissionState.value =
                permissions[Manifest.permission.CAMERA] ?: false
            audioPermissionState.value =
                permissions[Manifest.permission.RECORD_AUDIO] ?: false
        }

    LaunchedEffect(Unit) {
        requestPermissionsIfNeeded(
            permissionLauncher,
            cameraPermissionState,
            audioPermissionState
        )
    }

    if (!cameraPermissionState.value || !audioPermissionState.value) {
        PermissionExplanation()
    } else {
        CameraWithRecorder(
            lifecycleOwner = lifecycleOwner,
            saveDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        )
    }
}

private fun requestPermissionsIfNeeded(
    launcher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    cameraPerm: MutableState<Boolean>,
    audioPerm: MutableState<Boolean>,
) {
    val needed = mutableListOf<String>()
    if (!cameraPerm.value) needed += Manifest.permission.CAMERA
    if (!audioPerm.value) needed += Manifest.permission.RECORD_AUDIO

    if (needed.isNotEmpty()) {
        launcher.launch(needed.toTypedArray())
    }
}

@Composable
fun PermissionExplanation() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("カメラとマイクの権限が必要です。", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("アプリを再起動して、権限を許可してください。")
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalVideoApi::class)
@Composable
fun CameraWithRecorder(
    lifecycleOwner: LifecycleOwner,
    saveDir: File?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // CameraX 関連の state
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }

    // UI state
    var isRecording by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableStateOf(90) }
    var lastSavedPath by remember { mutableStateOf<String?>(null) }

    // Camera セットアップ
    LaunchedEffect(previewView) {
        val view = previewView ?: return@LaunchedEffect

        val cameraProvider = ProcessCameraProvider.getInstance(context).get()

        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(
                    Quality.FHD,
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                )
            )
            .build()
        val newVideoCapture = VideoCapture.withOutput(recorder)

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(view.surfaceProvider)
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                newVideoCapture
            )
            videoCapture = newVideoCapture
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Column(Modifier.fillMaxSize()) {
        // カメラプレビュー
        AndroidView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            factory = { ctx ->
                PreviewView(ctx).also { pv ->
                    pv.scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView = pv
                }
            }
        )

        // コントロールエリア
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = if (isRecording) "録画中: 残り ${remainingSeconds} 秒"
                else "90秒間の録画を行います",
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    if (!isRecording) {
                        // 録画開始
                        val vc = videoCapture ?: return@Button
                        val dir = saveDir ?: return@Button
                        dir.mkdirs()

                        val fileName = "interview_" +
                                SimpleDateFormat(
                                    "yyyyMMdd_HHmmss",
                                    Locale.JAPAN
                                ).format(Date()) + ".mp4"
                        val file = File(dir, fileName)

                        val outputOptions =
                            FileOutputOptions.Builder(file).build()

                        val recording = vc.output
                            .prepareRecording(context, outputOptions)
                            .withAudioEnabled()
                            .start(
                                ContextCompat.getMainExecutor(context)
                            ) { event ->
                                when (event) {
                                    is VideoRecordEvent.Finalize -> {
                                        if (!event.hasError()) {
                                            lastSavedPath = file.absolutePath
                                        }
                                        isRecording = false
                                        activeRecording = null
                                        remainingSeconds = 90
                                    }
                                    else -> {}
                                }
                            }

                        activeRecording = recording
                        isRecording = true
                        remainingSeconds = 90

                        // 90秒タイマー
                        scope.launch {
                            while (isRecording && remainingSeconds > 0) {
                                delay(1000)
                                remainingSeconds -= 1
                            }
                            if (isRecording) {
                                // 自動停止
                                activeRecording?.stop()
                            }
                        }
                    } else {
                        // 手動停止
                        activeRecording?.stop()
                    }
                },
                enabled = videoCapture != null
            ) {
                Text(if (isRecording) "停止" else "録画開始")
            }

            Spacer(Modifier.height(8.dp))

            lastSavedPath?.let { path ->
                Text(
                    text = "保存先: $path",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
