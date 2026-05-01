// Save at: app/src/main/java/com/bulksms/sender/data/models/SmsMessage.kt

package com.bulksms.sender.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "smsmessage")
data class SmsMessage(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val phoneNumber: String,
    val message: String,
    val status: SmsStatus = SmsStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val batchId: String = ""
)

enum class SmsStatus {
    PENDING, SENT, FAILED, SENDING
}

@Entity(tableName = "smsbatch")
data class SmsBatch(
    @PrimaryKey
    val batchId: String = UUID.randomUUID().toString(),
    val totalMessages: Int,
    val sentMessages: Int = 0,
    val failedMessages: Int = 0,
    val currentNumber: String = "",
    val status: BatchStatus = BatchStatus.PENDING,
    val startTime: Long = System.currentTimeMillis(),
    val intervalSeconds: Int = 2,
    val selectedSimSlot: Int? = null,
    val countryCode: String = "+1"
)

enum class BatchStatus {
    PENDING, RUNNING, PAUSED, COMPLETED, CANCELLED
}