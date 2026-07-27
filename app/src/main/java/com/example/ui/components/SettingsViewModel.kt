package com.example.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    data object Ready : DownloadState()
    data object Error : DownloadState()
}

sealed class ConnectionStatus {
    data object Checking : ConnectionStatus()
    data object Online : ConnectionStatus()
    data object Offline : ConnectionStatus()
    data object Error : ConnectionStatus()
}

class SettingsViewModel : ViewModel() {

    private val _sttDownloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val sttDownloadState: StateFlow<DownloadState> = _sttDownloadState

    private val _ttsDownloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val ttsDownloadState: StateFlow<DownloadState> = _ttsDownloadState

    private val _zenAiStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Checking)
    val zenAiStatus: StateFlow<ConnectionStatus> = _zenAiStatus

    private val _hasZenApiKey = MutableStateFlow(false)
    val hasZenApiKey: StateFlow<Boolean> = _hasZenApiKey

    private val _hasGeminiApiKey = MutableStateFlow(false)
    val hasGeminiApiKey: StateFlow<Boolean> = _hasGeminiApiKey

    init {
        checkApiKeys()
        checkZenAiConnection()
    }

    private fun checkApiKeys() {
        _hasGeminiApiKey.value = try {
            val key = BuildConfig.GEMINI_API_KEY
            key.isNotBlank() && !key.contains("MY_GEMINI_API_KEY")
        } catch (_: Exception) { false }

        _hasZenApiKey.value = try {
            val key = BuildConfig.ZENAI_API_KEY
            key.isNotBlank() && !key.contains("MY_ZENAI_API_KEY")
        } catch (_: Exception) { false }
    }

    private fun checkZenAiConnection() {
        viewModelScope.launch {
            delay(1500)
            _zenAiStatus.value = ConnectionStatus.Online
        }
    }

    fun downloadSttModel() {
        if (_sttDownloadState.value is DownloadState.Downloading) return
        viewModelScope.launch {
            try {
                _sttDownloadState.value = DownloadState.Downloading(0f)
                for (i in 1..10) {
                    delay(300)
                    _sttDownloadState.value = DownloadState.Downloading(i / 10f)
                }
                _sttDownloadState.value = DownloadState.Ready
            } catch (_: Exception) {
                _sttDownloadState.value = DownloadState.Error
            }
        }
    }

    fun downloadTtsModel() {
        if (_ttsDownloadState.value is DownloadState.Downloading) return
        viewModelScope.launch {
            try {
                _ttsDownloadState.value = DownloadState.Downloading(0f)
                for (i in 1..10) {
                    delay(300)
                    _ttsDownloadState.value = DownloadState.Downloading(i / 10f)
                }
                _ttsDownloadState.value = DownloadState.Ready
            } catch (_: Exception) {
                _ttsDownloadState.value = DownloadState.Error
            }
        }
    }
}