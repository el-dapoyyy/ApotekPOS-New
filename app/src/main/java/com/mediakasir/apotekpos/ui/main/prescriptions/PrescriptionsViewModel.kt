package com.mediakasir.apotekpos.ui.main.prescriptions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediakasir.apotekpos.data.model.PosPrescriptionDto
import com.mediakasir.apotekpos.data.network.ApiService
import com.mediakasir.apotekpos.util.mapNetworkOrGenericError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrescriptionsViewModel @Inject constructor(
    private val api: ApiService,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _items = MutableStateFlow<List<PosPrescriptionDto>>(emptyList())
    val items: StateFlow<List<PosPrescriptionDto>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun load(search: String = "", status: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val env = api.listPrescriptions(
                    page = 1,
                    perPage = 20,
                    search = search.takeIf { it.isNotBlank() },
                    status = status,
                )
                if (env.success != true) {
                    throw IllegalStateException(env.message ?: "Gagal memuat resep")
                }
                _items.value = env.data?.prescriptions.orEmpty()
            } catch (e: Exception) {
                _error.value = appContext.mapNetworkOrGenericError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
