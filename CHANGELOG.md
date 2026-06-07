# Changelog

Format: Keep a Changelog; semver.

## [Unreleased]

## [1.0.0] - 2026-06-07
First public release.

### Added
- `WireCheckout` — presents a Wire hosted checkout session in a Chrome Custom Tab
  (`androidx.browser`) and resolves the outcome from the authoritative status API.
- `WireCheckoutResult` sealed type: `Completed` / `Canceled` / `Failed`, each carrying
  `paymentStatus` and `redirectUrl`.
- Cancellable coroutine-based polling of `GET {origin}/checkout/{token}/status` with a
  configurable interval and timeout (`WireCheckoutConfig`).
- Hosted-URL parsing/validation (`CheckoutEndpoint`) with host allow-listing and HTTPS
  enforcement, plus a `{ token, baseUrl }` constructor.
- Pure-JVM, framework-free core (endpoint parsing, status decoding, state machine, poller with
  injectable HTTP + clock seams) covered by JUnit unit tests.
- No third-party HTTP dependency (`HttpURLConnection` + platform `org.json`); client-side only,
  never handles a secret key.
