package com.pesalytics.data

object MerchantCategoryEngine {

    // Ordered list — more specific keywords first, generic ones last.
    // Each pair: keyword (lowercase) to category name.
    private val merchantMap = listOf(
        // Groceries
        "naivas" to "Groceries",
        "carrefour" to "Groceries",
        "quickmart" to "Groceries",
        "chandarana" to "Groceries",
        "eastmatt" to "Groceries",
        "uchumi" to "Groceries",
        "cleanshelf" to "Groceries",
        "tuskys" to "Groceries",
        "market" to "Groceries",

        // Fuel
        "total energies" to "Fuel",
        "totalenergies" to "Fuel",
        "shell" to "Fuel",
        "rubis" to "Fuel",
        "kenol" to "Fuel",
        "hass petroleum" to "Fuel",
        "oilibya" to "Fuel",
        "kobil" to "Fuel",
        "gulf energy" to "Fuel",

        // Eating Out
        "java house" to "Eating Out",
        "artcaffe" to "Eating Out",
        "kfc" to "Eating Out",
        "chicken inn" to "Eating Out",
        "kenchic" to "Eating Out",
        "subway" to "Eating Out",
        "galito" to "Eating Out",
        "pizza inn" to "Eating Out",
        "dormans" to "Eating Out",
        "cafeteria" to "Eating Out",
        "restaurant" to "Eating Out",

        // Transport
        "kaps" to "Transport",
        "uber" to "Transport",
        "bolt" to "Transport",
        "little" to "Transport",
        "faras" to "Transport",
        "super metro" to "Transport",
        "mololine" to "Transport",
        "2nk" to "Transport",
        "express" to "Transport",
        "shuttle" to "Transport",
        "matatu" to "Transport",

        // Savings & Investments
        "sacco" to "Savings",
        "chama" to "Savings",
        
        // Banking
        "kcb" to "Banking",
        "equity bank" to "Banking",
        "equity" to "Banking",
        "cooperative bank" to "Banking",
        "co-op" to "Banking",
        "coop bank" to "Banking",
        "national bank" to "Banking",
        "ncba" to "Banking",
        "stanbic" to "Banking",
        "standard chartered" to "Banking",
        "absa" to "Banking",
        "dtb" to "Banking",
        "family bank" to "Banking",
        "i&m" to "Banking",
        "im bank" to "Banking",
        "prime bank" to "Banking",
        "bank" to "Banking",

        // Utilities
        "kenya power" to "Utilities",
        "kplc" to "Utilities",
        "nairobi water" to "Utilities",
        "mombasa water" to "Utilities",
        "nakuru water" to "Utilities",
        "county" to "Utilities",
        "water" to "Utilities",

        // Airtime & Data
        "safaricom" to "Airtime & Data",
        "airtel" to "Airtime & Data",
        "telkom" to "Airtime & Data",
        "zuku" to "Airtime & Data",
        "faiba" to "Airtime & Data",
        "jamii" to "Airtime & Data",

        // Insurance
        "nhif" to "Insurance",
        "sha" to "Insurance",
        "jubilee" to "Insurance",
        "aar" to "Insurance",
        "cic insurance" to "Insurance",
        "britam" to "Insurance",
        "uap" to "Insurance",
        "sanlam" to "Insurance",
        "madison" to "Insurance",
        "heritage" to "Insurance",
        "insurance" to "Insurance",

        // Government
        "ecitizen" to "Government",
        "e-citizen" to "Government",
        "ntsa" to "Government",
        "kra" to "Government",
        "huduma" to "Government",
        "immigration" to "Government",

        // Healthcare
        "aga khan" to "Healthcare",
        "nairobi hospital" to "Healthcare",
        "mp shah" to "Healthcare",
        "m.p. shah" to "Healthcare",
        "kenyatta" to "Healthcare",
        "mater" to "Healthcare",
        "gertrudes" to "Healthcare",
        "pharmacy" to "Healthcare",
        "chemist" to "Healthcare",
        "hospital" to "Healthcare",
        "clinic" to "Healthcare",

        // Education
        "helb" to "Education",
        "nemis" to "Education",
        "school" to "Education",
        "college" to "Education",
        "university" to "Education",
        "knec" to "Education",

        // Subscriptions
        "netflix" to "Subscriptions",
        "showmax" to "Subscriptions",
        "dstv" to "Subscriptions",
        "canal" to "Subscriptions",
        "youtube" to "Subscriptions",
        "spotify" to "Subscriptions",
    )

    fun categorize(payee: String): String? {
        val lower = payee.lowercase()
        if (lower.contains("fuliza")) return null
        return merchantMap.firstOrNull { (keyword, _) -> lower.contains(keyword) }?.second
    }
}
