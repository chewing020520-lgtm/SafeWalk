package com.example.safewalk.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safewalk.fusion.FusedDiagnostic
import com.example.safewalk.fusion.WalkingState
import com.example.safewalk.hardware.OrientationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class UiState(
    val isNavigating: Boolean = false,
    val currentHeading: Float = 0f,
    val walkingState: WalkingState = WalkingState.ALIGNED,
    val driftSeverity: Float = 0f,
    val debugInfo: String = ""
)

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    fun updateOrientation(data: OrientationData) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                currentHeading = data.azimuthDeg,
                debugInfo = "Heading: ${data.azimuthDeg.toInt()}°"
            )
        }
    }

    fun updateWalkingState(diagnostic: FusedDiagnostic) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                walkingState = diagnostic.state,
                driftSeverity = diagnostic.severityDeg,
                debugInfo = _uiState.value.debugInfo + "\n${diagnostic.state.name}: ${diagnostic.severityDeg.toInt()}°"
            )
        }
    }

    fun startNavigation() {
        _uiState.value = _uiState.value.copy(isNavigating = true)
    }

    fun stopNavigation() {
        _uiState.value = _uiState.value.copy(isNavigating = false)
    }
}
