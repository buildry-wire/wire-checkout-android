package mn.wire.checkout

import java.net.URI

/**
 * A parsed and validated hosted-checkout location.
 *
 * A hosted checkout URL looks like `https://pay.wire.mn/c/{token}`. From it we derive the
 * `origin` (scheme + host + optional port) and the `token`, which together build the status
 * endpoint `{origin}/checkout/{token}/status`.
 *
 * This is pure JVM logic with no Android dependencies, so it can be unit-tested with plain JUnit.
 */
public data class CheckoutEndpoint(
    /** Scheme + host (+ port) — e.g. `https://pay.wire.mn`. */
    val origin: String,
    /** The opaque checkout token from the `/c/{token}` path. */
    val token: String,
    /** The full hosted checkout URL to present in the browser. */
    val checkoutUrl: String,
) {
    /** `GET` this to learn the authoritative status. */
    val statusUrl: String
        get() = "$origin/checkout/$token/status"

    public companion object {
        /** Hosts permitted for a hosted checkout (the host itself or any subdomain). */
        private val ALLOWED_HOST_SUFFIXES = listOf("wire.mn")

        /**
         * Parse a full hosted-checkout URL of the form `{origin}/c/{token}`.
         *
         * @param allowInsecureLocalhost permit plain `http` for `localhost`/`127.0.0.1`
         *   (used in tests and local development only).
         */
        @JvmStatic
        @JvmOverloads
        public fun parse(url: String, allowInsecureLocalhost: Boolean = true): CheckoutEndpoint {
            val uri = try {
                URI(url.trim())
            } catch (e: Exception) {
                throw WireCheckoutException.InvalidUrl(url)
            }

            val scheme = uri.scheme?.lowercase() ?: throw WireCheckoutException.InvalidUrl(url)
            val host = uri.host?.lowercase() ?: throw WireCheckoutException.InvalidUrl(url)
            validateHost(scheme, host, allowInsecureLocalhost)

            // Path must be /c/{token} with a non-empty token.
            val segments = (uri.rawPath ?: "").split('/').filter { it.isNotEmpty() }
            if (segments.size != 2 || segments[0] != "c") {
                throw WireCheckoutException.InvalidUrl(url)
            }
            val token = decode(segments[1])
            if (token.isEmpty()) throw WireCheckoutException.InvalidUrl(url)

            return CheckoutEndpoint(
                origin = origin(scheme, host, uri.port),
                token = token,
                checkoutUrl = url.trim(),
            )
        }

        /**
         * Build an endpoint from a `{ token, baseUrl }` pair. `baseUrl` is the origin
         * (e.g. `https://pay.wire.mn`).
         */
        @JvmStatic
        @JvmOverloads
        public fun fromToken(
            token: String,
            baseUrl: String,
            allowInsecureLocalhost: Boolean = true,
        ): CheckoutEndpoint {
            val trimmedToken = token.trim()
            if (trimmedToken.isEmpty()) throw WireCheckoutException.InvalidUrl("empty token")

            val uri = try {
                URI(baseUrl.trim())
            } catch (e: Exception) {
                throw WireCheckoutException.InvalidUrl(baseUrl)
            }
            val scheme = uri.scheme?.lowercase() ?: throw WireCheckoutException.InvalidUrl(baseUrl)
            val host = uri.host?.lowercase() ?: throw WireCheckoutException.InvalidUrl(baseUrl)
            validateHost(scheme, host, allowInsecureLocalhost)

            val origin = origin(scheme, host, uri.port)
            return CheckoutEndpoint(
                origin = origin,
                token = trimmedToken,
                checkoutUrl = "$origin/c/$trimmedToken",
            )
        }

        private fun origin(scheme: String, host: String, port: Int): String =
            if (port == -1) "$scheme://$host" else "$scheme://$host:$port"

        private fun validateHost(scheme: String, host: String, allowInsecureLocalhost: Boolean) {
            val isLocalhost = host == "localhost" || host == "127.0.0.1"
            if (isLocalhost) {
                if (!allowInsecureLocalhost && scheme != "https") {
                    throw WireCheckoutException.InvalidHost(host)
                }
                return
            }
            if (scheme != "https") throw WireCheckoutException.InvalidHost(host)
            val allowed = ALLOWED_HOST_SUFFIXES.any { suffix ->
                host == suffix || host.endsWith(".$suffix")
            }
            if (!allowed) throw WireCheckoutException.InvalidHost(host)
        }

        private fun decode(segment: String): String = try {
            java.net.URLDecoder.decode(segment, Charsets.UTF_8.name())
        } catch (e: Exception) {
            segment
        }
    }
}
