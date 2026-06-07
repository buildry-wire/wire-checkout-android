package mn.wire.checkout

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Entry point for presenting a Wire hosted checkout on Android.
 *
 * The merchant backend creates the checkout session server-side (with its secret key) and returns
 * the hosted `url` (`https://pay.wire.mn/c/{token}`) — or a `{ token, baseUrl }` pair — to the app.
 * This SDK is **client-side only** and never handles a secret key.
 *
 * Typical use from a coroutine:
 * ```kotlin
 * val checkout = WireCheckout(context)
 * val endpoint = checkout.launch(url, redirectScheme = "myapp")
 * val result = checkout.awaitResult(endpoint)
 * when (result) {
 *     is WireCheckoutResult.Completed -> // payment succeeded
 *     is WireCheckoutResult.Canceled  -> // buyer canceled
 *     is WireCheckoutResult.Failed    -> // payment failed
 * }
 * ```
 *
 * The authoritative outcome always comes from the status API ([awaitResult]) — never from the
 * redirect. Call [awaitResult] when the buyer returns to the app (via the redirect deeplink or by
 * backing out of the Custom Tab) to do a final, authoritative status poll.
 */
public class WireCheckout @JvmOverloads constructor(
    private val context: Context,
    private val config: WireCheckoutConfig = WireCheckoutConfig(),
    private val http: StatusHttpClient = HttpUrlConnectionStatusClient(),
) {
    private val poller = CheckoutPoller(http, config)

    /**
     * Parse and validate a full hosted-checkout [url] and present it in a Chrome Custom Tab.
     *
     * @param redirectScheme the URI scheme the app registers via an intent-filter to receive the
     *   return deeplink. Informational here (the hosted page redirects to the session's configured
     *   return URL); pass it so the caller keeps the contract explicit.
     * @return the parsed [CheckoutEndpoint] — pass it to [awaitResult] to resolve the outcome.
     * @throws WireCheckoutException.InvalidUrl / [WireCheckoutException.InvalidHost] on a bad URL.
     * @throws WireCheckoutException.Presentation if no browser can present the Custom Tab.
     */
    @JvmOverloads
    public fun launch(url: String, redirectScheme: String? = null): CheckoutEndpoint {
        val endpoint = CheckoutEndpoint.parse(url, config.allowInsecureLocalhost)
        present(endpoint.checkoutUrl)
        return endpoint
    }

    /**
     * Present a checkout identified by [token] against [baseUrl] (or [WireCheckoutConfig.baseUrl]).
     *
     * Named distinctly from [launch] because, with `@JvmOverloads`, a second `launch(String, ...)`
     * overload would generate a JVM signature that clashes with [launch].
     */
    @JvmOverloads
    public fun launchWithToken(
        token: String,
        baseUrl: String? = null,
        redirectScheme: String? = null,
    ): CheckoutEndpoint {
        val origin = baseUrl ?: config.baseUrl
            ?: throw WireCheckoutException.InvalidUrl("no baseUrl provided")
        val endpoint = CheckoutEndpoint.fromToken(token, origin, config.allowInsecureLocalhost)
        present(endpoint.checkoutUrl)
        return endpoint
    }

    /**
     * Poll the status API for [endpoint] until it reaches a terminal state, the configured timeout
     * elapses ([WireCheckoutException.Timeout]), or the surrounding coroutine is cancelled. Runs
     * the blocking HTTP work on [Dispatchers.IO].
     */
    public suspend fun awaitResult(endpoint: CheckoutEndpoint): WireCheckoutResult =
        withContext(Dispatchers.IO) { poller.await(endpoint) }

    /** Convenience overload: parse [url] then [awaitResult]. */
    public suspend fun awaitResult(url: String): WireCheckoutResult =
        awaitResult(CheckoutEndpoint.parse(url, config.allowInsecureLocalhost))

    private fun present(checkoutUrl: String) {
        try {
            val intent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            intent.intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.launchUrl(context, Uri.parse(checkoutUrl))
        } catch (e: Exception) {
            throw WireCheckoutException.Presentation(
                e.message ?: "no browser available for Custom Tabs",
                e,
            )
        }
    }
}
