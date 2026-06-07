package mn.wire.checkout

/**
 * Tunables for [WireCheckout]. All fields have sensible defaults.
 *
 * @property baseUrl optional default origin (e.g. `https://pay.wire.mn`) used by the
 *   `token`-based launch overloads. The full-URL overloads ignore it.
 * @property pollIntervalMillis delay between status polls (default 2s).
 * @property timeoutMillis maximum total time to poll before giving up (default 10 min).
 * @property allowInsecureLocalhost permit `http://localhost` checkout URLs (tests/dev only).
 */
public data class WireCheckoutConfig(
    val baseUrl: String? = null,
    val pollIntervalMillis: Long = 2_000L,
    val timeoutMillis: Long = 600_000L,
    val allowInsecureLocalhost: Boolean = false,
)
