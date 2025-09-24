package com.encasaxo.hq.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encasaxo.hq.network.dto.PackingListHeader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed interface PackingListUiState {
    object Loading : PackingListUiState
    data class Success(val items: List<PackingListHeader>) : PackingListUiState
    data class Error(val message: String) : PackingListUiState
}

class PackingListViewModel(
    private val repository: PackingListRepository = PackingListRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<PackingListUiState>(PackingListUiState.Loading)
    val uiState: StateFlow<PackingListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = PackingListUiState.Loading
            try {
                val resp = repository.list()
                if (resp.isSuccessful) {
                    val body = resp.body()
                    val headers = body?.packingLists ?: emptyList()
                    _uiState.value = PackingListUiState.Success(headers)
                } else {
                    val err = "Server error: ${resp.code()} ${resp.message()}"
                    _uiState.value = PackingListUiState.Error(err)
                }
            } catch (e: IOException) {
                _uiState.value = PackingListUiState.Error("Network error: ${e.message ?: "unknown"}")
            } catch (e: HttpException) {
                _uiState.value = PackingListUiState.Error("HTTP error: ${e.message ?: "unknown"}")
            } catch (e: Exception) {
                _uiState.value = PackingListUiState.Error("Unexpected error: ${e.message ?: "unknown"}")
            }
        }
    }
}
