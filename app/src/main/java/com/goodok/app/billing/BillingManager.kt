package com.goodok.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.goodok.app.data.local.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    private val prefsManager = PreferencesManager(context)
    private var billingClient: BillingClient? = null

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState

    companion object {
        const val SKU_BASIC_MONTHLY = "goodok_basic_monthly"
        const val SKU_BASIC_FOREVER = "goodok_basic_forever"
        const val SKU_GOODPLAN_MONTHLY = "goodok_goodplan_monthly"
        const val SKU_GOODPLAN_FOREVER = "goodok_goodplan_forever"
    }

    sealed class PurchaseState {
        object Idle : PurchaseState()
        object Loading : PurchaseState()
        data class Success(val sku: String) : PurchaseState()
        data class Error(val message: String) : PurchaseState()
    }

    private val cachedProducts = mutableMapOf<String, ProductDetails>()

    init {
        initBillingClient()
    }

    private fun initBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
    }

    fun startConnection(onConnected: () -> Unit) {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    onConnected()
                }
            }

            override fun onBillingServiceDisconnected() {
                startConnection(onConnected)
            }
        })
    }

    fun queryAllProducts(onResult: ((List<ProductDetails>) -> Unit)? = null) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_BASIC_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_BASIC_FOREVER)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_GOODPLAN_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_GOODPLAN_FOREVER)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { _, productDetailsList ->
            productDetailsList.forEach { details ->
                cachedProducts[details.productId] = details
            }
            onResult?.invoke(productDetailsList)
        }
    }

    fun getProductDetails(productId: String): ProductDetails? {
        return cachedProducts[productId]
    }

    fun purchase(activity: Activity, productDetails: ProductDetails, offerToken: String? = null) {
        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)

        if (offerToken != null) {
            productDetailsParamsBuilder.setOfferToken(offerToken)
        }

        val productDetailsParams = productDetailsParamsBuilder.build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        billingClient?.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseState.value = PurchaseState.Error("Purchase cancelled")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                _purchaseState.value = PurchaseState.Success("already_owned")
            }
            else -> {
                _purchaseState.value = PurchaseState.Error(billingResult.debugMessage)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val sku = purchase.products.firstOrNull() ?: ""
            when (sku) {
                SKU_BASIC_MONTHLY, SKU_BASIC_FOREVER -> grantPremium("BASIC")
                SKU_GOODPLAN_MONTHLY, SKU_GOODPLAN_FOREVER -> grantPremium("GOODPLAN")
            }

            if (!purchase.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient?.acknowledgePurchase(params) {
                    _purchaseState.value = PurchaseState.Success(sku)
                }
            } else {
                _purchaseState.value = PurchaseState.Success(sku)
            }
        }
    }

    private fun grantPremium(type: String) {
        prefsManager.isPremium = true
        prefsManager.premiumType = type
        prefsManager.premiumExpiry = if (type.contains("FOREVER")) {
            Long.MAX_VALUE
        } else {
            System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
        }
    }

    fun restorePurchases(onRestored: (List<Purchase>) -> Unit) {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { _, purchasesList ->
            purchasesList.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    handlePurchase(purchase)
                }
            }
            onRestored(purchasesList)
        }
    }

    fun endConnection() {
        billingClient?.endConnection()
    }
}
