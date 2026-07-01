package com.pesalytics.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object Splash

@Serializable
object Onboarding

@Serializable
object Home

@Serializable
object Analytics

@Serializable
object Bills

@Serializable
object Settings

@Serializable
object BudgetPlanner

@Serializable
object Subscription

@Serializable
data class AllTransactions(val filter: String = "All")

@Serializable
object FinancialGoals

@Serializable
object Faq

@Serializable
object Report

@Serializable
object NeedsWants

@Serializable
data class PayeeHistory(val payee: String)
