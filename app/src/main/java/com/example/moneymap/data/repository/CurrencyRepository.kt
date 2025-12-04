package com.example.moneymap.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class CurrencyRates(
    val base: String,
    val rates: Map<String, Double>,
    val fetchedAtMillis: Long
)

@Singleton
class CurrencyRepository @Inject constructor() {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val _rates = MutableStateFlow<CurrencyRates?>(null)
    val rates: StateFlow<CurrencyRates?> = _rates.asStateFlow()

    /**
     * Fetch the latest FX rates if we don't have any yet or if the cached
     * value is older than [maxAgeMinutes].
     *
     * The implementation uses the open.er-api.com public endpoint which does
     * not require an API key. You can swap this out for any other provider.
     */
    suspend fun refreshRatesIfStale(
        base: String = DEFAULT_BASE_CURRENCY,
        maxAgeMinutes: Long = 60
    ) {
        val current = _rates.value
        val now = System.currentTimeMillis()
        if (current != null &&
            current.base.equals(base, ignoreCase = true) &&
            (now - current.fetchedAtMillis) < maxAgeMinutes * 60_000
        ) {
            return
        }

        fetchLatestRates(base)
    }

    private suspend fun fetchLatestRates(base: String) {
        withContext(ioDispatcher) {
            try {
                val url = URL("https://open.er-api.com/v6/latest/$base")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    connection.disconnect()
                    return@withContext
                }

                val body = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val json = JSONObject(body)
                val result = json.optString("result")
                if (!result.equals("success", ignoreCase = true)) {
                    return@withContext
                }

                val apiBase = json.optString("base_code", base)
                val ratesJson = json.getJSONObject("rates")
                val ratesMap = mutableMapOf<String, Double>()
                val keys = ratesJson.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    ratesMap[key] = ratesJson.getDouble(key)
                }

                _rates.value = CurrencyRates(
                    base = apiBase.uppercase(),
                    rates = ratesMap,
                    fetchedAtMillis = System.currentTimeMillis()
                )
            } catch (_: Exception) {
                // Ignore failures and keep existing cached rates if any.
            }
        }
    }

    /**
     * Convert [amount] from [fromCurrency] into [toCurrency] using the
     * currently cached FX table. If rates are unavailable or the currencies
     * are unknown, the original [amount] is returned.
     */
    fun convert(
        amount: Double,
        fromCurrency: String,
        toCurrency: String
    ): Double {
        if (amount == 0.0) return 0.0
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) return amount

        val current = _rates.value ?: return amount
        val base = current.base
        val rates = current.rates

        val fromCode = fromCurrency.uppercase()
        val toCode = toCurrency.uppercase()

        // If we don't have one of the currencies, fall back to the raw amount.
        val fromRate = rates[fromCode] ?: return amount
        val toRate = rates[toCode] ?: return amount

        return if (base.equals(fromCode, ignoreCase = true)) {
            // base -> target
            amount * toRate
        } else if (base.equals(toCode, ignoreCase = true)) {
            // source -> base
            if (fromRate == 0.0) return amount // Prevent division by zero
            amount / fromRate
        } else {
            // source -> base -> target
            if (fromRate == 0.0) return amount // Prevent division by zero
            val amountInBase = amount / fromRate
            amountInBase * toRate
        }
    }

    companion object {
        private const val DEFAULT_BASE_CURRENCY = "USD"
    }
}


