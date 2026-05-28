package com.example.utils

import android.content.Context
import android.os.Environment
import com.example.model.Transaction
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExportHelper {

    fun exportToCsv(context: Context, transactions: List<Transaction>): File? {
        if (!isExternalStorageWritable()) {
            return null
        }

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val fileName = "PesaSense_Export_${System.currentTimeMillis()}.csv"
        val file = File(downloadsDir, fileName)

        try {
            val writer = FileWriter(file)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            // Write CSV Header matching the requested screenshot format
            writer.append("Date,Category,Merchant,Amount (KSh),Transaction Count\n")

            // Group by date, category, merchant if needed, or just list transactions
            // The screenshot shows unique transactions grouped or listed with count
            val groupedTransactions = transactions.groupBy { 
                "${dateFormat.format(Date(it.timestamp))}_${it.category}_${it.payee}" 
            }

            for ((_, group) in groupedTransactions) {
                val first = group.first()
                val dateStr = dateFormat.format(Date(first.timestamp))
                val category = first.category ?: "Uncategorized"
                val merchant = first.payee
                val totalAmount = group.sumOf { it.amount }
                val count = group.size

                writer.append("$dateStr,$category,\"$merchant\",$totalAmount,$count\n")
            }

            // Append Total Row
            val grandTotal = transactions.sumOf { it.amount }
            val totalCount = transactions.size
            writer.append("TOTAL,-,-,$grandTotal,$totalCount\n")

            writer.flush()
            writer.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun isExternalStorageWritable(): Boolean {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }
}
