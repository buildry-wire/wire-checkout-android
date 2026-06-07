package mn.wire.checkout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CheckoutEndpointTest {

    @Test
    fun parse_validUrl_extractsOriginTokenAndStatusUrl() {
        val e = CheckoutEndpoint.parse("https://pay.wire.mn/c/cs_test_123")
        assertEquals("https://pay.wire.mn", e.origin)
        assertEquals("cs_test_123", e.token)
        assertEquals("https://pay.wire.mn/c/cs_test_123", e.checkoutUrl)
        assertEquals("https://pay.wire.mn/checkout/cs_test_123/status", e.statusUrl)
    }

    @Test
    fun parse_subdomainOfWireMn_isAllowed() {
        val e = CheckoutEndpoint.parse("https://checkout.wire.mn/c/tok")
        assertEquals("https://checkout.wire.mn", e.origin)
    }

    @Test
    fun parse_withPort_preservesPortInOrigin() {
        val e = CheckoutEndpoint.parse("http://localhost:8080/c/tok", allowInsecureLocalhost = true)
        assertEquals("http://localhost:8080", e.origin)
        assertEquals("http://localhost:8080/checkout/tok/status", e.statusUrl)
    }

    @Test
    fun parse_httpOnNonLocalhost_isRejected() {
        assertFailsWith<WireCheckoutException.InvalidHost> {
            CheckoutEndpoint.parse("http://pay.wire.mn/c/tok")
        }
    }

    @Test
    fun parse_disallowedHost_isRejected() {
        assertFailsWith<WireCheckoutException.InvalidHost> {
            CheckoutEndpoint.parse("https://evil.example.com/c/tok")
        }
    }

    @Test
    fun parse_lookalikeHostSuffix_isRejected() {
        // "notwire.mn" must not match the "wire.mn" suffix.
        assertFailsWith<WireCheckoutException.InvalidHost> {
            CheckoutEndpoint.parse("https://notwire.mn/c/tok")
        }
    }

    @Test
    fun parse_wrongPathShape_isRejected() {
        assertFailsWith<WireCheckoutException.InvalidUrl> {
            CheckoutEndpoint.parse("https://pay.wire.mn/checkout/tok")
        }
        assertFailsWith<WireCheckoutException.InvalidUrl> {
            CheckoutEndpoint.parse("https://pay.wire.mn/c/")
        }
        assertFailsWith<WireCheckoutException.InvalidUrl> {
            CheckoutEndpoint.parse("https://pay.wire.mn/c/tok/extra")
        }
    }

    @Test
    fun parse_localhostHttpDisallowed_isRejected() {
        assertFailsWith<WireCheckoutException.InvalidHost> {
            CheckoutEndpoint.parse("http://localhost/c/tok", allowInsecureLocalhost = false)
        }
    }

    @Test
    fun parse_garbage_isRejected() {
        assertFailsWith<WireCheckoutException.InvalidUrl> {
            CheckoutEndpoint.parse("not a url")
        }
    }

    @Test
    fun fromToken_buildsCheckoutUrl() {
        val e = CheckoutEndpoint.fromToken("tok_42", "https://pay.wire.mn")
        assertEquals("https://pay.wire.mn", e.origin)
        assertEquals("tok_42", e.token)
        assertEquals("https://pay.wire.mn/c/tok_42", e.checkoutUrl)
    }

    @Test
    fun fromToken_emptyToken_isRejected() {
        assertFailsWith<WireCheckoutException.InvalidUrl> {
            CheckoutEndpoint.fromToken("  ", "https://pay.wire.mn")
        }
    }

    @Test
    fun fromToken_badBaseHost_isRejected() {
        assertFailsWith<WireCheckoutException.InvalidHost> {
            CheckoutEndpoint.fromToken("tok", "https://evil.example.com")
        }
    }
}
