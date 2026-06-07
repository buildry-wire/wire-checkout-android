package mn.wire.checkout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PaymentStatusTest {

    private fun status(payment: String, redirect: String? = null) =
        CheckoutSessionStatus("checkout.session.status", "open", payment, redirect)

    @Test
    fun nonTerminal_statesKeepPolling() {
        assertNull(PaymentStatus.resolve(status("requires_payment")))
        assertNull(PaymentStatus.resolve(status("processing")))
    }

    @Test
    fun succeeded_resolvesCompleted() {
        val r = PaymentStatus.resolve(status("succeeded", "https://m/r"))
        assertTrue(r is WireCheckoutResult.Completed)
        assertEquals("succeeded", r.paymentStatus)
        assertEquals("https://m/r", r.redirectUrl)
    }

    @Test
    fun canceled_resolvesCanceled() {
        assertTrue(PaymentStatus.resolve(status("canceled")) is WireCheckoutResult.Canceled)
        assertTrue(PaymentStatus.resolve(status("cancelled")) is WireCheckoutResult.Canceled)
    }

    @Test
    fun otherTerminal_resolvesFailed() {
        assertTrue(PaymentStatus.resolve(status("failed")) is WireCheckoutResult.Failed)
        assertTrue(PaymentStatus.resolve(status("expired")) is WireCheckoutResult.Failed)
    }
}
