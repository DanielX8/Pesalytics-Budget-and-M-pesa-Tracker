package com.pesalytics.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.InputStream

sealed class RestoreParseResult {
    data class Success(val schema: BackupSchema) : RestoreParseResult()
    data class InvalidFormat(val message: String) : RestoreParseResult()
    data class UnsupportedVersion(val version: Int) : RestoreParseResult()
    object EmptyFile : RestoreParseResult()
}

object JsonRestoreHelper {

    suspend fun parse(context: Context, uri: Uri): RestoreParseResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                return@withContext RestoreParseResult.InvalidFormat("Could not open file.")
            }

            val jsonString = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }

            val json = Json { ignoreUnknownKeys = true }
            val schema: BackupSchema = json.decodeFromString(jsonString)

            if (schema.version > 1) {
                return@withContext RestoreParseResult.UnsupportedVersion(schema.version)
            }

            if (schema.transactions.isEmpty() && schema.bills.isEmpty() && 
                schema.budgets.isEmpty() && schema.goals.isEmpty() && schema.custom_rules.isEmpty()) {
                return@withContext RestoreParseResult.EmptyFile
            }

            RestoreParseResult.Success(schema)
        } catch (e: Exception) {
            e.printStackTrace()
            RestoreParseResult.InvalidFormat("Invalid JSON structure: ${e.message}")
        }
    }
}
