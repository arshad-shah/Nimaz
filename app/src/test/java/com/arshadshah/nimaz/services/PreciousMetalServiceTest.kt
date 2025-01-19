package com.arshadshah.nimaz.services

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

class PreciousMetalServiceTest {
    private lateinit var service: PreciousMetalService
    private lateinit var mockEngine: MockEngine

    private val sampleGoldResponse = """
        {
          "timestamp": 1736631660,
          "metal": "XAU",
          "currency": "USD",
          "exchange": "FOREXCOM",
          "symbol": "FOREXCOM:XAUUSD",
          "prev_close_price": 2670.39,
          "open_price": 2670.39,
          "low_price": 2663.98,
          "high_price": 2698.055,
          "open_time": 1736467200,
          "price": 2689.98,
          "ch": 19.59,
          "chp": 0.73,
          "ask": 2690.84,
          "bid": 2689.12,
          "price_gram_24k": 86.4849,
          "price_gram_22k": 79.2778,
          "price_gram_21k": 75.6743,
          "price_gram_20k": 72.0707,
          "price_gram_18k": 64.8636,
          "price_gram_16k": 57.6566,
          "price_gram_14k": 50.4495,
          "price_gram_10k": 36.0354
        }
    """.trimIndent()

    private val sampleSilverResponse = """
        {
          "timestamp": 1736631672,
          "metal": "XAG",
          "currency": "USD",
          "exchange": "FOREXCOM",
          "symbol": "FOREXCOM:XAGUSD",
          "prev_close_price": 30.129,
          "open_price": 30.129,
          "low_price": 29.964,
          "high_price": 30.67,
          "open_time": 1736467200,
          "price": 30.407,
          "ch": 0.278,
          "chp": 0.92,
          "ask": 30.431,
          "bid": 30.383,
          "price_gram_24k": 0.9776,
          "price_gram_22k": 0.8961,
          "price_gram_21k": 0.8554,
          "price_gram_20k": 0.8147,
          "price_gram_18k": 0.7332,
          "price_gram_16k": 0.6517,
          "price_gram_14k": 0.5703,
          "price_gram_10k": 0.4073
        }
    """.trimIndent()

    @Before
    fun setup() {
        mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/XAU/USD" -> respond(
                    content = sampleGoldResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                "/XAG/USD" -> respond(
                    content = sampleSilverResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> error("Unhandled ${request.url.encodedPath}")
            }
        }

        service = PreciousMetalService()
    }

    @After
    fun tearDown() {
        service.cleanup()
    }

    @Test
    fun `test successful nisab thresholds calculation`() = runBlocking {
        val thresholds = service.getNisabThresholds(Locale.US)

        assertNotNull(thresholds)
        assertTrue(thresholds.goldNisab > 0)
        assertTrue(thresholds.silverNisab > 0)
        assertEquals("USD", thresholds.currencyInfo.code)
        assertEquals("US$", thresholds.currencyInfo.symbol)
    }

    @Test
    fun `test cache functionality`() = runBlocking {
        // First call should hit the API
        val firstCall = service.getNisabThresholdsWithCache()

        // Second call within cache duration should return same values
        val secondCall = service.getNisabThresholdsWithCache()

        assertEquals(firstCall.goldNisab, secondCall.goldNisab, 0.01)
        assertEquals(firstCall.silverNisab, secondCall.silverNisab, 0.01)
        assertTrue(secondCall.timestamp - firstCall.timestamp < 1000) // Should be almost instant
    }

    @Test
    fun `test different locale currency handling`() = runBlocking {
        val japaneseLocale = Locale("ja", "JP")
        val thresholds = service.getNisabThresholds(japaneseLocale)

        assertEquals("JPY", thresholds.currencyInfo.code)
        assertEquals("JP¥", thresholds.currencyInfo.symbol)
    }

    @Test
    fun `test api failure fallback values`() = runBlocking {
        // Configure mock engine to simulate API failure
        mockEngine = MockEngine { request ->
            respond(
                content = "",
                status = HttpStatusCode.InternalServerError
            )
        }

        val thresholds = service.getNisabThresholds()

        assertEquals(PreciousMetalService.DEFAULT_GOLD_NISAB, thresholds.goldNisab, 0.01)
        assertEquals(PreciousMetalService.DEFAULT_SILVER_NISAB, thresholds.silverNisab, 0.01)
    }

    @Test
    fun `test correct nisab calculation based on metal weights`() = runBlocking {
        val thresholds = service.getNisabThresholds()

        // Gold nisab calculation check
        val expectedGoldNisab = 64.8636 * PreciousMetalService.GOLD_NISAB_GRAMS
        assertEquals(expectedGoldNisab, thresholds.goldNisab, 0.01)

        // Silver nisab calculation check
        val expectedSilverNisab = 0.9776 * PreciousMetalService.SILVER_NISAB_GRAMS
        assertEquals(expectedSilverNisab, thresholds.silverNisab, 0.01)
    }

    @Test
    fun `test invalid locale fallback to USD`() = runBlocking {
        val invalidLocale = Locale("xx", "XX")
        val thresholds = service.getNisabThresholds(invalidLocale)

        assertEquals("USD", thresholds.currencyInfo.code)
        assertEquals("$", thresholds.currencyInfo.symbol)
    }

    @Test
    fun `test cache invalidation on currency change`() = runBlocking {
        // First call with USD
        val usdThresholds = service.getNisabThresholdsWithCache(Locale.US)

        // Second call with different locale should invalidate cache
        val jpyThresholds = service.getNisabThresholdsWithCache(Locale("ja", "JP"))

        assertTrue(usdThresholds.currencyInfo.code != jpyThresholds.currencyInfo.code)
        assertTrue(usdThresholds.timestamp != jpyThresholds.timestamp)
    }
}