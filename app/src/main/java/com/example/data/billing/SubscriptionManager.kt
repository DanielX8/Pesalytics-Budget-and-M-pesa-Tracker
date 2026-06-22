package com.pesalytics.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.pesalytics.domain.model.SubscriptionTier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class SubscriptionManager(private val context: Context) : PurchasesUpdatedListener, BillingClientStateListener {

    private val prefs = context.getSharedPreferences("pesa_subscription", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var reconnectDelay = 1_000L

    private val _state = MutableStateFlow(loadStateFromPrefs())
    val state: StateFlow<SubscriptionState> = _state

    private val _products = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val products: StateFlow<Map<String, ProductDetails>> = _products

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    // ── Play Billing connection ──────────────────────────────────────────

    fun connect() {
        if (!billingClient.isReady) billingClient.startConnection(this)
    }

    fun disconnect() {
        billingClient.endConnection()  // stop incoming events before cancelling inflight coroutines
        scope.cancel()
    }

    override fun onBillingSetupFinished(result: BillingResult) {
        reconnectDelay = 1_000L
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            scope.launch { queryProducts(); syncPurchases() }
        }
    }

    override fun onBillingServiceDisconnected() {
        scope.launch {
            delay(reconnectDelay)
            reconnectDelay = (reconnectDelay * 2).coerceAtMost(32_000L)
            connect()
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            scope.launch { processPurchases(purchases.orEmpty()) }
        }
    }

    private suspend fun queryProducts() {
        val subsResult = billingClient.queryProductDetails(
            QueryProductDetailsParams.newBuilder().setProductList(
                BillingConfig.ALL_SUBS.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it).setProductType(BillingClient.ProductType.SUBS).build()
                }
            ).build()
        )
        val inappResult = billingClient.queryProductDetails(
            QueryProductDetailsParams.newBuilder().setProductList(
                BillingConfig.ALL_INAPP.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it).setProductType(BillingClient.ProductType.INAPP).build()
                }
            ).build()
        )
        _products.value = (subsResult.productDetailsList.orEmpty() + inappResult.productDetailsList.orEmpty())
            .associateBy { it.productId }
    }

    suspend fun syncPurchases() {
        val subs = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        )
        val inapp = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        )
        processPurchases(subs.purchasesList + inapp.purchasesList)
    }

    private suspend fun processPurchases(purchases: List<Purchase>) {
        for (purchase in purchases) {
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) continue
            if (!purchase.isAcknowledged) {
                val ack = billingClient.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                )
                if (ack.responseCode != BillingClient.BillingResponseCode.OK) continue
            }
            val tier = tierFromProducts(purchase.products) ?: continue
            val expiryMs = purchase.purchaseTime + expiryDurationMs(tier)
            savePlayBillingState(tier, purchase.purchaseToken, expiryMs)
            _state.value = loadStateFromPrefs()
        }
    }

    private fun tierFromProducts(products: List<String>): SubscriptionTier? = when {
        BillingConfig.SKU_LIFETIME  in products -> SubscriptionTier.PREMIUM_LIFETIME
        BillingConfig.SKU_YEARLY    in products -> SubscriptionTier.PREMIUM_YEARLY
        BillingConfig.SKU_QUARTERLY in products -> SubscriptionTier.PREMIUM_QUARTERLY
        BillingConfig.SKU_MONTHLY   in products -> SubscriptionTier.PREMIUM_MONTHLY
        else -> null
    }

    private fun expiryDurationMs(tier: SubscriptionTier): Long = when (tier) {
        SubscriptionTier.PREMIUM_MONTHLY   -> TimeUnit.DAYS.toMillis(31)
        SubscriptionTier.PREMIUM_QUARTERLY -> TimeUnit.DAYS.toMillis(92)
        SubscriptionTier.PREMIUM_YEARLY    -> TimeUnit.DAYS.toMillis(366)
        SubscriptionTier.PREMIUM_LIFETIME  -> Long.MAX_VALUE / 2
        else -> 0L
    }

    fun launchBillingFlow(activity: Activity, sku: String, offerToken: String? = null): BillingResult {
        val productDetails = _products.value[sku] ?: return BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE).build()
        val params = if (offerToken != null) {
            listOf(BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails).setOfferToken(offerToken).build())
        } else {
            listOf(BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails).build())
        }
        return billingClient.launchBillingFlow(activity,
            BillingFlowParams.newBuilder().setProductDetailsParamsList(params).build())
    }

    // ── State persistence ─────────────────────────────────────────────────

    private fun loadStateFromPrefs(): SubscriptionState {
        val now = System.currentTimeMillis()
        val tierName = prefs.getString("tier", SubscriptionTier.FREE.name) ?: SubscriptionTier.FREE.name
        val tier = runCatching { SubscriptionTier.valueOf(tierName) }.getOrDefault(SubscriptionTier.FREE)

        if (tier == SubscriptionTier.PREMIUM_LIFETIME) {
            return SubscriptionState(tier = tier, expiryMs = Long.MAX_VALUE / 2)
        }
        val expiryMs = prefs.getLong("expiry_ms", 0L)
        if (tier.isPremium && expiryMs > now) {
            return SubscriptionState(tier = tier, expiryMs = expiryMs)
        }
        val referralExpiry = prefs.getLong("referral_expiry_ms", 0L)
        if (referralExpiry > now) {
            return SubscriptionState(tier = SubscriptionTier.PREMIUM_MONTHLY, expiryMs = referralExpiry, source = "referral")
        }
        val trialStart = prefs.getLong("trial_start_ms", 0L)
        val trialDuration = TimeUnit.DAYS.toMillis(14)
        if (trialStart > 0L && (now - trialStart) < trialDuration) {
            val remaining = TimeUnit.MILLISECONDS.toDays(trialDuration - (now - trialStart)).toInt().coerceAtLeast(1)
            return SubscriptionState(tier = SubscriptionTier.TRIAL, expiryMs = trialStart + trialDuration, trialDaysRemaining = remaining)
        }
        // One-time migration: prior versions wrote is_premium=true to a different prefs file.
        // Elevate those users to PREMIUM_LIFETIME so they don't lose access after an update.
        val legacyPrefs = context.getSharedPreferences("pesa_prefs", Context.MODE_PRIVATE)
        if (legacyPrefs.getBoolean("is_premium", false)) {
            prefs.edit()
                .putString("tier", SubscriptionTier.PREMIUM_LIFETIME.name)
                .putLong("expiry_ms", Long.MAX_VALUE / 2)
                .apply()
            legacyPrefs.edit().remove("is_premium").apply()
            return SubscriptionState(tier = SubscriptionTier.PREMIUM_LIFETIME, expiryMs = Long.MAX_VALUE / 2)
        }
        return SubscriptionState(tier = SubscriptionTier.FREE)
    }

    private fun savePlayBillingState(tier: SubscriptionTier, token: String, expiryMs: Long) {
        prefs.edit().putString("tier", tier.name).putLong("expiry_ms", expiryMs)
            .putString("purchase_token", token).apply()
    }

    fun startTrialIfNotStarted() {
        if (prefs.getLong("trial_start_ms", 0L) == 0L) {
            prefs.edit().putLong("trial_start_ms", System.currentTimeMillis()).apply()
            _state.value = loadStateFromPrefs()
        }
    }

    fun grantReferralBonus() {
        prefs.edit().putLong("referral_expiry_ms", System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)).apply()
        _state.value = loadStateFromPrefs()
    }

    fun grantFromPromo(grant: PromoGrant) {
        val now = System.currentTimeMillis()
        when (grant) {
            is PromoGrant.Lifetime -> prefs.edit()
                .putString("tier", SubscriptionTier.PREMIUM_LIFETIME.name)
                .putLong("expiry_ms", Long.MAX_VALUE / 2).apply()
            is PromoGrant.Monthly -> prefs.edit()
                .putString("tier", SubscriptionTier.PREMIUM_MONTHLY.name)
                .putLong("expiry_ms", now + TimeUnit.DAYS.toMillis(31)).apply()
            is PromoGrant.Quarterly -> prefs.edit()
                .putString("tier", SubscriptionTier.PREMIUM_QUARTERLY.name)
                .putLong("expiry_ms", now + TimeUnit.DAYS.toMillis(92)).apply()
            is PromoGrant.Yearly -> prefs.edit()
                .putString("tier", SubscriptionTier.PREMIUM_YEARLY.name)
                .putLong("expiry_ms", now + TimeUnit.DAYS.toMillis(366)).apply()
            is PromoGrant.Trial14Days -> prefs.edit()
                .putLong("trial_start_ms", now).apply()
        }
        _state.value = loadStateFromPrefs()
    }

    // ── Promo code redemption ─────────────────────────────────────────────

    private val codeRegistry: Map<String, PromoCodeEntry> = mapOf(
        // ── LIFETIME (10) ────────────────────────────────────────────────────
        "808994d6babda94ff365e41ab43d13ad71959d8f3ce36f40e017931c91f487e3" to PromoCodeEntry(PromoGrant.Lifetime, "Lifetime-01"),
        "8dfa583657f04ed7539abdb3244f42ede9d2a2858fad74c56402db7084a48dcd" to PromoCodeEntry(PromoGrant.Lifetime, "Lifetime-02"),
        "990e29504b7c47fb71c90f5b3f8e892a1d47452e29158fdfdc7c340a01bf6236" to PromoCodeEntry(PromoGrant.Lifetime, "Lifetime-03"),
        "0da77f0ebc4d0e36ff8a91153ee2e34f5af5088dd8631e3208669dd716ebb3f7" to PromoCodeEntry(PromoGrant.Lifetime, "Lifetime-04"),
        "18955fa2d184a937badda9f2048b76e8277a7458f9dc10db8b23349c61f61655" to PromoCodeEntry(PromoGrant.Lifetime, "Lifetime-05"),
        "2dc13a5b07fb9e138a6e4ab7ac4d7156cc76162cf1cad539d3d415ea6ef14b10" to PromoCodeEntry(PromoGrant.Lifetime, "Lifetime-06"),
        "fc4b3ab7140bd49d9144e2a59b6bf21a592008dc4eee56aaad0b98ac464afad6" to PromoCodeEntry(PromoGrant.Lifetime, "Lifetime-07"),
        "6b80c08d273d5579e79a1486f3a1870183e6b360557279a944ea5d17badcd250" to PromoCodeEntry(PromoGrant.Lifetime, "Lifetime-08"),
        "8eb8a329d89040efd34660feb75eed8a453f4a58f3202fa1aa7efeb8aaad1bfb" to PromoCodeEntry(PromoGrant.Lifetime, "Lifetime-09"),
        "4a901cc740b80ff497aea3c557a598268f54ffa529789213185943532f00dd5a" to PromoCodeEntry(PromoGrant.Lifetime, "Lifetime-10"),
        // ── YEARLY (20) ──────────────────────────────────────────────────────
        "5143994b4a8eb88726347c0950769294b6000c087faf39acfd8bb13bbe0a3243" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-01"),
        "0e50fdecd2dc83dddb160a67b5da18beaf15726676432a2e3fea6318e6a315d5" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-02"),
        "33e09223cec22ba0b2ba852eab59fe36b32ac805724cc05fd2da846c55baa657" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-03"),
        "4e27f6aa315c86601af348c84a5953f6abedb003ada3584a979a7b2a6b94fe85" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-04"),
        "b15ee0a32f6b056bac5989a55edf268dcf3a929ca0c1d882b1239685e38fa1d0" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-05"),
        "6135aa3357202915c6843b0bcbe2abdc7493a6b8307da963933e090b97f25359" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-06"),
        "1bd9483be61f586c5aef9a89d01962fba3e6a2b253613d106b8f8bc9c74bee5b" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-07"),
        "d2052411fb79517c0da18b7c53f9854c8e940fce9580d7010aaf76794a36a244" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-08"),
        "3c581190b3da3d5f7eee06dc4a366a0d33f29bf64c04608c208839896c2ab54f" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-09"),
        "8e85f3d73f4e0f399f8f29701cbd290625b2befd8c6ac900614a9800af29eeb8" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-10"),
        "056d998f1ebf6d6db37207fa6dc832fba0db3ab5a99e60f944bd8bf9a7afa3d5" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-11"),
        "9a57510b5bdb2f53c084fceac4bf75a681ba0e1149afc44bd879669b6316431b" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-12"),
        "fec11173a32a039fc1f7f0b73e731abbcacec321ce6db7cf5799811038389d2f" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-13"),
        "a64dae31c26044a2917f01c2027af2613507e5285eb98a29470c62a51b3f2dc6" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-14"),
        "fb7b10a1f4be022ce407f3289b4c92e7d9dea77a3bb2e690e7a84132af8c3582" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-15"),
        "0a8e9ed705bba94ae9b53d3d47cad62bc64fc01c3d0fde760b5bb4effee6502e" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-16"),
        "9eada5130a2375da1565479dbd93aa45fbd33fef9f5758ce32d49c4e8afd9021" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-17"),
        "d3df61b1e28d4336c83d275cfd2887a348263755a3428376ab4d7785b65c19e2" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-18"),
        "a81911fece88706633311006cde112a2852b6d00abd081868a1d984d3d75a7b1" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-19"),
        "e567cb9b2c6e904513a8a762267c16db5957508d82d20e58aa009bbfb53e0f0a" to PromoCodeEntry(PromoGrant.Yearly, "Yearly-20"),
        // ── QUARTERLY (20) ───────────────────────────────────────────────────
        "33e340bab94075e5448ad8b6b800ea15db884c7d3533f7a848af7beb83a5224c" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-01"),
        "96fb7831726a94ea0b5f0841cd5e150deb7a05651ee25c30633ab40a87842b32" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-02"),
        "fc889cf16c948a21ad5a604ce130fe4f729eaac48a439dc8b6fbe0de0b1f33d5" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-03"),
        "3b28f7b23ced8ea88afc9c6140cc9b991b4097f77ff11a143829a9e0867b356d" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-04"),
        "48624072440992db06ed3325650ff86bbfc90d287b28d650890a416045715a6f" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-05"),
        "8e74ff1d6a6c82b61069471390878db3da0b8d4f8f44d8f54b266fb7e404707d" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-06"),
        "a9f7b40f2687f50b941c4d6135ac0ea10d2571f754d2f833596a7af83c4412fb" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-07"),
        "1b14badb3eb1af8e07f97046c5b95185bc405f4e3b4a8339e98c033246746015" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-08"),
        "6371cd855689ed37e8b805ed1c8255f5f41954f5d67b5c7353652820d4fbf564" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-09"),
        "d76a6c3db575e9d7875dbcece865268a5bc1758e75214f63f2acdca90759f863" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-10"),
        "9a67f64001ac5486addd33612cc5857431d81d6732700d8982d4b8c344e82c86" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-11"),
        "024236edd7e8cd69a3ed607974c493857189a6dc5a04c590117b470d4a3377b7" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-12"),
        "7cce3db29726ce443b6b36babfde25aae4e5e5ac763a05f788c36b72a4cea353" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-13"),
        "cae8110221c8a67695eafe26ef009336bdd18fef4c502fbacff6d3a8e53a1233" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-14"),
        "e8523a29eebbc53587af32230c42b5a70204ad6d372475a53f703fb671a72f0a" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-15"),
        "7a7f842ca9456e9f430daf2ccd89cd63776471673cfa9033f398c5b7642c0cdb" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-16"),
        "aa6e594fa89a2bc1c4583fdd38bef0da5c5bdec68927082b1c8e1b4a6ab32da0" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-17"),
        "77985a5d654b2fc8305e0cc6c7e67d263e75c16b44c0977e2a1e802af6069efd" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-18"),
        "9e4de70968296a1c7ddc69adfbb33be039783e3f241633cdbeab49bc669436d6" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-19"),
        "e153105ffee5a9c9f586112e4c3fc9e922bfd5529507405157bc0f704a6529c1" to PromoCodeEntry(PromoGrant.Quarterly, "Quarterly-20"),
        // ── MONTHLY (50) ─────────────────────────────────────────────────────
        "3dc4661ab2b56fed5093fc83d2b5add25d7c8df5f8012fe7ae7db43c5d8598ce" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-01"),
        "1170f9ff26c151e504eec9617fe33c63982275c9bc871136fb0a3cd8baada8d6" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-02"),
        "b7657d83dcf552a48484416331d4603bbb4be2342c5b331a87e46095d8844b2b" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-03"),
        "9ac16a3385bb6cee5e8ea75b23f8331577cab18ff41ced98b0c0e8c3d2139283" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-04"),
        "0a822b8af5bcc6cda896f2b4ebd973470b66abe3813dcf71d6f5f8cdaabd45de" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-05"),
        "bb0efd66f733c517d2a0191531367b08f4f89dcb380c506c1e7396be090cb75d" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-06"),
        "9f0a20ac5e80bd970386bcfb73cde6bea89be055e7cec49aaea8d8ec97ce1fa9" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-07"),
        "4974391067bbc7fc0abebfb7b327577ab6ef9d09b0afa4debe431e04f3c69278" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-08"),
        "0e52871c4ab628f670e34d765535d028fc9ef24d6e38e48eca4b13a556096805" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-09"),
        "a454e56bff8e3c99db99b5b5137280d4aa6482da75f6cc43d842a199c6790de2" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-10"),
        "f8b16e433be9326d8ecba4ad7bc261ad519762c529a785f2e71d71e2824e08df" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-11"),
        "01eaa00a787842250fcf94bf483eedf0eb640a48d89079ac514e24a4005a39d2" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-12"),
        "de01d09a91ea2ce31e481fc5e2bc3bf8a3e15d2b2311574af4412d4d3bb4dd47" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-13"),
        "094b8dd05e9ef8d9b112959d50e3ea0b9274c9642868489d6a7f35e2df5c9526" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-14"),
        "0e6c2ee5c3b46c527982575ceb3facda5bd359e5370deaca36a3716df1ff79f7" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-15"),
        "9bdefcbd3a2c33560a3101f2d4a0b872a5a43332bea35a3a840b56433a027f17" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-16"),
        "0dd90876383b595b4a7826494a81874e7d116321e24b90bb037fe04de9318fb7" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-17"),
        "9c3815e40852df38358db13e8ca5fb2c88019017d1f28cdeb8660c373dd8a417" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-18"),
        "596ebd58e29d898b63c4199bc92b4e39dea56f2efdc5720bac05301fbe01c009" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-19"),
        "171953fc73507d96352d62f5e4a9feee9018d67caa8b63950c69559c0a575724" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-20"),
        "a75875ea339ecb3aa9bf208d560a16af0334ba02546934d899355c010f3b8334" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-21"),
        "66e6b9115c457b8b9a84f07fc3c850a1787b768874eb2d8d67a2f08c68fe7ede" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-22"),
        "d8432455127eb568c22f8b3e626a23b6b819ee6bceb5ec9f30b58e46fac32c1f" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-23"),
        "cbbe7fc1498f48059d971c8aa7188041343b10b38beca0ce42780a0a93dd12c8" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-24"),
        "d293993b01f5c5a040bde3f0e2dff55b4a77d69bb717ae0b4f387410b8fb9a1f" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-25"),
        "48e9f9a34db8a595a269c495dac6581a90f9f719aa0d656ae5c3dd4ed6c37a6b" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-26"),
        "12125d718555f6bfc5d3ba1ba11f75551728e4dc9fc58e879afce3554a951569" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-27"),
        "da3c29669d670f52d9603f3e056a9498d7841271b62bdb25f5a6789298cdbbd8" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-28"),
        "4b2a52286f193135e4a9a1ba7356d9832e0728901221afdc92fb23a69f33db16" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-29"),
        "f0efbeb23c2bb790853fcd6b2c70e10c9dd817672cbf7c8a812d486625887085" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-30"),
        "9b28fc28839df1265c9788a41568ff5a3907b03165015173c813626b8c73f285" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-31"),
        "76dc8813337e83ace45860d9267f8ad359232985d0bb175dceea217c09a61134" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-32"),
        "bbf6621d2491e3f449c127596460db1630e0bf6ac56d2e995e7a9a5f304d5e31" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-33"),
        "96985a6499663dd9857fd1d9a420ad784555ce075d946b5834cfeea0918b87f0" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-34"),
        "198264454d0b9a6b400b9cb83672e8551e65f7e1e9ca56e97960c3802819c929" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-35"),
        "f9779973234424caf0e31322224dbb37753752df17f7525344b8a55fd75c499b" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-36"),
        "fc85470176a16bc11f316dd79526700a2f6ef9f7efd261477fdddad1fdc2b0b7" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-37"),
        "70b06908734fd9a4cb24014818d7b44ed8b11fe31e0e458f2ffd9618dd70342c" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-38"),
        "494316ed91017dcf2930c53f93cd10fa38f36cfd088a8187abc3f62d19d358e0" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-39"),
        "d20535b8406106124747cee1c594f16f656046be67981f0c0b73d86d31646733" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-40"),
        "b4806694f2e28d102e4e15284ace60e6c84bca775b2d8b76803d3fb4465ea246" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-41"),
        "789bdf0563b74566c5c7edb30fec7d4575c64d9528c8b2f0059736b046ae778e" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-42"),
        "7f3a33b76a38c9294f3b3238cc12053cb0276d45b0fabd790c10d094fb007480" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-43"),
        "5e8fd67a871b541d44739269445d290f1d17cbb2a7771e005f7d37ebbaa1527d" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-44"),
        "971cae1f5a4c495c1b1b06d7884619e7c5d505f290ce08cd76c73419d27d631f" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-45"),
        "68d6743f3d87a9fbfafa5c3a95f7adddff9a759296f0b4f538fdd5303c187cb6" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-46"),
        "a12ef09dd6acf38bf9242564c378e7d620b692736b0d7dc3500a7e7e2059add1" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-47"),
        "8b4c04912feb9d7ec64b49f000eb4f86549ca84460b7cc14660e285595306709" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-48"),
        "912ff802cd5b48e8067c6aa5f55a22be63dc91d757eee5e80fc0a41f1f693e5c" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-49"),
        "4b113e161fa69dd71b90536421d4b9d1d4656b95ed54a29c6ce611334ce415ab" to PromoCodeEntry(PromoGrant.Monthly, "Monthly-50"),
    )

    private val MAX_EARLYBIRD = 300
    private val EARLYBIRD_WINDOW_DAYS = 90L

    fun redeemPromoCode(rawCode: String): PromoResult {
        val code = rawCode.trim().uppercase()

        if (code == "EARLYBIRD") {
            val alreadyRedeemed = prefs.getBoolean("earlybird_redeemed", false)
            if (alreadyRedeemed) return PromoResult.AlreadyRedeemed

            val count = prefs.getInt("earlybird_count", 0)
            val daysSince = (System.currentTimeMillis() - BillingConfig.PLAY_STORE_LAUNCH_MS) / TimeUnit.DAYS.toMillis(1)
            val sunset = count >= MAX_EARLYBIRD || daysSince >= EARLYBIRD_WINDOW_DAYS

            return if (sunset) {
                grantFromPromo(PromoGrant.Trial14Days)
                PromoResult.EarlybirdSunset
            } else {
                prefs.edit().putInt("earlybird_count", count + 1).putBoolean("earlybird_redeemed", true).apply()
                grantFromPromo(PromoGrant.Lifetime)
                PromoResult.EarlybirdLifetime
            }
        }

        val hash = sha256(code)
        val redeemed = prefs.getStringSet("redeemed_codes", emptySet()) ?: emptySet()
        if (hash in redeemed) return PromoResult.AlreadyRedeemed
        val entry = codeRegistry[hash] ?: return PromoResult.Invalid
        prefs.edit().putStringSet("redeemed_codes", redeemed + hash).apply()
        grantFromPromo(entry.grant)
        return PromoResult.Success(entry.grant, entry.label)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
