package mn.wire.checkout

import org.json.JSONObject

/**
 * The raw `checkout.session.status` resource returned by
 * `GET {origin}/checkout/{token}/status`.
 *
 * ```json
 * {
 *   "object": "checkout.session.status",
 *   "status": "open",
 *   "payment_status": "requires_payment",
 *   "redirect_url": "https://merchant.example/return"
 * }
 * ```
 */
public data class CheckoutSessionStatus(
    /** Always `"checkout.session.status"`. */
    val `object`: String,
    /** Lifecycle of the session itself (e.g. `open`, `complete`, `expired`). */
    val status: String,
    /** Payment progress. The state machine keys off this field. */
    val paymentStatus: String,
    /** Where the hosted page redirected the buyer once the flow finished. */
    val redirectUrl: String?,
) {
    public companion object {
        /**
         * Decode a status response body. `org.json` ships with the Android platform, so this adds
         * no extra runtime dependency.
         *
         * @throws WireCheckoutException.Decoding if the body is not valid JSON.
         */
        @JvmStatic
        public fun decode(body: String): CheckoutSessionStatus {
            val json = try {
                JSONObject(body)
            } catch (e: Exception) {
                throw WireCheckoutException.Decoding(e.message ?: "invalid JSON")
            }
            val paymentStatus = json.optString("payment_status", "")
            if (paymentStatus.isEmpty()) {
                throw WireCheckoutException.Decoding("missing payment_status")
            }
            return CheckoutSessionStatus(
                `object` = json.optString("object", ""),
                status = json.optString("status", ""),
                paymentStatus = paymentStatus,
                redirectUrl = json.optString("redirect_url").ifEmpty { null },
            )
        }
    }
}
