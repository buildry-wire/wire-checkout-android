package mn.wire.checkout

import java.net.HttpURLConnection
import java.net.URL

/**
 * The HTTP seam used by [CheckoutPoller] to read the status endpoint. Implementations must be
 * safe to call from a background thread. Kept as an interface so unit tests can inject a fake.
 */
public fun interface StatusHttpClient {
    /**
     * Perform a `GET` against [url] and return the decoded session status.
     *
     * @throws WireCheckoutException.Network on a connection/timeout failure.
     * @throws WireCheckoutException.HttpStatus on a non-2xx response.
     * @throws WireCheckoutException.Decoding on an unparseable body.
     */
    public fun fetchStatus(url: String): CheckoutSessionStatus
}

/**
 * Default [StatusHttpClient] backed by [HttpURLConnection] — no third-party HTTP dependency. The
 * status endpoint is unauthenticated (no secret key is ever attached).
 */
public class HttpUrlConnectionStatusClient(
    private val connectTimeoutMillis: Int = 10_000,
    private val readTimeoutMillis: Int = 10_000,
) : StatusHttpClient {

    override fun fetchStatus(url: String): CheckoutSessionStatus {
        val connection = try {
            (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMillis
                readTimeout = readTimeoutMillis
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "wire-checkout-android/$VERSION")
            }
        } catch (e: Exception) {
            throw WireCheckoutException.Network(e.message ?: "failed to open connection", e)
        }

        try {
            val code = try {
                connection.responseCode
            } catch (e: Exception) {
                throw WireCheckoutException.Network(e.message ?: "request failed", e)
            }
            if (code !in 200..299) {
                throw WireCheckoutException.HttpStatus(code)
            }
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            return CheckoutSessionStatus.decode(body)
        } finally {
            connection.disconnect()
        }
    }

    public companion object {
        internal const val VERSION: String = "1.0.0"
    }
}
