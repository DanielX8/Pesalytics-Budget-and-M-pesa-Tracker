package com.pesalytics.utils

import android.content.Context
import android.os.Environment
import android.print.PrintAttributes
import android.webkit.WebView
import android.webkit.WebViewClient
import com.pesalytics.model.Transaction
import com.pesalytics.model.TransactionType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportHelper {

    fun generatePdf(context: Context, transactions: List<Transaction>, onComplete: (File?) -> Unit) {
        val htmlContent = generateHtmlReport(transactions)

        val webView = WebView(context)
        webView.settings.javaScriptEnabled = false

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                createPdfFile(context, view, onComplete)
            }
        }

        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    private fun generateHtmlReport(transactions: List<Transaction>): String {
        val dateFmt  = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val tsFmt    = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val generatedOn = dateFmt.format(Date())

        val clean = transactions.filter { !it.isFeeTransaction }

        val incomeTypes = setOf(TransactionType.RECEIVE_MONEY, TransactionType.MANUAL_INCOME)
        val expenseTxns = clean.filter { it.type !in incomeTypes && it.type != TransactionType.MANUAL_TRANSFER }
        val incomeTxns  = clean.filter { it.type in incomeTypes }

        val totalExpense  = expenseTxns.sumOf { it.amount }
        val totalIncome   = incomeTxns.sumOf { it.amount }
        val totalFees     = transactions.filter { it.isFeeTransaction }.sumOf { it.amount }
        val netSavings    = totalIncome - totalExpense
        val txnCount      = clean.size

        // Actual date range from the data
        val minTs = clean.minOfOrNull { it.timestamp }
        val maxTs = clean.maxOfOrNull { it.timestamp }
        val dateRange = if (minTs != null && maxTs != null) {
            "${dateFmt.format(Date(minTs))} – ${dateFmt.format(Date(maxTs))}"
        } else generatedOn
        val daySpan = if (minTs != null && maxTs != null) {
            maxOf(1L, (maxTs - minTs) / (1000L * 60 * 60 * 24)).toInt()
        } else 30
        val dailyAverage = if (daySpan > 0) totalExpense / daySpan else 0.0

        val biggestExpense = expenseTxns.maxByOrNull { it.amount }

        // Category breakdown (expenses)
        val categories = expenseTxns
            .groupBy { it.category ?: "Other" }
            .mapValues { e -> e.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        // Top merchants (expenses)
        val topMerchants = expenseTxns
            .groupBy { it.payee }
            .mapValues { e -> e.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        // Recent transactions for the table (last 20, all types)
        val recentAll = clean.sortedByDescending { it.timestamp }.take(20)

        val catColors = listOf("#49BC4C", "#0B4631", "#4496FF", "#FFB60D", "#FF4E4E", "#94A3B8")

        fun fmt(v: Double) = "%,.2f".format(v)
        fun fmtInt(v: Double) = "%,.0f".format(v)

        val categoryRowsHtml = if (categories.isEmpty()) {
            "<tr><td colspan=\"3\" style=\"text-align:center;color:#64748B;\">No expense data</td></tr>"
        } else {
            categories.mapIndexed { i, (cat, amt) ->
                val pct = if (totalExpense > 0) (amt / totalExpense * 100) else 0.0
                val color = catColors[i % catColors.size]
                val barPct = pct.toInt().coerceIn(0, 100)
                """
                <tr>
                  <td style="padding:10px 12px;">
                    <span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:$color;margin-right:8px;vertical-align:middle;"></span>$cat
                  </td>
                  <td style="padding:10px 12px;">
                    <div style="background:#F1F5F9;border-radius:4px;height:8px;width:100%;min-width:80px;">
                      <div style="background:$color;border-radius:4px;height:8px;width:$barPct%;"></div>
                    </div>
                  </td>
                  <td class="val-col" style="padding:10px 12px;">${"%.1f".format(pct)}%</td>
                  <td class="val-col" style="padding:10px 12px;">KSh ${fmtInt(amt)}</td>
                </tr>
                """.trimIndent()
            }.joinToString("\n")
        }

        val merchantRowsHtml = if (topMerchants.isEmpty()) {
            "<p style=\"color:#64748B;font-size:13px;\">No merchant data</p>"
        } else {
            topMerchants.mapIndexed { i, (name, amt) ->
                """
                <div class="merchant-item">
                  <div class="merchant-name">
                    <div class="merchant-icon">${name.take(1).uppercase()}</div>
                    ${name.take(28)}
                  </div>
                  <div class="merchant-val">KSh ${fmtInt(amt)}</div>
                </div>
                """.trimIndent()
            }.joinToString("\n")
        }

        val txnRowsHtml = recentAll.joinToString("\n") { t ->
            val isIncome = t.type in incomeTypes
            val amtColor = if (isIncome) "#49BC4C" else "#FF4E4E"
            val amtPrefix = if (isIncome) "+" else "-"
            """
            <tr>
              <td style="padding:10px 12px;">${tsFmt.format(Date(t.timestamp))}</td>
              <td style="padding:10px 12px;">${t.type.name.replace("_", " ")}</td>
              <td style="padding:10px 12px;">${t.category ?: "Other"}</td>
              <td style="padding:10px 12px;">${t.payee.take(30)}</td>
              <td class="val-col" style="padding:10px 12px;color:$amtColor;font-weight:600;">$amtPrefix KSh ${fmt(t.amount)}</td>
              <td class="val-col" style="padding:10px 12px;color:#64748B;">${if (t.fee > 0) "KSh ${fmt(t.fee)}" else "—"}</td>
            </tr>
            """.trimIndent()
        }

        val insightText = when {
            categories.isNotEmpty() -> {
                val top = categories.first()
                val pct = if (totalExpense > 0) (top.second / totalExpense * 100) else 0.0
                "${top.first} is your biggest spend category at ${"%.0f".format(pct)}% of total expenses (KSh ${fmtInt(top.second)})."
            }
            else -> "No spending data available for this period."
        }

        return """
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8"/>
<style>
  body { font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; margin: 0; padding: 32px; color: #1E293B; background: #FAFAFA; }
  .page { background: #FFFFFF; border-radius: 12px; padding: 36px; box-shadow: 0 4px 6px rgba(0,0,0,0.05); border: 1px solid #E2E8F0; }
  .header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 32px; border-bottom: 2px solid #F1F5F9; padding-bottom: 20px; }
  .logo-icon { background: #0B4631; color: white; width: 40px; height: 40px; border-radius: 8px; border-bottom-right-radius: 20px; display: inline-flex; align-items: center; justify-content: center; font-weight: 800; font-size: 22px; margin-right: 12px; vertical-align: middle; }
  .logo-text { color: #0B4631; font-weight: 800; font-size: 26px; vertical-align: middle; }
  .logo-sub { color: #64748B; font-size: 10px; font-weight: 600; letter-spacing: 1px; display: block; margin-top: 2px; }
  .report-meta { text-align: right; }
  .report-title { color: #334155; font-weight: 800; font-size: 15px; margin-bottom: 4px; }
  .report-date { color: #64748B; font-size: 12px; }

  .grid2 { display: flex; gap: 20px; margin-bottom: 20px; }
  .glance-card { background: #0B4631; color: white; padding: 24px; border-radius: 12px; min-width: 200px; flex-shrink: 0; }
  .glance-header { font-weight: 700; font-size: 13px; margin-bottom: 20px; letter-spacing: 0.5px; }
  .glance-label { font-size: 11px; color: #86EFAC; margin-bottom: 3px; margin-top: 14px; }
  .glance-value { font-size: 18px; font-weight: 800; }
  .glance-value-small { font-size: 13px; font-weight: 600; color: #BBF7D0; margin-top: 2px; }

  .card { background: white; padding: 20px; border-radius: 12px; border: 1px solid #E2E8F0; flex-grow: 1; }
  .card-title { font-weight: 700; font-size: 13px; margin-bottom: 16px; color: #0F172A; }

  .insight-card { background: #F0FDF4; border: 1px solid #DCFCE7; padding: 16px 20px; border-radius: 12px; margin-bottom: 20px; display: flex; align-items: center; }
  .insight-dot { background: #49BC4C; color: white; width: 22px; height: 22px; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; margin-right: 14px; font-size: 12px; font-weight: 700; flex-shrink: 0; }
  .insight-text { color: #0B4631; font-size: 13px; font-weight: 600; }

  .merchant-item { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; font-size: 12px; }
  .merchant-name { display: flex; align-items: center; font-weight: 600; color: #334155; }
  .merchant-icon { width: 26px; height: 26px; border-radius: 50%; background: #F1F5F9; margin-right: 10px; display: inline-flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 700; color: #334155; flex-shrink: 0; }
  .merchant-val { font-weight: 700; color: #0F172A; }

  table { width: 100%; border-collapse: collapse; font-size: 12px; margin-bottom: 20px; }
  th { background: #F0FDF4; color: #0B4631; text-align: left; padding: 10px 12px; font-weight: 700; border-top: 1px solid #DCFCE7; border-bottom: 2px solid #DCFCE7; }
  td { border-bottom: 1px solid #F8FAFC; color: #334155; vertical-align: middle; }
  .val-col { text-align: right; }
  .section-title { font-weight: 700; font-size: 14px; color: #0F172A; margin: 24px 0 12px; }
  .footer { text-align: center; color: #94A3B8; font-size: 11px; margin-top: 32px; border-top: 1px solid #F1F5F9; padding-top: 16px; }
</style>
</head>
<body>
<div class="page">
  <!-- Header -->
  <div class="header">
    <div>
      <span class="logo-icon">P</span>
      <span class="logo-text">Pesalytics</span>
      <span class="logo-sub">KNOW MORE, GROW MORE</span>
    </div>
    <div class="report-meta">
      <div class="report-title">FINANCIAL REPORT</div>
      <div class="report-date">Period: $dateRange</div>
      <div class="report-date">Generated: $generatedOn</div>
    </div>
  </div>

  <!-- At a Glance + Category Breakdown -->
  <div class="grid2">
    <div class="glance-card">
      <div class="glance-header">AT A GLANCE</div>

      <div class="glance-label">Total Income</div>
      <div class="glance-value">KSh ${fmtInt(totalIncome)}</div>

      <div class="glance-label">Total Expenses</div>
      <div class="glance-value">KSh ${fmtInt(totalExpense)}</div>

      <div class="glance-label">Net ${if (netSavings >= 0) "Savings" else "Deficit"}</div>
      <div class="glance-value" style="color:${if (netSavings >= 0) "#4ADE80" else "#FF4E4E"};">KSh ${fmtInt(kotlin.math.abs(netSavings))}</div>

      <div class="glance-label">Daily Average Spend</div>
      <div class="glance-value">KSh ${fmtInt(dailyAverage)}</div>

      <div class="glance-label">Transactions</div>
      <div class="glance-value">$txnCount</div>

      ${if (totalFees > 0) """<div class="glance-label">Carrier Fees Paid</div><div class="glance-value-small">KSh ${fmtInt(totalFees)}</div>""" else ""}

      ${if (biggestExpense != null) """
      <div class="glance-label">Biggest Expense</div>
      <div class="glance-value-small">KSh ${fmtInt(biggestExpense.amount)}</div>
      <div style="font-size:10px;color:#86EFAC;">${biggestExpense.payee.take(22)} · ${dateFmt.format(Date(biggestExpense.timestamp))}</div>
      """ else ""}
    </div>

    <div class="card">
      <div class="card-title">Expense Breakdown by Category</div>
      <table>
        <tr>
          <th>Category</th>
          <th>Share</th>
          <th class="val-col">%</th>
          <th class="val-col">Amount (KSh)</th>
        </tr>
        $categoryRowsHtml
        <tr style="font-weight:700;background:#FAFAFA;">
          <td colspan="2" style="padding:10px 12px;">Total Expenses</td>
          <td class="val-col" style="padding:10px 12px;">100%</td>
          <td class="val-col" style="padding:10px 12px;">KSh ${fmtInt(totalExpense)}</td>
        </tr>
      </table>
    </div>
  </div>

  <!-- Insight -->
  <div class="insight-card">
    <div class="insight-dot">✓</div>
    <div class="insight-text">$insightText</div>
  </div>

  <!-- Top Merchants -->
  <div class="grid2">
    <div class="card" style="flex:1;">
      <div class="card-title">Top Merchants / Payees</div>
      $merchantRowsHtml
    </div>
    <div class="card" style="flex:1;">
      <div class="card-title">Summary</div>
      <table>
        <tr><th>Metric</th><th class="val-col">Value</th></tr>
        <tr><td style="padding:8px 12px;">Total Income</td><td class="val-col" style="padding:8px 12px;color:#49BC4C;font-weight:700;">KSh ${fmtInt(totalIncome)}</td></tr>
        <tr><td style="padding:8px 12px;">Total Expenses</td><td class="val-col" style="padding:8px 12px;color:#FF4E4E;font-weight:700;">KSh ${fmtInt(totalExpense)}</td></tr>
        <tr><td style="padding:8px 12px;">Carrier Fees</td><td class="val-col" style="padding:8px 12px;">KSh ${fmtInt(totalFees)}</td></tr>
        <tr style="font-weight:700;"><td style="padding:8px 12px;">Net ${if (netSavings >= 0) "Savings" else "Deficit"}</td><td class="val-col" style="padding:8px 12px;color:${if (netSavings >= 0) "#49BC4C" else "#FF4E4E"};">KSh ${fmtInt(kotlin.math.abs(netSavings))}</td></tr>
        <tr><td style="padding:8px 12px;">Transactions</td><td class="val-col" style="padding:8px 12px;">$txnCount</td></tr>
        <tr><td style="padding:8px 12px;">Date Range</td><td class="val-col" style="padding:8px 12px;font-size:11px;">$dateRange</td></tr>
      </table>
    </div>
  </div>

  <!-- Transaction Table -->
  <div class="section-title">Recent Transactions (last ${recentAll.size})</div>
  <table>
    <tr>
      <th>Date &amp; Time</th>
      <th>Type</th>
      <th>Category</th>
      <th>Payee / Merchant</th>
      <th class="val-col">Amount (KSh)</th>
      <th class="val-col">Fee (KSh)</th>
    </tr>
    $txnRowsHtml
  </table>

  <div class="footer">
    Generated by Pesalytics · Data processed entirely on-device · For personal use only
  </div>
</div>
</body>
</html>
        """.trimIndent()
    }

    private fun createPdfFile(context: Context, webView: WebView, onComplete: (File?) -> Unit) {
        val fileName = "Pesalytics_Report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
        val printAdapter = webView.createPrintDocumentAdapter(fileName)
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
        printManager.print("Pesalytics Report", printAdapter, PrintAttributes.Builder().build())
        onComplete(null)
    }
}
