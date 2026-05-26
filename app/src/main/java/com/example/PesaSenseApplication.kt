package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.PesaRepository

class PesaSenseApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { 
        PesaRepository(
            database.transactionDao(),
            database.billDao(),
            database.budgetDao(),
            database.customRuleDao(),
            database.goalDao()
        )
    }
}
