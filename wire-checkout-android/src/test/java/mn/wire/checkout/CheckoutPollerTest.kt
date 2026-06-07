package mn.wire.checkout

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutPollerTest {

    private val endpoint = CheckoutEndpoint.parse("https://pay.wire.mn/c/tok")

    /** Returns each scripted status in order; the last one repeats. Throwables are rethrown. */
    private class ScriptedHttp(private val script: List<Any>) : StatusHttpClient {
        var calls = 0
            private set

        override fun fetchStatus(url: String): CheckoutSessionStatus {
            val item = script[minOf(calls, script.lastIndex)]
            calls++
            return when (item) {
                is CheckoutSessionStatus -> item
                is Throwable -> throw item
                else -> error("bad script item")
            }
        }
    }

    private fun status(payment: String, redirect: String? = null) =
        CheckoutSessionStatus("checkout.session.status", "open", payment, redirect)

    @Test
    fun pollsPendingThenSucceeded_resolvesCompleted() = runTest {
        val http = ScriptedHttp(
            listOf(
                status("requires_payment"),
                status("processing"),
                status("succeeded", "https://m/r"),
            )
        )
        // Drive the injected clock off the test scheduler's virtual time.
        val clock = MonotonicClock { currentTime * 1_000_000L }
        val poller = CheckoutPoller(http, WireCheckoutConfig(pollIntervalMillis = 2_000L), clock)

        val result = poller.await(endpoint)

        assertTrue(result is WireCheckoutResult.Completed)
        assertEquals("https://m/r", result.redirectUrl)
        assertEquals(3, http.calls)
    }

    @Test
    fun immediateCanceled_resolvesCanceled() = runTest {
        val http = ScriptedHttp(listOf(status("canceled")))
        val clock = MonotonicClock { currentTime * 1_000_000L }
        val poller = CheckoutPoller(http, WireCheckoutConfig(), clock)
        assertTrue(poller.await(endpoint) is WireCheckoutResult.Canceled)
    }

    @Test
    fun failed_resolvesFailed() = runTest {
        val http = ScriptedHttp(listOf(status("failed")))
        val clock = MonotonicClock { currentTime * 1_000_000L }
        val poller = CheckoutPoller(http, WireCheckoutConfig(), clock)
        assertTrue(poller.await(endpoint) is WireCheckoutResult.Failed)
    }

    @Test
    fun neverTerminal_timesOut() = runTest {
        val http = ScriptedHttp(listOf(status("requires_payment")))
        val clock = MonotonicClock { currentTime * 1_000_000L }
        val poller = CheckoutPoller(
            http,
            WireCheckoutConfig(pollIntervalMillis = 2_000L, timeoutMillis = 10_000L),
            clock,
        )
        assertFailsWith<WireCheckoutException.Timeout> { poller.await(endpoint) }
    }

    @Test
    fun transientNetworkError_isRetried() = runTest {
        val http = ScriptedHttp(
            listOf(
                WireCheckoutException.Network("boom"),
                status("succeeded"),
            )
        )
        val clock = MonotonicClock { currentTime * 1_000_000L }
        val poller = CheckoutPoller(http, WireCheckoutConfig(pollIntervalMillis = 2_000L), clock)
        assertTrue(poller.await(endpoint) is WireCheckoutResult.Completed)
        assertEquals(2, http.calls)
    }

    @Test
    fun httpStatusError_propagates() = runTest {
        val http = ScriptedHttp(listOf(WireCheckoutException.HttpStatus(500)))
        val clock = MonotonicClock { currentTime * 1_000_000L }
        val poller = CheckoutPoller(http, WireCheckoutConfig(), clock)
        assertFailsWith<WireCheckoutException.HttpStatus> { poller.await(endpoint) }
    }

    @Test
    fun cancellation_stopsPolling() = runTest {
        val http = ScriptedHttp(listOf(status("requires_payment")))
        val clock = MonotonicClock { currentTime * 1_000_000L }
        val poller = CheckoutPoller(http, WireCheckoutConfig(pollIntervalMillis = 2_000L), clock)

        val job = launch { poller.await(endpoint) }
        testScheduler.advanceTimeBy(5_000L)
        job.cancel()
        testScheduler.advanceUntilIdle()
        assertTrue(job.isCancelled)
    }
}
