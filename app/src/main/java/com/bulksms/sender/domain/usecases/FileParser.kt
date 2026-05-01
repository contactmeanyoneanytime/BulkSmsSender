// Save at: app/src/main/java/com/bulksms/sender/domain/usecases/FileParser.kt

package com.bulksms.sender.domain.usecases

import android.content.Context
import com.opencsv.CSVReader
import org.apache.poi.ss.usermodel.WorkbookFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class FileParser {

    suspend fun extractPhoneNumbers(inputStream: InputStream, fileName: String): List<String> {
        return withContext(Dispatchers.IO) {
            val numbers = mutableListOf<String>()

            try {
                when {
                    fileName.endsWith(".txt") -> {
                        numbers.addAll(parseTxtFile(inputStream))
                    }
                    fileName.endsWith(".csv") -> {
                        numbers.addAll(parseCsvFile(inputStream))
                    }
                    fileName.endsWith(".xls") || fileName.endsWith(".xlsx") -> {
                        numbers.addAll(parseExcelFile(inputStream))
                    }
                }
            } finally {
                inputStream.close()
            }

            numbers.distinct()
        }
    }

    private fun parseTxtFile(inputStream: InputStream): List<String> {
        val content = inputStream.bufferedReader().use { it.readText() }
        return PhoneNumberUtils.extractNumbersFromText(content)
    }

    private fun parseCsvFile(inputStream: InputStream): List<String> {
        val numbers = mutableListOf<String>()
        CSVReader(inputStream.bufferedReader()).use { reader ->
            reader.forEach { row ->
                row.forEach { cell ->
                    numbers.addAll(PhoneNumberUtils.extractNumbersFromText(cell))
                }
            }
        }
        return numbers
    }

    private fun parseExcelFile(inputStream: InputStream): List<String> {
        val numbers = mutableListOf<String>()
        val workbook = WorkbookFactory.create(inputStream)

        for (sheetIndex in 0 until workbook.numberOfSheets) {
            val sheet = workbook.getSheetAt(sheetIndex)
            for (row in sheet) {
                row.forEach { cell ->
                    val cellValue = when (cell.cellType) {
                        org.apache.poi.ss.usermodel.CellType.STRING -> cell.stringCellValue
                        org.apache.poi.ss.usermodel.CellType.NUMERIC -> cell.numericCellValue.toString()
                        else -> ""
                    }
                    numbers.addAll(PhoneNumberUtils.extractNumbersFromText(cellValue))
                }
            }
        }
        workbook.close()
        return numbers
    }
}