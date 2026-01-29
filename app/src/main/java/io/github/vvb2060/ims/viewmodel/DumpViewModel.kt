package io.github.vvb2060.ims.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.vvb2060.ims.ShizukuProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DumpViewModel(application: Application) : AndroidViewModel(application) {
    data class DumpUiState(
        val loading: Boolean = false,
        val text: String = "",
        val error: String? = null,
    )

    private val _uiState = MutableStateFlow(DumpUiState())
    val uiState: StateFlow<DumpUiState> = _uiState.asStateFlow()

    fun loadDump(subId: Int) {
        viewModelScope.launch {
            if (subId < 0) {
                _uiState.value = DumpUiState(error = "invalid subId")
                return@launch
            }
            _uiState.value = DumpUiState(loading = true)
            val text = ShizukuProvider.dumpCarrierConfig(getApplication(), subId)
            if (text == null) {
                _uiState.value = DumpUiState(loading = false, error = "empty result")
            } else {
                _uiState.value = DumpUiState(loading = false, text = text)
            }
        }
    }
}
