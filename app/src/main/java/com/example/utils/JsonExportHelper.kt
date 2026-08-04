package com.pesalytics.utils

import android.content.Context
import com.pesalytics.data.PesaRepository
import com.pesalytics.model.Bill
import com.pesalytics.model.Budget
import com.pesalytics.model.CustomRule
import com.pesalytics.model.Goal
import com.pesalytics.model.GoalTransaction
import com.pesalytics.model.Transaction
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class BackupSchema(
    val version: Int = 1,
    val exported_at: String,
    val app_version: String = "1.5.2",
    val transactions: List<Transaction>,
    val bills: List<Bill>,
    val budgets: List<Budget>,
    val goals: List<Goal>,
    val goal_transactions: List<GoalTransaction>,
    val custom_rules: List<CustomRule>
)

object JsonExportHelper {
    suspend fun generateBackup(context: Context, repository: PesaRepository): File? {
        return try {
            val transactions = repository.getTransactionsOnce()
            val bills = repository.getBillsOnce()
            val budgets = repository.getBudgetsOnce()
            val goals = repository.getGoalsOnce()
            val goalTransactions = repository.getGoalTransactionsOnce()
            val rules = repository.getCustomRulesOnce()

            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            val backup = BackupSchema(
                exported_at = dateFormat.format(Date()),
                transactions = transactions,
                bills = bills,
                budgets = budgets,
                goals = goals,
                goal_transactions = goalTransactions,
                custom_rules = rules
            )

            val jsonString = Json { prettyPrint = true }.encodeToString(backup)

            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val fileName = "pesalytics_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
            val file = File(exportDir, fileName)
            file.writeText(jsonString, Charsets.UTF_8)

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
