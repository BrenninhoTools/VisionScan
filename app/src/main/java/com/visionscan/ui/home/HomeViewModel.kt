package com.visionscan.ui.home

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.visionscan.R
import com.visionscan.VisionScanApp
import com.visionscan.data.ScanRepository
import com.visionscan.model.ScanDocument
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val documents: List<ScanDocument> = emptyList(),
    val isLoading: Boolean = true
)

class HomeViewModel(private val repository: ScanRepository) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _messages = Channel<Int>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val documents = repository.documents()
            _state.update { it.copy(documents = documents, isLoading = false) }
        }
    }

    fun onScanned(uri: Uri) {
        viewModelScope.launch {
            val result = runCatching { repository.import(uri) }
            notify(if (result.isSuccess) R.string.scan_saved else R.string.error_scan_failed)
            refresh()
        }
    }

    fun rename(document: ScanDocument, name: String) {
        viewModelScope.launch {
            if (repository.rename(document, name) == null) notify(R.string.error_rename_failed)
            refresh()
        }
    }

    fun delete(document: ScanDocument) {
        viewModelScope.launch {
            repository.delete(document)
            refresh()
        }
    }

    fun notify(@StringRes message: Int) {
        _messages.trySend(message)
    }

    suspend fun thumbnail(document: ScanDocument, width: Int) = repository.thumbnail(document, width)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                HomeViewModel((this[APPLICATION_KEY] as VisionScanApp).scanRepository)
            }
        }
    }
}
