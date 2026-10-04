package com.kushal.mealapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.android.billingclient.api.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProBillingActivity : ComponentActivity(), PurchasesUpdatedListener {

    private lateinit var billingClient: BillingClient
    private val isBillingConnected = mutableStateOf(false)
    private val playConsoleProducts = mutableStateOf<Map<String, ProductDetails>>(emptyMap())
    private var pendingTransaction: PendingTransaction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val name = intent.getStringExtra("name") ?: ""
        val item = intent.getStringExtra("item") ?: ""
        val meal = intent.getStringExtra("meal") ?: ""
        val expenditure = intent.getStringExtra("expenditure") ?: ""
        val price = intent.getStringExtra("price") ?: ""
        val date = intent.getStringExtra("date") ?: ""
        val depositTo = intent.getStringExtra("depositTo") ?: ""
        val hasPendingTransaction = intent.getBooleanExtra("hasPendingTransaction", false)

        if (hasPendingTransaction) {
            pendingTransaction = PendingTransaction(name, item, meal, expenditure, price, date, depositTo)
        }

        // Initialize Official Google Play Billing Client
        setupPlayBilling()

        setContent {
            MaterialTheme {
                ProBillingScreen(
                    hasPendingTransaction = hasPendingTransaction,
                    pendingDetails = pendingTransaction,
                    isBillingConnected = isBillingConnected.value,
                    playConsoleProducts = playConsoleProducts.value,
                    onSubscribeClick = { plan ->
                        launchPlayStoreBilling(plan)
                    },
                    onRestorePurchases = {
                        restorePlayStorePurchases()
                    },
                    onBack = { finish() },
                    onTransactionCompleted = {
                        setResult(RESULT_OK)
                        finish()
                    }
                )
            }
        }
    }

    // -------------------------------------------------------------
    // Google Play Billing Integration Methods
    // -------------------------------------------------------------
    private fun setupPlayBilling() {
        billingClient = BillingClient.newBuilder(this)
            .setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()

        startBillingConnection()
    }

    private fun startBillingConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("PlayBilling", "Google Play Billing setup successful!")
                    isBillingConnected.value = true
                    queryActivePurchases()
                    queryProductDetailsFromPlayConsole()
                } else {
                    Log.e("PlayBilling", "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w("PlayBilling", "Billing service disconnected. Retrying...")
                isBillingConnected.value = false
            }
        })
    }

    private fun queryProductDetailsFromPlayConsole() {
        // 1. Query Subscription Products (jdb-pro-monthly-01, jdb-pro-yearly-01, pro_monthly, pro_yearly)
        val subsList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("jdb-pro-monthly-01")
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("pro_monthly")
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("jdb-pro-yearly-12")
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("pro_yearly")
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(subsList)
            .build()

        billingClient.queryProductDetailsAsync(subsParams) { billingResult, productDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val list = productDetailsResult.productDetailsList
                Log.d("PlayBilling", "Fetched ${list.size} SUBS products from Play Console")
                val updatedMap = playConsoleProducts.value.toMutableMap()
                list.forEach { updatedMap[it.productId] = it }
                playConsoleProducts.value = updatedMap
            } else {
                Log.e("PlayBilling", "queryProductDetails SUBS failed: ${billingResult.debugMessage}")
            }
        }

        // 2. Query In-App One-Time Products (jdb-pro-lifetime-01, pro_lifetime)
        val inAppList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("jdb-pro-lifetime-01")
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("pro_lifetime")
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("pro_lifetime_purchase")
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(inAppList)
            .build()

        billingClient.queryProductDetailsAsync(inAppParams) { billingResult, productDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val list = productDetailsResult.productDetailsList
                Log.d("PlayBilling", "Fetched ${list.size} INAPP products from Play Console")
                val updatedMap = playConsoleProducts.value.toMutableMap()
                list.forEach { updatedMap[it.productId] = it }
                playConsoleProducts.value = updatedMap
            } else {
                Log.e("PlayBilling", "queryProductDetails INAPP failed: ${billingResult.debugMessage}")
            }
        }
    }

    private fun queryActivePurchases() {
        val subParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(subParams) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
        }

        val inAppParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(inAppParams) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
        }
    }

    private fun findProductDetails(plan: BillingPlan): ProductDetails? {
        val candidateIds = when (plan) {
            BillingPlan.MONTHLY -> listOf("jdb-pro-monthly-01", "pro_monthly", "jdb-pro-monthly")
            BillingPlan.YEARLY -> listOf("jdb-pro-yearly-01", "pro_yearly", "jdb-pro-yearly")
            BillingPlan.LIFETIME -> listOf("jdb-pro-lifetime-01", "pro_lifetime", "pro_lifetime_purchase", "jdb-pro-lifetime")
        }

        return candidateIds.firstNotNullOfOrNull { id -> playConsoleProducts.value[id] }
    }

    private fun launchPlayStoreBilling(plan: BillingPlan) {
        val productDetails = findProductDetails(plan)

        if (productDetails != null) {
            // Find offerToken from backwards-compatible base plan (where offerId is null) or first offer
            val offerToken = if (productDetails.productType == BillingClient.ProductType.SUBS) {
                val offerList = productDetails.subscriptionOfferDetails
                offerList?.firstOrNull { it.offerId == null }?.offerToken
                    ?: offerList?.firstOrNull()?.offerToken
            } else {
                null
            }

            val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)

            if (!offerToken.isNullOrBlank()) {
                productDetailsParamsBuilder.setOfferToken(offerToken)
            }

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParamsBuilder.build()))
                .build()

            val result = billingClient.launchBillingFlow(this, billingFlowParams)
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Toast.makeText(this, "Play Store Billing launch failed: ${result.debugMessage}", Toast.LENGTH_LONG).show()
            }
        } else {
            val requestedId = when (plan) {
                BillingPlan.MONTHLY -> "jdb-pro-monthly-01"
                BillingPlan.YEARLY -> "jdb-pro-yearly-01"
                BillingPlan.LIFETIME -> "jdb-pro-lifetime-01"
            }
            Log.w("PlayBilling", "Product details for $requestedId not found on Play Console.")
            Toast.makeText(
                this,
                "Product '$requestedId' not found on Play Console. Ensure product is published & active.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Toast.makeText(this, "Purchase canceled by user", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Billing error: ${billingResult.debugMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d("PlayBilling", "Purchase acknowledged with Google Play!")
                        activateProInSession()
                    }
                }
            } else {
                activateProInSession()
            }
        }
    }

    private fun restorePlayStorePurchases() {
        if (!billingClient.isReady) {
            startBillingConnection()
            Toast.makeText(this, "Connecting to Google Play...", Toast.LENGTH_SHORT).show()
            return
        }

        queryActivePurchases()
        Toast.makeText(this, "Querying active purchases from Google Play...", Toast.LENGTH_SHORT).show()
    }

    private fun activateProInSession() {
        getSharedPreferences("SessionPrefs", MODE_PRIVATE).edit().putBoolean("isProVersion", true).commit()
        runOnUiThread {
            Toast.makeText(this, "Google Play Purchase Verified! PRO Activated 🌟", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::billingClient.isInitialized) {
            billingClient.endConnection()
        }
    }
}

