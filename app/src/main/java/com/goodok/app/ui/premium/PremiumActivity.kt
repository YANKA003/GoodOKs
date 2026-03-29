package com.goodok.app.ui.premium

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.ProductDetails
import com.goodok.app.R
import com.goodok.app.billing.BillingManager
import com.goodok.app.data.Repository
import com.goodok.app.databinding.ActivityPremiumBinding
import com.goodok.app.util.LanguageHelper
import kotlinx.coroutines.launch

class PremiumActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPremiumBinding
    private lateinit var billingManager: BillingManager
    private lateinit var repository: Repository
    private var products: List<ProductDetails> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        applyLanguage()
        applyTheme()
        super.onCreate(savedInstanceState)

        binding = ActivityPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)
        billingManager = BillingManager(this)

        setupToolbar()
        setupBilling()
        setupViews()
    }

    private fun applyLanguage() {
        try {
            val repo = Repository(applicationContext)
            LanguageHelper.setLanguage(this, repo.language)
        } catch (e: Exception) {
            LanguageHelper.setLanguage(this, "ru")
        }
    }

    private fun applyTheme() {
        try {
            val repo = Repository(applicationContext)
            when (repo.theme) {
                0 -> setTheme(R.style.Theme_GoodOK_Classic)
                1 -> setTheme(R.style.Theme_GoodOK_Modern)
                2 -> setTheme(R.style.Theme_GoodOK_Neon)
                3 -> setTheme(R.style.Theme_GoodOK_Childish)
            }
        } catch (e: Exception) {
            setTheme(R.style.Theme_GoodOK_Classic)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.premium)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupBilling() {
        billingManager.startConnection {
            billingManager.queryAllProducts { productList ->
                products = productList
                updateProductUI()
            }
        }

        lifecycleScope.launch {
            billingManager.purchaseState.collect { state ->
                when (state) {
                    is BillingManager.PurchaseState.Success -> {
                        Toast.makeText(
                            this@PremiumActivity,
                            "Purchase successful!",
                            Toast.LENGTH_SHORT
                        ).show()
                        updatePremiumUI()
                    }
                    is BillingManager.PurchaseState.Error -> {
                        Toast.makeText(
                            this@PremiumActivity,
                            "Error: ${state.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun setupViews() {
        binding.btnBasicMonthly.setOnClickListener {
            purchaseProduct(BillingManager.SKU_BASIC_MONTHLY)
        }

        binding.btnBasicForever.setOnClickListener {
            purchaseProduct(BillingManager.SKU_BASIC_FOREVER)
        }

        binding.btnGoodplanMonthly.setOnClickListener {
            purchaseProduct(BillingManager.SKU_GOODPLAN_MONTHLY)
        }

        binding.btnGoodplanForever.setOnClickListener {
            purchaseProduct(BillingManager.SKU_GOODPLAN_FOREVER)
        }

        binding.btnRestore.setOnClickListener {
            billingManager.restorePurchases {
                Toast.makeText(this, "Purchases restored", Toast.LENGTH_SHORT).show()
            }
        }

        updatePremiumUI()
    }

    private fun purchaseProduct(productId: String) {
        val product = billingManager.getProductDetails(productId) ?: return

        val offerToken = if (product.productType == com.android.billingclient.api.BillingClient.ProductType.SUBS) {
            product.subscriptionOfferDetails?.firstOrNull()?.offerToken
        } else {
            null
        }

        billingManager.purchase(this, product, offerToken)
    }

    private fun updateProductUI() {
        products.forEach { product ->
            val price = product.oneTimePurchaseOfferDetails?.formattedPrice
                ?: product.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                ?: ""

            when (product.productId) {
                BillingManager.SKU_BASIC_MONTHLY -> binding.btnBasicMonthly.text = "BASIC Monthly - $price"
                BillingManager.SKU_BASIC_FOREVER -> binding.btnBasicForever.text = "BASIC Forever - $price"
                BillingManager.SKU_GOODPLAN_MONTHLY -> binding.btnGoodplanMonthly.text = "GOODPLAN Monthly - $price"
                BillingManager.SKU_GOODPLAN_FOREVER -> binding.btnGoodplanForever.text = "GOODPLAN Forever - $price"
            }
        }
    }

    private fun updatePremiumUI() {
        if (repository.isPremium) {
            binding.tvCurrentPlan.text = "Current Plan: ${repository.premiumType}"
            binding.tvCurrentPlan.visibility = android.view.View.VISIBLE
        } else {
            binding.tvCurrentPlan.visibility = android.view.View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.endConnection()
    }
}
