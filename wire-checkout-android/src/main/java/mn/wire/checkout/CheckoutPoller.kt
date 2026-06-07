package mn.wire.checkout

import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/**
 * A monotonic clock seam, so tests can drive timeout logic deterministically without real time.
 */
public fun interface MonotonicClock {
    /** Elapsed nanoseconds from an arbitrary fixed origin (like [System.nanoTime]). */
    public fun nanoTime(): Long
}

/**
 * Polls the status endpoint until it reaches a terminal state, the timeout elapses, or the
 * surrounding coroutine is cancelled. Pure logic: the HTTP call and the clock are injected, and
 * waiting uses [kotlinx.coroutines.delay] so it is fully cancellable.
 */
public class CheckoutPoller(
    private val http: StatusHttpClient,
    private val config: WireCheckoutConfig = WireCheckoutConfig(),
    private val clock: MonotonicClock = MonotonicClock { System.nanoTime() },
) {
    /**
     * Poll [endpoint] until terminal.
     *
     * Behaviour:
     * - Returns the resolved [WireCheckoutResult] as soon as `payment_status` is terminal
     *   (`succeeded` → Completed, `canceled` → Canceled, otherwise → Failed).
     * - Waits [WireCheckoutConfig.pollIntervalMillis] between attempts while non-terminal.
     * - Throws [WireCheckoutException.Timeout] once [WireCheckoutConfig.timeoutMillis] elapses.
     * - Network errors are retried (transient); other [WireCheckoutException]s propagate.
     * - Honors coroutine cancellation between attempts and waits.
     */
    public suspend fun await(endpoint: CheckoutEndpoint): WireCheckoutResult {
        val start = clock.nanoTime()
        val timeoutNanos = config.timeoutMillis * 1_000_000L

        while (true) {
            coroutineContext.ensureActive()

            val status = try {
                http.fetchStatus(endpoint.statusUrl)
            } catch (e: WireCheckoutException.Network) {
                // Transient — fall through to the timeout check and retry after the interval.
                null
            }

            if (status != null) {
                PaymentStatus.resolve(status)?.let { return it }
            }

            if (clock.nanoTime() - start >= timeoutNanos) {
                throw WireCheckoutException.Timeout()
            }
            delay(config.pollIntervalMillis)
        }
    }
}
