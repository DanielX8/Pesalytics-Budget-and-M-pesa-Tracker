package com.pesalytics.patterns

import com.pesalytics.model.Transaction

fun Transaction.isUncategorized(): Boolean =
    !isFeeTransaction &&
    category in setOf("Send Money", "Paybill", "Buy Goods", "Withdraw", "Other", "Transfer", "")
