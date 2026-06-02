package com.pesasense.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.pesasense.model.Transaction
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExportHelper {

    fun exportToCsv(context: Context, transactions: List<Transaction>): File? {
        if (transactions.isEmpty()) return null

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "PesaSense_$stamp.csv"
        val csvContent = buildCsvContent(transactions)

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return null
                resolver.openOutputStream(uri)?.use { it.write(csvContent.toByteArray(Charsets.UTF_8)) }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(dir, fileName)
                file.writeText(csvContent, Charsets.UTF_8)
                file
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun buildCsvContent(transactions: List<Transaction>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val sb = StringBuilder()
        sb.appendLine("Date,Type,Category,Payee,Amount (KES),Fee (KES),Balance After,M-PESA Ref")
        transactions
            .filter { !it.isFeeTransaction }
            .sortedByDescending { it.timestamp }
            .forEach { t ->
                val date = dateFormat.format(Date(t.timestamp))
                val type = t.type.name.replace("_", " ")
                val payee = "\"${t.payee.replace("\"", "\"\"")}\""
                sb.appendLine(
                    "$date,$type,${t.category},$payee," +
                    "${"%.2f".format(t.amount)},${"%.2f".format(t.fee)}," +
                    "${"%.2f".format(t.balanceAfter)},${t.remoteRef}"
                )
            }
        val grandTotal = transactions.filter { !it.isFeeTransaction }.sumOf { it.amount }
        sb.appendLine("TOTAL,,,,${String.format("%.2f", grandTotal)},,,")
        return sb.toString()
    }
}
