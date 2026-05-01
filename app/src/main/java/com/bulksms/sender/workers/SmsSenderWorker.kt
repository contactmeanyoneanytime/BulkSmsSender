// Save at: app/src/main/java/com/bulksms/sender/workers/SmsSenderWorker.kt

package com.bulksms.sender.workers

import android.content.Context
import androidx.work.*
import com.bulksms.sender.data.database.SmsDatabase
import com.bulksms.sender.data.models.BatchStatus
import com.bulksms.sender.data.models.SmsStatus
import com.bulksms.sender.domain.usecases.SmsManagerService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit

class SmsSenderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val smsManagerService = SmsManagerService(context)
    private val database = SmsDatabase.getInstance(context)

    private val _progress = MutableStateFlow<SendingProgress?>(null)
    val progress: StateFlow<SendingProgress?> = _progress

    override suspend fun doWork(): Result {
        val batchId = inputData.getString("batch_id") ?: return Result.failure()
        val batch = database.batchDao().getBatchById(batchId) ?: return Result.failure()

        val messages = database.smsDao().getMessagesByBatch(batchId)
        val intervalMs = batch.intervalSeconds * 1000L

        var currentIndex = messages.indexOfFirst { it.status == SmsStatus.PENDING }
        if (currentIndex == -1) currentIndex = 0

        var sent = messages.count { it.status == SmsStatus.SENT }
        var failed = messages.count { it.status == SmsStatus.FAILED }

        batch.status = BatchStatus.RUNNING
        database.batchDao().updateBatch(batch)

        while (currentIndex < messages.size) {
            val message = messages[currentIndex]

            if (message.status == SmsStatus.SENT || message.status == SmsStatus.FAILED) {
                currentIndex++
                continue
            }

            _progress.value = SendingProgress(
                total = messages.size,
                sent = sent,
                failed = failed,
                currentNumber = message.phoneNumber
            )

            batch.currentNumber = message.phoneNumber
            batch.sentMessages = sent
            batch.failedMessages = failed
            database.batchDao().updateBatch(batch)

            message.status = SmsStatus.SENDING
            database.smsDao().updateMessage(message)

            val result = smsManagerService.sendSms(
                message.phoneNumber,
                message.message,
                batch.selectedSimSlot
            )

            when (result) {
                is SmsManagerService.SmsResult.Success -> {
                    message.status = SmsStatus.SENT
                    sent++
                }
                is SmsManagerService.SmsResult.Failure -> {
                    message.status = SmsStatus.FAILED
                    message.errorMessage = result.error
                    failed++

                    if (message.retryCount < 3) {
                        message.retryCount++
                        message.status = SmsStatus.PENDING
                        database.smsDao().updateMessage(message)
                        continue
                    }
                }
            }

            database.smsDao().updateMessage(message)
            currentIndex++

            if (currentIndex < messages.size && messages[currentIndex].status != SmsStatus.PENDING) {
                continue
            }

            if (currentIndex < messages.size) {
                delay(intervalMs)
            }

            if (currentIndex % 5 == 0) {
                batch.sentMessages = sent
                batch.failedMessages = failed
                database.batchDao().updateBatch(batch)
            }
        }

        batch.status = BatchStatus.COMPLETED
        batch.sentMessages = sent
        batch.failedMessages = failed
        database.batchDao().updateBatch(batch)

        return Result.success()
    }

    data class SendingProgress(
        val total: Int,
        val sent: Int,
        val failed: Int,
        val currentNumber: String
    )
}