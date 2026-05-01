// Save at: app/src/main/java/com/bulksms/sender/data/database/SmsDatabase.kt

package com.bulksms.sender.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.bulksms.sender.data.models.SmsMessage
import com.bulksms.sender.data.models.SmsBatch

@Database(
    entities = [SmsMessage::class, SmsBatch::class],
    version = 1,
    exportSchema = false
)
abstract class SmsDatabase : RoomDatabase() {
    abstract fun smsDao(): SmsDao
    abstract fun batchDao(): BatchDao

    companion object {
        @Volatile
        private var INSTANCE: SmsDatabase? = null

        fun getInstance(context: Context): SmsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmsDatabase::class.java,
                    "sms_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}