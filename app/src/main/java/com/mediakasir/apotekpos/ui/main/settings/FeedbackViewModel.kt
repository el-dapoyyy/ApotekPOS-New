package com.mediakasir.apotekpos.ui.main.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediakasir.apotekpos.BuildConfig
import com.mediakasir.apotekpos.data.model.FeedbackRequest
import com.mediakasir.apotekpos.data.model.UserInfo
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
class FeedbackViewModel @Inject constructor(
    private val api: ApiService,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun sendFeedback(
        category: String,
        subject: String,
        message: String,
        user: UserInfo?,
        branchId: String,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _isSuccess.value = false
            _errorMessage.value = null
            try {
                val request = FeedbackRequest(
                    category = category,
                    subject = subject,
                    message = message,
                    userId = user?.userId.orEmpty(),
                    branchId = branchId,
                    appVersion = BuildConfig.VERSION_NAME,
                )
                val envelope = api.sendFeedback(request)
                if (envelope.success) {
                    _isSuccess.value = true
                } else {
                    _errorMessage.value = envelope.message ?: "Gagal mengirim masukan"
                }
            } catch (e: Exception) {
                _errorMessage.value = appContext.mapNetworkOrGenericError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetState() {
        _isSuccess.value = false
        _errorMessage.value = null
    }
}
