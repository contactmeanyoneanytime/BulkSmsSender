// Save at: app/src/main/java/com/bulksms/sender/data/database/SmsDao.kt

package com.bulksms.sender.data.database

import androidx.room.*
import com.bulksms.sender.data.models.SmsMessage

@Dao
interface SmsDao {
    @Query("SELECT * FROM smsmessage WHERE batchId = :batchId")
    suspend fun getMessagesByBatch(batchId: String): List<SmsMessage>

    @Insert
    suspend fun insertMessage(message: SmsMessage)

    @Insert
    suspend fun insertAllMessages(messages: List<SmsMessage>)

    @Update
    suspend fun updateMessage(message: SmsMessage)

    @Query("DELETE FROM smsmessage WHERE batchId = :batchId")
    suspend fun deleteMessagesByBatch(batchId: String)
}