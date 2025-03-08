package com.arshadshah.nimaz.services

import com.arshadshah.nimaz.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

@Serializable
data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val exchangeRate: Double = 1.0 // Default to USD rate
)

enum class NisabType {
    GOLD,
    SILVER
}

@Serializable
data class GoldApiResponse(
    val timestamp: Long,
    val metal: String,
    val currency: String,
    val exchange: String,
    val symbol: String,
    val prev_close_price: Double,
    val open_price: Double,
    val low_price: Double,
    val high_price: Double,
    val open_time: Long,
    val price: Double,
    val ch: Double,
    val chp: Double,
    val ask: Double,
    val bid: Double,
    val price_gram_24k: Double,
    val price_gram_22k: Double,
    val price_gram_21k: Double,
    val price_gram_20k: Double,
    val price_gram_18k: Double,
    val price_gram_16k: Double,
    val price_gram_14k: Double,
    val price_gram_10k: Double
)

class PreciousMetalService @Inject constructor() {
    companion object {
        const val GOLD_NISAB_GRAMS = 87.48
        const val SILVER_NISAB_GRAMS = 612.36
        const val DEFAULT_GOLD_NISAB = 5674.267
        const val DEFAULT_SILVER_NISAB = 598.64
        private const val BASE_URL = "https://www.goldapi.io/api/"
        private const val API_KEY = BuildConfig.METAL_API_KEY
    }

    @Serializable
    data class NisabThresholds(
        val goldNisab: Double,
        val silverNisab: Double,
        val timestamp: Long = System.currentTimeMillis(),
        val currencyInfo: CurrencyInfo = CurrencyInfo("USD", "$")
    )

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        install(DefaultRequest) {
            header("x-access-token", API_KEY)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 15000
        }

    }

    private suspend fun getGoldPrice(): Result<GoldApiResponse> {
        return try {
            val response: GoldApiResponse = client.get("${BASE_URL}XAU/USD").body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getSilverPrice(): Result<GoldApiResponse> {
        return try {
            val response: GoldApiResponse = client.get("${BASE_URL}/XAG/USD").body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Basic caching implementation
    private var cachedThresholds: NisabThresholds? = null
    private val CACHE_DURATION = 1000 * 60 * 60 * 24 // 24 hours

    private fun getCurrencyInfoForLocale(locale: Locale = Locale.getDefault()): CurrencyInfo {
        return try {
            val currency = Currency.getInstance(locale)
            CurrencyInfo(
                code = currency.currencyCode,
                symbol = currency.symbol
            )
        } catch (e: Exception) {
            // Default to USD if currency cannot be determined
            CurrencyInfo("USD", "$")
        }
    }

    suspend fun getNisabThresholdsWithCache(locale: Locale = Locale.getDefault()): NisabThresholds {
        val currentTime = System.currentTimeMillis()
        val currentCurrency = getCurrencyInfoForLocale(locale)

        // Return cached value if it's still valid and currency matches
        cachedThresholds?.let { cached ->
            if (currentTime - cached.timestamp < CACHE_DURATION &&
                cached.currencyInfo.code == currentCurrency.code
            ) {
                return cached
            }
        }

        // Fetch new values
        val thresholds = getNisabThresholds(locale)
        cachedThresholds = thresholds
        return thresholds
    }

    suspend fun getNisabThresholds(locale: Locale = Locale.getDefault()): NisabThresholds {
        val currencyInfo = getCurrencyInfoForLocale(locale)
        val goldResponse = getGoldPrice()
        val silverResponse = getSilverPrice()

        val goldNisab = goldResponse.fold(
            onSuccess = { response ->
                response.price_gram_18k * GOLD_NISAB_GRAMS
            },
            onFailure = {
                DEFAULT_GOLD_NISAB
            }
        )

        val silverNisab = silverResponse.fold(
            onSuccess = { response ->
                response.price_gram_24k * SILVER_NISAB_GRAMS
            },
            onFailure = {
                DEFAULT_SILVER_NISAB
            }
        )
        return NisabThresholds(
            goldNisab = goldNisab,
            silverNisab = silverNisab,
            currencyInfo = currencyInfo
        )
    }

    fun cleanup() {
        client.close()
    }
}