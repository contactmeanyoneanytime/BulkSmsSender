// Save at: app/src/main/java/com/bulksms/sender/domain/usecases/PhoneNumberUtils.kt

package com.bulksms.sender.domain.usecases

object PhoneNumberUtils {

    fun cleanNumber(number: String): String {
        return number.replace(Regex("[^0-9+]"), "")
    }

    fun normalizeNumber(number: String, countryCode: String): String {
        var cleaned = cleanNumber(number)
        cleaned = cleaned.replace(Regex("^0+"), "")

        return if (cleaned.startsWith("+")) {
            cleaned
        } else {
            "$countryCode$cleaned"
        }
    }

    fun extractNumbersFromText(text: String): List<String> {
        val phoneRegex = Regex("\\+?[0-9\\s\\-()]{7,}")
        return phoneRegex.findAll(text)
            .map { it.value }
            .toList()
    }

    fun validatePhoneNumber(number: String): Boolean {
        val cleaned = cleanNumber(number)
        return cleaned.replace("+", "").length >= 7
    }
}