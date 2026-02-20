package com.mediakasir.apotekpos.ui.main.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediakasir.apotekpos.data.model.Transaction
import com.mediakasir.apotekpos.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _total = MutableStateFlow(0)
    val total: StateFlow<Int> = _total.asStateFlow()

    private var currentPage = 1

    fun load(branchId: String, refresh: Boolean = false) {
        viewModelScope.launch {
            if (refresh) {
                currentPage = 1
                _transactions.value = emptyList()
            }
            _isLoading.value = true
            try {
                val res = api.getTransactions(branchId, currentPage)
                _transactions.value = if (refresh) res.data else _transactions.value + res.data
                _total.value = res.total
                currentPage++
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }
}