data class PendingTransaction(
    val name: String,
    val item: String,
    val meal: String,
    val expenditure: String,
    val price: String,
    val date: String,
    val depositTo: String
)

enum class BillingPlan(val title: String, val price: String, val period: String, val badge: String?) {
    MONTHLY("Monthly Pro", "₹99", "/ month", null),
    YEARLY("Yearly Pro", "₹499", "/ year", "BEST VALUE - SAVE 58%"),
    LIFETIME("Lifetime Pro", "₹999", "one-time", "UNLIMITED ACCESS")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProBillingScreen(
    hasPendingTransaction: Boolean,
    pendingDetails: PendingTransaction?,
    isBillingConnected: Boolean,
    playConsoleProducts: Map<String, ProductDetails>,
    onSubscribeClick: (BillingPlan) -> Unit,
    onRestorePurchases: () -> Unit,
    onBack: () -> Unit,
    onTransactionCompleted: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPrefs = remember { context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE) }

    var isProUser by remember { mutableStateOf(sharedPrefs.getBoolean("isProVersion", false)) }
    var selectedPlan by remember { mutableStateOf(BillingPlan.YEARLY) }
    var isSubmittingTransaction by remember { mutableStateOf(false) }

    fun submitPendingTransaction(onDone: () -> Unit) {
        if (pendingDetails == null) {
            onDone()
            return
        }

        val token = sharedPrefs.getString("sessionId", null) ?: sharedPrefs.getString("sessionId1", null)
        if (token.isNullOrEmpty()) {
            Toast.makeText(context, "Session token not found. Please log in.", Toast.LENGTH_SHORT).show()
            onDone()
            return
        }

        isSubmittingTransaction = true
        val apiService = RetrofitInstance.api

        apiService.submitForm(
            name = pendingDetails.name,
            item = pendingDetails.item,
            meal = pendingDetails.meal,
            expenditure = pendingDetails.expenditure,
            price = pendingDetails.price,
            date = pendingDetails.date,
            depositTo = pendingDetails.depositTo,
            token = token
        ).enqueue(object : Callback<JSONObject> {
            override fun onResponse(call: Call<JSONObject>, response: Response<JSONObject>) {
                isSubmittingTransaction = false
                if (response.isSuccessful) {
                    Toast.makeText(context, "Transaction submitted successfully! ✅", Toast.LENGTH_LONG).show()
                    onDone()
                } else {
                    Toast.makeText(context, "Error submitting transaction: ${response.message()}", Toast.LENGTH_LONG).show()
                    onDone()
                }
            }

            override fun onFailure(call: Call<JSONObject>, t: Throwable) {
                isSubmittingTransaction = false
                Toast.makeText(context, "Submission Network Error: ${t.message}", Toast.LENGTH_LONG).show()
                onDone()
            }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PRO Membership", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        )
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(60.dp),
                        shape = CircleShape,
                        color = Color(0xFFFFD700)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "Crown",
                                tint = Color(0xFF5D4037),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isProUser) "You are a PRO Member 🌟" else "Upgrade to Journal DayBook PRO",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (isProUser) "Enjoy unlimited access, cloud backups & reports" else "Unlock unlimited transactions, cloud backup & member reports",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Google Play Connection Status Indicator
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isBillingConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isBillingConnected) Icons.Default.CheckCircle else Icons.Default.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isBillingConnected) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBillingConnected) "Google Play Store Connected" else "Connecting to Google Play Store...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pending Transaction Card
            if (hasPendingTransaction && pendingDetails != null) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Pending Transaction",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "READY",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${pendingDetails.name} • ${pendingDetails.item} (${pendingDetails.meal.ifEmpty { "N/A" }})",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Amount: ₹${pendingDetails.price} | Date: ${pendingDetails.date}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Features List
            Text(
                text = "Included with PRO:",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            val features = listOf(
                "⚡ Unlimited Transaction Submissions",
                "👥 Unlimited Group Members & Personal Accounts",
                "☁️ Automatic Google Drive Cloud Sync",
                "📄 Member Report Exports & Invoices",
                "📊 Access to PRO Calculator & Analytics"
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                features.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Plan Selector Cards
            Text(
                text = "Choose Your Play Store Plan:",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            BillingPlan.values().forEach { plan ->
                val isSelected = selectedPlan == plan
                val candidateIds = when (plan) {
                    BillingPlan.MONTHLY -> listOf("jdb-pro-monthly-01", "pro_monthly", "jdb-pro-monthly")
                    BillingPlan.YEARLY -> listOf("jdb-pro-yearly-01", "pro_yearly", "jdb-pro-yearly")
                    BillingPlan.LIFETIME -> listOf("jdb-pro-lifetime-01", "pro_lifetime", "pro_lifetime_purchase", "jdb-pro-lifetime")
                }
                val playDetails = candidateIds.firstNotNullOfOrNull { id -> playConsoleProducts[id] }
                val basePlanOffer = playDetails?.subscriptionOfferDetails?.firstOrNull { it.offerId == null }
                    ?: playDetails?.subscriptionOfferDetails?.firstOrNull()

                val displayPrice = playDetails?.oneTimePurchaseOfferDetails?.formattedPrice
                    ?: basePlanOffer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                    ?: plan.price

                OutlinedCard(
                    onClick = { selectedPlan = plan },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = plan.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                if (plan.badge != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = plan.badge,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "$displayPrice ${plan.period}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedPlan = plan }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            var isAgreedToTerms by remember { mutableStateOf(false) }

            // Privacy Policy & Terms Agreement Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (!isAgreedToTerms) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isAgreedToTerms,
                        onCheckedChange = { isAgreedToTerms = it }
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    val annotatedString = buildAnnotatedString {
                        append("I agree to the ")

                        pushStringAnnotation(tag = "PRIVACY", annotation = "https://legalcount.in/meal/privacy.html")
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) {
                            append("Privacy Policy")
                        }
                        pop()

                        append(" & ")

                        pushStringAnnotation(tag = "TERMS", annotation = "https://legalcount.in/meal/terms.html")
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) {
                            append("Terms and Conditions")
                        }
                        pop()
                        append(".")
                    }

                    ClickableText(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        onClick = { offset ->
                            annotatedString.getStringAnnotations(offset, offset).firstOrNull()?.let { annotation ->
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot open browser link: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Play Store Purchase Action Button
            Button(
                onClick = {
                    if (!isAgreedToTerms) {
                        Toast.makeText(
                            context,
                            "Please check the box to agree to the Privacy Policy & Terms before purchasing.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@Button
                    }
                    onSubscribeClick(selectedPlan)
                },
                enabled = !isSubmittingTransaction,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isSubmittingTransaction) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Buy via Google Play (${selectedPlan.price})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            if (hasPendingTransaction) {
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        submitPendingTransaction {
                            onTransactionCompleted()
                        }
                    },
                    enabled = !isSubmittingTransaction,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Proceed with Free Submission")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            var showPromoDialog by remember { mutableStateOf(false) }
            var promoCodeInput by remember { mutableStateOf("") }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onRestorePurchases
                ) {
                    Text("Restore Play Store Purchases", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                TextButton(
                    onClick = { showPromoDialog = true }
                ) {
                    Text("Redeem Code 🎁", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }

            if (showPromoDialog) {
                AlertDialog(
                    onDismissRequest = { showPromoDialog = false },
                    title = { Text("Redeem Code 🎁") },
                    text = {
                        Column {
                            Text("Enter a promo code or Play Store gift code to activate PRO membership:")
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = promoCodeInput,
                                onValueChange = { promoCodeInput = it.uppercase() },
                                label = { Text("Promo / Gift Code") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val code = promoCodeInput.trim().uppercase()
                                if (code.isNotBlank()) {
                                    // Verify promo code strictly through Google Play Store official backend
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/redeem?code=$code"))
                                        context.startActivity(intent)
                                        showPromoDialog = false
                                        Toast.makeText(context, "Verifying code with Google Play Store...", Toast.LENGTH_LONG).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open Play Store: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Please enter a valid promo code.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Redeem on Play Store")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPromoDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
