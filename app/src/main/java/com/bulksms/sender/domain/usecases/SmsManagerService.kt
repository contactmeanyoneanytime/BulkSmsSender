// Save at: app/src/main/java/com/bulksms/sender/domain/usecases/SmsManagerService.kt

package com.bulksms.sender.domain.usecases

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class SmsManagerService(private val context: Context) {

    fun getAvailableSimCards(): List<SimCard> {
        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val result = mutableListOf<SimCard>()

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                subscriptionManager.activeSubscriptionInfoList?.forEach { subInfo ->
                    result.add(SimCard(
                        subscriptionId = subInfo.subscriptionId,
                        displayName = subInfo.displayName?.toString() ?: "SIM ${result.size + 1}",
                        carrierName = subInfo.carrierName?.toString() ?: "Unknown"
                    ))
                }
            } else {
                @Suppress("DEPRECATION")
                subscriptionManager.activeSubscriptionInfoList?.forEach { subInfo ->
                    result.add(SimCard(
                        subscriptionId = subInfo.subscriptionId,
                        displayName = subInfo.displayName?.toString() ?: "SIM ${result.size + 1}",
                        carrierName = subInfo.carrierName?.toString() ?: "Unknown"
                    ))
                }
            }
        }

        return result
    }

    suspend fun sendSms(
        phoneNumber: String,
        message: String,
        subscriptionId: Int?
    ): SmsResult = suspendCancellableCoroutine { continuation ->
        try {
            val smsManager = if (subscriptionId != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
            } else {
                SmsManager.getDefault()
            }

            val sentIntent = PendingIntent.getBroadcast(
                context,
                System.currentTimeMillis().toInt(),
                Intent("SMS_SENT").apply { putExtra("number", phoneNumber) },
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )

            val deliveryIntent = PendingIntent.getBroadcast(
                context,
                System.currentTimeMillis().toInt(),
                Intent("SMS_DELIVERED"),
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    context?.unregisterReceiver(this)
                    when (resultCode) {
                        android.app.Activity.RESULT_OK -> {
                            continuation.resume(SmsResult.Success)
                        }
                        SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                            continuation.resume(SmsResult.Failure("Generic failure"))
                        }
                        SmsManager.RESULT_ERROR_NO_SERVICE -> {
                            continuation.resume(SmsResult.Failure("No service"))
                        }
                        SmsManager.RESULT_ERROR_NULL_PDU -> {
                            continuation.resume(SmsResult.Failure("Null PDU"))
                        }
                        SmsManager.RESULT_ERROR_RADIO_OFF -> {
                            continuation.resume(SmsResult.Failure("Radio off"))
                        }
                        else -> {
                            continuation.resume(SmsResult.Failure("Unknown error: $resultCode"))
                        }
                    }
                }
            }

            context.registerReceiver(receiver, IntentFilter("SMS_SENT"))

            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(
                phoneNumber,
                null,
                parts,
                listOf(sentIntent),
                listOf(deliveryIntent)
            )
        } catch (e: Exception) {
            continuation.resume(SmsResult.Failure(e.message ?: "Unknown error"))
        }
    }

    data class SimCard(
        val subscriptionId: Int,
        val displayName: String,
        val carrierName: String
    )

    sealed class SmsResult {
        object Success : SmsResult()
        data class Failure(val error: String) : SmsResult()
    }
}