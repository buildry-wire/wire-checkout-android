package mn.wire.checkout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CheckoutSessionStatusTest {

    @Test
    fun decode_fullBody_parsesAllFields() {
        val s = CheckoutSessionStatus.decode(
            """
            {
              "object": "checkout.session.status",
              "status": "complete",
              "payment_status": "succeeded",
              "redirect_url": "https://merchant.example/return"
            }
            """.trimIndent()
        )
        assertEquals("checkout.session.status", s.`object`)
        assertEquals("complete", s.status)
        assertEquals("succeeded", s.paymentStatus)
        assertEquals("https://merchant.example/return", s.redirectUrl)
    }

    @Test
    fun decode_missingRedirectUrl_isNull() {
        val s = CheckoutSessionStatus.decode(
            """{"object":"checkout.session.status","status":"open","payment_status":"requires_payment"}"""
        )
        assertEquals("requires_payment", s.paymentStatus)
        assertNull(s.redirectUrl)
    }

    @Test
    fun decode_missingPaymentStatus_throwsDecoding() {
        assertFailsWith<WireCheckoutException.Decoding> {
            CheckoutSessionStatus.decode("""{"object":"checkout.session.status","status":"open"}""")
        }
    }

    @Test
    fun decode_invalidJson_throwsDecoding() {
        assertFailsWith<WireCheckoutException.Decoding> {
            CheckoutSessionStatus.decode("not json")
        }
    }
}
