// Save at: app/src/main/java/com/bulksms/sender/presentation/viewmodel/MainViewModel.kt

package com.bulksms.sender.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.bulksms.sender.data.database.SmsDatabase
import com.bulksms.sender.data.models.SmsBatch
import com.bulksms.sender.data.models.SmsMessage
import com.bulksms.sender.domain.usecases.FileParser
import com.bulksms.sender.domain.usecases.PhoneNumberUtils
import com.bulksms.sender.domain.usecases.SmsManagerService
import com.bulksms.sender.workers.SmsSenderWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream

class MainViewModel(
    private val context: Context
) : ViewModel() {

    private val smsManagerService = SmsManagerService(context)
    private val fileParser = FileParser()
    private val database = SmsDatabase.getInstance(context)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadBatches()
    }

    fun loadBatches() {
        viewModelScope.launch {
            val batches = database.batchDao().getAllBatches()
            _uiState.update { it.copy(batches = batches) }
        }
    }

    fun importFile(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val fileName = getFileName(uri)
                if (inputStream != null) {
                    val numbers = fileParser.extractPhoneNumbers(inputStream, fileName)

                    val cleanedNumbers = numbers.map { PhoneNumberUtils.cleanNumber(it) }
                        .filter { it.isNotEmpty() }

                    _uiState.update {
                        it.copy(
                            extractedNumbers = cleanedNumbers,
                            showCountryCodeDialog = true,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Failed to parse file: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun applyCountryCodeAndNormalize(countryCode: String) {
        val normalizedNumbers = _uiState.value.extractedNumbers.map {
            PhoneNumberUtils.normalizeNumber(it, countryCode)
        }.filter { PhoneNumberUtils.validatePhoneNumber(it) }

        _uiState.update {
            it.copy(
                phoneNumbers = normalizedNumbers,
                countryCode = countryCode,
                showCountryCodeDialog = false,
                extractedNumbers = emptyList(),
                error = if (normalizedNumbers.isEmpty()) "No valid phone numbers found" else null
            )
        }
    }

    fun setInterval(seconds: Int) {
        _uiState.update { it.copy(intervalSeconds = seconds) }
    }

    fun setMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    fun addManualNumber(number: String) {
        val cleaned = PhoneNumberUtils.cleanNumber(number)
        if (cleaned.isNotEmpty()) {
            _uiState.update {
                it.copy(phoneNumbers = it.phoneNumbers + cleaned)
            }
        }
    }

    fun removeNumber(index: Int) {
        _uiState.update {
            it.copy(phoneNumbers = it.phoneNumbers.toMutableList().apply { removeAt(index) })
        }
    }

    fun startBulkSend() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.phoneNumbers.isEmpty()) {
                _uiState.update { it.copy(error = "Please add phone numbers") }
                return@launch
            }
            if (state.message.isBlank()) {
                _uiState.update { it.copy(error = "Please enter a message") }
                return@launch
            }

            val simCards = smsManagerService.getAvailableSimCards()
            if (simCards.size > 1 && state.selectedSimSlot == null) {
                _uiState.update { it.copy(showSimSelector = true, availableSims = simCards) }
                return@launch
            }

            proceedWithSending()
        }
    }

    private fun proceedWithSending() {
        viewModelScope.launch {
            val state = _uiState.value

            val batch = SmsBatch(
                totalMessages = state.phoneNumbers.size,
                intervalSeconds = state.intervalSeconds,
                selectedSimSlot = state.selectedSimSlot,
                countryCode = state.countryCode
            )

            database.batchDao().insertBatch(batch)

            val messages = state.phoneNumbers.map { phoneNumber ->
                SmsMessage(
                    phoneNumber = phoneNumber,
                    message = state.message,
                    batchId = batch.batchId
                )
            }

            database.smsDao().insertAllMessages(messages)

            val workRequest = OneTimeWorkRequestBuilder<SmsSenderWorker>()
                .setInputData(workDataOf("batch_id" to batch.batchId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .addTag("sms_batch_${batch.batchId}")
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)

            _uiState.update {
                it.copy(
                    phoneNumbers = emptyList(),
                    message = "",
                    error = null
                )
            }

            loadBatches()
        }
    }

    fun selectSimCard(sim: SmsManagerService.SimCard) {
        _uiState.update {
            it.copy(
                selectedSimSlot = sim.subscriptionId,
                showSimSelector = false
            )
        }
        proceedWithSending()
    }

    fun pauseSending(batchId: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag("sms_batch_$batchId")
    }

    fun resumeSending(batchId: String) {
        viewModelScope.launch {
            val workRequest = OneTimeWorkRequestBuilder<SmsSenderWorker>()
                .setInputData(workDataOf("batch_id" to batchId))
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    fun cancelSending(batchId: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag("sms_batch_$batchId")
        viewModelScope.launch {
            database.smsDao().deleteMessagesByBatch(batchId)
            database.batchDao().deleteBatch(batchId)
            loadBatches()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun getFileName(uri: Uri): String {
        var fileName = "unknown"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }
        return fileName
    }

    data class MainUiState(
        val phoneNumbers: List<String> = emptyList(),
        val message: String = "",
        val intervalSeconds: Int = 2,
        val isLoading: Boolean = false,
        val error: String? = null,
        val extractedNumbers: List<String> = emptyList(),
        val showCountryCodeDialog: Boolean = false,
        val countryCode: String = "+1",
        val selectedSimSlot: Int? = null,
        val showSimSelector: Boolean = false,
        val availableSims: List<SmsManagerService.SimCard> = emptyList(),
        val batches: List<SmsBatch> = emptyList()
    )
}