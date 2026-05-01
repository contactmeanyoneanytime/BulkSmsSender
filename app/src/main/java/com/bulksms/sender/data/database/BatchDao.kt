// Save at: app/src/main/java/com/bulksms/sender/data/database/BatchDao.kt

package com.bulksms.sender.data.database

import androidx.room.*
import com.bulksms.sender.data.models.SmsBatch
import kotlinx.coroutines.flow.Flow

@Dao
interface BatchDao {
    @Query("SELECT * FROM smsbatch ORDER BY startTime DESC")
    suspend fun getAllBatches(): List<SmsBatch>

    @Query("SELECT * FROM smsbatch ORDER BY startTime DESC")
    fun getBatchesFlow(): Flow<List<SmsBatch>>

    @Insert
    suspend fun insertBatch(batch: SmsBatch)

    @Update
    suspend fun updateBatch(batch: SmsBatch)

    @Query("SELECT * FROM smsbatch WHERE batchId = :batchId")
    suspend fun getBatchById(batchId: String): SmsBatch?

    @Query("DELETE FROM smsbatch WHERE batchId = :batchId")
    suspend fun deleteBatch(batchId: String)
}