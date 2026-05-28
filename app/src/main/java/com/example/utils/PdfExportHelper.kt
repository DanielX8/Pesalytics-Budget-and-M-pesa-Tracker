package com.example.utils

import android.content.Context
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.model.Transaction
import java.io.File

object PdfExportHelper {

    fun generatePdf(context: Context, transactions: List<Transaction>, onComplete: (File?) -> Unit) {
        val htmlContent = generateHtmlReport(transactions)
        
        val webView = WebView(context)
        webView.settings.javaScriptEnabled = true
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                createPdfFile(context, view, onComplete)
            }
        }
        
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    private fun generateHtmlReport(transactions: List<Transaction>): String {
        val expenses = transactions.filter { it.type != com.example.model.TransactionType.RECEIVE_MONEY && it.type != com.example.model.TransactionType.MANUAL_INCOME }
        val totalExpenses = expenses.sumOf { it.amount }
        val transactionCount = expenses.size
        
        val dailyAverage = if (transactionCount > 0) totalExpenses / 30 else 0.0 // Assuming a month
        val biggestExpense = expenses.maxByOrNull { it.amount }
        
        // Basic category breakdown mockup
        val categories = expenses.groupBy { it.category ?: "Other" }.mapValues { entry -> entry.value.sumOf { it.amount } }.toList().sortedByDescending { it.second }
        val topMerchants = expenses.groupBy { it.payee }.mapValues { entry -> entry.value.sumOf { it.amount } }.toList().sortedByDescending { it.second }.take(4)

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; margin: 0; padding: 40px; color: #1E293B; background: #FAFAFA; }
                    .page-container { background: #FFFFFF; border-radius: 12px; padding: 40px; box-shadow: 0 4px 6px rgba(0,0,0,0.05); border: 1px solid #E2E8F0; }
                    .header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 40px; border-bottom: 2px solid #F1F5F9; padding-bottom: 20px; }
                    .logo-area { display: flex; align-items: center; }
                    .logo-icon { background: #166534; color: white; width: 40px; height: 40px; border-radius: 8px; border-bottom-right-radius: 20px; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 24px; margin-right: 12px; }
                    .logo-text { color: #166534; font-weight: 800; font-size: 28px; line-height: 1; }
                    .logo-sub { color: #64748B; font-size: 10px; font-weight: 600; letter-spacing: 1px; margin-top: 4px; }
                    .report-title-area { text-align: right; }
                    .report-title { color: #334155; font-weight: 800; font-size: 16px; margin-bottom: 4px; }
                    .report-date { color: #64748B; font-size: 12px; }
                    
                    .main-grid { display: flex; gap: 24px; margin-bottom: 24px; }
                    .glance-card { background: #166534; color: white; padding: 24px; border-radius: 12px; width: 220px; flex-shrink: 0; }
                    .glance-header { font-weight: bold; font-size: 14px; margin-bottom: 24px; }
                    .glance-label { font-size: 11px; color: #BBF7D0; margin-bottom: 4px; }
                    .glance-value { font-size: 20px; font-weight: 800; margin-bottom: 20px; }
                    
                    .breakdown-card { background: white; padding: 24px; border-radius: 12px; border: 1px solid #E2E8F0; flex-grow: 1; }
                    .card-title { font-weight: bold; font-size: 14px; margin-bottom: 20px; color: #0F172A; }
                    
                    .breakdown-content { display: flex; align-items: center; justify-content: center; gap: 40px; }
                    .donut-placeholder { width: 160px; height: 160px; border-radius: 50%; border: 30px solid #E2E8F0; border-top-color: #4ADE80; border-right-color: #166534; border-bottom-color: #334155; display: flex; align-items: center; justify-content: center; flex-direction: column; }
                    .donut-inner-val { font-size: 16px; font-weight: bold; color: #0F172A; }
                    .donut-inner-lbl { font-size: 10px; color: #64748B; }
                    
                    .legend-list { list-style: none; padding: 0; margin: 0; width: 100%; max-width: 200px; }
                    .legend-item { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; font-size: 12px; }
                    .legend-label { display: flex; align-items: center; color: #334155; font-weight: 600; }
                    .legend-dot { width: 10px; height: 10px; border-radius: 50%; margin-right: 8px; }
                    .legend-val { color: #0F172A; }
                    
                    .second-row-grid { display: flex; gap: 24px; margin-bottom: 24px; }
                    .over-time-card { flex: 2; background: white; padding: 24px; border-radius: 12px; border: 1px solid #E2E8F0; }
                    .merchants-card { flex: 1; background: white; padding: 24px; border-radius: 12px; border: 1px solid #E2E8F0; }
                    
                    .merchant-item { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; font-size: 13px; }
                    .merchant-name { display: flex; align-items: center; font-weight: 600; color: #334155; }
                    .merchant-icon { width: 24px; height: 24px; border-radius: 50%; background: #F1F5F9; margin-right: 12px; display: flex; align-items: center; justify-content: center; font-size: 10px; }
                    .merchant-val { font-weight: bold; color: #0F172A; }
                    
                    .insight-card { background: #F0FDF4; border: 1px solid #DCFCE7; padding: 20px; border-radius: 12px; margin-bottom: 40px; display: flex; align-items: center; }
                    .insight-icon { background: #4ADE80; color: white; width: 24px; height: 24px; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin-right: 16px; font-size: 12px; font-weight: bold; }
                    .insight-text { color: #166534; font-size: 13px; font-weight: 600; }
                    
                    table { width: 100%; border-collapse: collapse; font-size: 12px; }
                    th { background-color: #F0FDF4; color: #166534; text-align: left; padding: 12px 16px; font-weight: bold; border-top: 1px solid #DCFCE7; border-bottom: 2px solid #DCFCE7; }
                    td { padding: 12px 16px; border-bottom: 1px solid #F1F5F9; color: #334155; }
                    .val-col { text-align: right; }
                </style>
            </head>
            <body>
                <div class="page-container">
                    <div class="header">
                        <div class="logo-area">
                            <div class="logo-icon">P</div>
                            <div>
                                <div class="logo-text">PesaSense</div>
                                <div class="logo-sub">KNOW MORE, GROW MORE</div>
                            </div>
                        </div>
                        <div class="report-title-area">
                            <div class="report-title">EXPENSE REPORT</div>
                            <div class="report-date">Generated Report</div>
                        </div>
                    </div>
                    
                    <div class="main-grid">
                        <div class="glance-card">
                            <div class="glance-header">At a Glance</div>
                            
                            <div class="glance-label">Total Expenses</div>
                            <div class="glance-value">KSh ${"%.2f".format(totalExpenses)}</div>
                            
                            <div class="glance-label">Daily Average</div>
                            <div class="glance-value">KSh ${"%.2f".format(dailyAverage)}</div>
                            
                            <div class="glance-label">Transactions</div>
                            <div class="glance-value">$transactionCount</div>
                            
                            ${if (biggestExpense != null) """
                            <div class="glance-label">Biggest Expense</div>
                            <div class="glance-value" style="margin-bottom: 4px;">KSh ${"%.2f".format(biggestExpense.amount)}</div>
                            <div style="font-size: 10px; color: #86EFAC;">on ${java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(biggestExpense.timestamp))}</div>
                            """ else ""}
                        </div>
                        
                        <div class="breakdown-card">
                            <div class="card-title">Expense Breakdown</div>
                            <div class="breakdown-content">
                                <div class="donut-placeholder">
                                    <div class="donut-inner-val">KSh ${"%.0f".format(totalExpenses)}</div>
                                    <div class="donut-inner-lbl">Total</div>
                                </div>
                                <ul class="legend-list">
                                    ${categories.take(4).mapIndexed { index, cat -> 
                                        val colors = listOf("#4ADE80", "#166534", "#94A3B8", "#334155")
                                        val percent = if (totalExpenses > 0) (cat.second / totalExpenses) * 100 else 0.0
                                        """
                                        <li class="legend-item">
                                            <span class="legend-label"><span class="legend-dot" style="background: ${colors[index % colors.size]}"></span>${cat.first}</span>
                                            <span class="legend-val">${"%.1f".format(percent)}%</span>
                                        </li>
                                        """
                                    }.joinToString("")}
                                </ul>
                            </div>
                        </div>
                    </div>
                    
                    <div class="second-row-grid">
                        <div class="over-time-card">
                            <div class="card-title">Category Breakdown Over Time</div>
                            <!-- Mockup of the Area Chart -->
                            <div style="width: 100%; height: 140px; background: linear-gradient(to bottom, transparent 30%, #DCFCE7 30%, #DCFCE7 50%, #86EFAC 50%, #86EFAC 70%, #22C55E 70%, #22C55E 85%, #166534 85%); border-bottom: 1px solid #E2E8F0; position: relative;">
                                <div style="position: absolute; bottom: -20px; left: 0; font-size: 10px; color: #64748B;">Start</div>
                                <div style="position: absolute; bottom: -20px; right: 0; font-size: 10px; color: #64748B;">End</div>
                            </div>
                        </div>
                        
                        <div class="merchants-card">
                            <div class="card-title">Top Merchants</div>
                            ${topMerchants.map { 
                                """
                                <div class="merchant-item">
                                    <div class="merchant-name"><div class="merchant-icon">${it.first.take(1).uppercase()}</div>${it.first}</div>
                                    <div class="merchant-val">KSh ${"%.0f".format(it.second)}</div>
                                </div>
                                """
                            }.joinToString("")}
                        </div>
                    </div>
                    
                    <div class="insight-card">
                        <div class="insight-icon">✓</div>
                        <div class="insight-text">${if (categories.isNotEmpty()) "${categories.first().first} is your biggest expense category, accounting for ${"%.0f".format((categories.first().second / totalExpenses) * 100)}% of your spending." else "No spending data available yet."}</div>
                    </div>
                    
                    <div style="text-align: center; margin-bottom: 20px;">
                        <h3 style="margin: 0; color: #0F172A;">Expenses Summary</h3>
                    </div>

                    <table>
                        <tr>
                            <th>Date</th>
                            <th>Category</th>
                            <th>Merchant</th>
                            <th class="val-col">Amount (KSh)</th>
                        </tr>
                        ${expenses.take(15).joinToString("") { 
                            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(it.timestamp))
                            "<tr><td>$dateStr</td><td>${it.category ?: "Other"}</td><td>${it.payee}</td><td class=\"val-col\">${"%.2f".format(it.amount)}</td></tr>" 
                        }}
                    </table>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun createPdfFile(context: Context, webView: WebView, onComplete: (File?) -> Unit) {
        val fileName = "PesaSense_Report_${System.currentTimeMillis()}.pdf"
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        val printAdapter = webView.createPrintDocumentAdapter(fileName)
        // Note: Writing to file automatically using PrintDocumentAdapter without UI is tricky.
        // Usually, PrintManager is invoked here for the user to save it.
        // For a completely silent export, you would use PdfDocument and write the WebView's Picture to it.
        // For now, let's just trigger the Android Print UI, which allows saving as PDF.
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
        val printJob = printManager.print("PesaSense Report", printAdapter, PrintAttributes.Builder().build())
        
        onComplete(null) // Return null as the OS handles the saving via PrintManager
    }
}
