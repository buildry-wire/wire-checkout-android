package mn.wire.checkout

/** Errors surfaced by [WireCheckout] and the pure polling logic. */
public sealed class WireCheckoutException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {

    /** The supplied checkout URL was malformed or did not match the expected `/c/{token}` shape. */
    public class InvalidUrl(value: String) :
        WireCheckoutException("Invalid checkout URL: $value")

    /** The checkout URL host was not an allowed Wire checkout host. */
    public class InvalidHost(host: String) :
        WireCheckoutException("Checkout host is not allowed: $host")

    /** A networking failure occurred while talking to the status API. */
    public class Network(detail: String, cause: Throwable? = null) :
        WireCheckoutException("Network error: $detail", cause)

    /** The status API returned a non-2xx HTTP status. */
    public class HttpStatus(public val code: Int) :
        WireCheckoutException("Unexpected HTTP status: $code")

    /** The status API response body could not be decoded. */
    public class Decoding(detail: String) :
        WireCheckoutException("Failed to decode status response: $detail")

    /** Polling exceeded the configured timeout before reaching a terminal state. */
    public class Timeout :
        WireCheckoutException("Timed out waiting for the checkout to reach a final state.")

    /** The in-app browser presentation failed to start or returned an error. */
    public class Presentation(detail: String, cause: Throwable? = null) :
        WireCheckoutException("Could not present the checkout: $detail", cause)
}
