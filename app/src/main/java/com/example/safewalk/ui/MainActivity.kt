package com.example.safewalk.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.example.safewalk.fusion.AlignmentFusionEngine
import com.example.safewalk.fusion.WalkingState
import com.example.safewalk.hardware.HapticFeedbackManager
import com.example.safewalk.hardware.ImuOrientationManager
import com.example.safewalk.utils.PermissionHelper
import com.example.safewalk.vision.CameraManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var imuManager: ImuOrientationManager
    private lateinit var hapticManager: HapticFeedbackManager
    private lateinit var fusionEngine: AlignmentFusionEngine
    private lateinit var cameraManager: CameraManager
    private lateinit var permissionHelper: PermissionHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imuManager = ImuOrientationManager(this)
        hapticManager = HapticFeedbackManager(this)
        fusionEngine = AlignmentFusionEngine()
        permissionHelper = PermissionHelper(this)

        lifecycleScope.launch {
            imuManager.orientation.collect { orientation ->
                viewModel.updateOrientation(orientation)
            }
        }

        lifecycleScope.launch {
            fusionEngine.state.collect { diagnostic ->
                viewModel.updateWalkingState(diagnostic)
                hapticManager.provideFeedback(diagnostic.state, diagnostic.severityDeg)
            }
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel)
                }
            }
        }

        if (permissionHelper.hasRequiredPermissions()) {
            startServices()
        } else {
            permissionHelper.requestPermissions()
        }
    }

    private fun startServices() {
        imuManager.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        imuManager.stop()
        hapticManager.release()
        if (::cameraManager.isInitialized) {
            cameraManager.shutdown()
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(16.dp)
        ) {
            Text(
                text = "방향: ${uiState.currentHeading.toInt()}°",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "상태: ${getStateText(uiState.walkingState)}",
                color = getStateColor(uiState.walkingState),
                style = MaterialTheme.typography.headlineSmall
            )

            if (uiState.driftSeverity > 0) {
                Text(
                    text = "편차: ${uiState.driftSeverity.toInt()}°",
                    color = Color.Yellow,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

fun getStateText(state: WalkingState): String = when(state) {
    WalkingState.ALIGNED -> "정상 보행"
    WalkingState.DRIFTING_LEFT -> "왼쪽 쏠림"
    WalkingState.DRIFTING_RIGHT -> "오른쪽 쏠림"
    WalkingState.UNCERTAIN -> "신호 부족"
}

fun getStateColor(state: WalkingState): androidx.compose.ui.graphics.Color = when(state) {
    WalkingState.ALIGNED -> Color.Green
    WalkingState.DRIFTING_LEFT, WalkingState.DRIFTING_RIGHT -> Color.Red
    WalkingState.UNCERTAIN -> Color.Gray
}
