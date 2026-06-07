package mn.wire.checkout

/**
 * The status state machine. Maps a raw `payment_status` string to either a non-terminal "keep
 * polling" signal or a terminal [WireCheckoutResult]. Pure logic, no Android or IO dependency.
 */
internal object PaymentStatus {
    /** Non-terminal states — the poller keeps waiting while in one of these. */
    private val NON_TERMINAL = setOf("requires_payment", "processing")

    /** Terminal "success" state. */
    private const val SUCCEEDED = "succeeded"

    /** Terminal "canceled" states. */
    private val CANCELED = setOf("canceled", "cancelled")

    /** True while polling should continue (status not yet terminal). */
    fun isTerminal(paymentStatus: String): Boolean = paymentStatus !in NON_TERMINAL

    /**
     * Resolve a terminal [WireCheckoutResult] from a status snapshot.
     *
     * - `succeeded` → [WireCheckoutResult.Completed]
     * - `canceled`/`cancelled` → [WireCheckoutResult.Canceled]
     * - anything else terminal (e.g. `failed`, `expired`) → [WireCheckoutResult.Failed]
     *
     * Returns `null` if the status is still non-terminal.
     */
    fun resolve(status: CheckoutSessionStatus): WireCheckoutResult? {
        if (!isTerminal(status.paymentStatus)) return null
        return when {
            status.paymentStatus == SUCCEEDED ->
                WireCheckoutResult.Completed(status.paymentStatus, status.redirectUrl)
            status.paymentStatus in CANCELED ->
                WireCheckoutResult.Canceled(status.paymentStatus, status.redirectUrl)
            else ->
                WireCheckoutResult.Failed(status.paymentStatus, status.redirectUrl)
        }
    }
}
