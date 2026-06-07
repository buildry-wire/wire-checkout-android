package mn.wire.checkout

/**
 * The terminal outcome of a hosted checkout, derived from the status API — never from the
 * redirect alone.
 */
public sealed class WireCheckoutResult {
    /** The raw `payment_status` from the status API (e.g. `succeeded`, `canceled`). */
    public abstract val paymentStatus: String

    /** The redirect target reported by the hosted page, if any. */
    public abstract val redirectUrl: String?

    /** The payment succeeded. */
    public data class Completed(
        override val paymentStatus: String,
        override val redirectUrl: String?,
    ) : WireCheckoutResult()

    /** The buyer canceled the checkout. */
    public data class Canceled(
        override val paymentStatus: String,
        override val redirectUrl: String?,
    ) : WireCheckoutResult()

    /** The payment failed or the session expired. */
    public data class Failed(
        override val paymentStatus: String,
        override val redirectUrl: String?,
    ) : WireCheckoutResult()
}
