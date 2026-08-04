package com.pesalytics.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.pesalytics.model.Transaction
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExportHelper {

    fun exportToCsv(context: Context, transactions: List<Transaction>): File? {
        if (transactions.isEmpty()) return null

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "pesalytics_$stamp.csv"
        val csvContent = buildCsvContent(transactions)

        return try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            
            val file = File(exportDir, fileName)
            file.writeText(csvContent, Charsets.UTF_8)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun buildCsvContent(transactions: List<Transaction>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val sb = StringBuilder()
        sb.appendLine("Date,Type,Category,Payee,Amount (KES),Fee (KES),Balance After,M-PESA Ref,Source,Notes")
        transactions
            .filter { !it.isFeeTransaction }
            .sortedByDescending { it.timestamp }
            .forEach { t ->
                val date = dateFormat.format(Date(t.timestamp))
                val type = t.type.name.replace("_", " ")
                val payee = "\"${t.payee.replace("\"", "\"\"")}\""
                val isManual = t.remoteRef.startsWith("MANUAL_")
                val source = if (isManual) t.remoteRef.removePrefix("MANUAL_") else "M-PESA"
                val notes = "\"${t.originalSms?.replace("\"", "\"\"") ?: ""}\""
                
                sb.appendLine(
                    "$date,$type,${t.category},$payee," +
                    "${"%.2f".format(t.amount)},${"%.2f".format(t.fee)}," +
                    "${"%.2f".format(t.balanceAfter)},${t.remoteRef},$source,$notes"
                )
            }
        val grandTotal = transactions.filter { !it.isFeeTransaction }.sumOf { it.amount }
        sb.appendLine("TOTAL,,,,${String.format("%.2f", grandTotal)},,,,,")
        return sb.toString()
    }
}
