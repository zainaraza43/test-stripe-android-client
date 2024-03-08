package com.zain.stripeclient

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val paymentSheet = rememberPaymentSheet(::onPaymentSheetResult)
            val context = LocalContext.current
            var customerConfig by remember { mutableStateOf<PaymentSheet.CustomerConfiguration?>(null) }
            var paymentIntentClientSecret by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(context) {
                "http://192.168.2.198:5000/payment-sheet".httpPost().responseJson { _, _, result ->
                    if (result is Result.Success) {
                        val responseJson = result.get().obj()
                        Log.i(TAG, "result=$responseJson")
                        paymentIntentClientSecret = responseJson.getString("paymentIntent")
                        customerConfig = PaymentSheet.CustomerConfiguration(
                            responseJson.getString("customer"),
                            responseJson.getString("ephemeralKey")
                        )
                        val publishableKey = responseJson.getString("publishableKey")
                        PaymentConfiguration.init(context, publishableKey)
                    }
                }
            }

            Button(
                onClick = {
                    val currentConfig = customerConfig
                    val currentClientSecret = paymentIntentClientSecret

                    if (currentConfig != null && currentClientSecret != null) {
                        presentPaymentSheet(paymentSheet, currentConfig, currentClientSecret)
                    }
                }
            ) {
                Text("Checkout")
            }
        }
    }
}

private fun presentPaymentSheet(
    paymentSheet: PaymentSheet,
    customerConfig: PaymentSheet.CustomerConfiguration,
    paymentIntentClientSecret: String
) {
    paymentSheet.presentWithPaymentIntent(
        paymentIntentClientSecret,
        PaymentSheet.Configuration(
            merchantDisplayName = "My merchant name",
            customer = customerConfig,
            // Set `allowsDelayedPaymentMethods` to true if your business handles
            // delayed notification payment methods like US bank accounts.
            allowsDelayedPaymentMethods = true
        )
    )
}

private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
    when(paymentSheetResult) {
        is PaymentSheetResult.Canceled -> {
            print("Canceled")
        }
        is PaymentSheetResult.Failed -> {
            print("Error: ${paymentSheetResult.error}")
        }
        is PaymentSheetResult.Completed -> {
            // Display for example, an order confirmation screen
            print("Completed")
        }
    }
}