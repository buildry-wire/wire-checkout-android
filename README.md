# wire-checkout-android

Client-side Android SDK for presenting a **Wire hosted checkout** session in a Chrome Custom Tab
and resolving the result from the authoritative status API.

This SDK runs in your buyer-facing app and **never handles a secret key**. Your backend creates the
checkout session server-side and hands the app the hosted `url` (or a `{ token, baseUrl }` pair).

- Group: `mn.wire` · Artifact: `wire-checkout-android` · `minSdk 21`, `compileSdk 34`
- Docs: https://docs.wire.mn

## Install

```kotlin
// settings.gradle.kts — Maven Central is the default repository for releases
dependencies {
    implementation("mn.wire:wire-checkout-android:1.0.0")
}
```

## How the flow works

1. Your **backend** creates a checkout session (using its secret key) and gets back a hosted
   `url` like `https://pay.wire.mn/c/{token}`.
2. Your app receives that `url` and launches it with this SDK.
3. The SDK opens the hosted checkout in a Custom Tab. The buyer pays.
4. When the buyer returns to your app, the SDK polls the status API and resolves the outcome.

The **source of truth is the status API**, never the redirect.

## Quickstart

```kotlin
import mn.wire.checkout.WireCheckout
import mn.wire.checkout.WireCheckoutResult

val checkout = WireCheckout(context)

// 1. Launch the hosted checkout in a Custom Tab.
val endpoint = checkout.launch(url, redirectScheme = "myapp")

// 2. When the buyer returns (via the return deeplink, or by backing out of the Custom Tab),
//    do an authoritative status poll. Call this from a coroutine.
when (val result = checkout.awaitResult(endpoint)) {
    is WireCheckoutResult.Completed -> showSuccess(result.redirectUrl)
    is WireCheckoutResult.Canceled  -> showCanceled()
    is WireCheckoutResult.Failed    -> showFailure(result.paymentStatus)
}
```

Configuration is optional:

```kotlin
val checkout = WireCheckout(
    context,
    WireCheckoutConfig(
        pollIntervalMillis = 2_000,   // default 2s
        timeoutMillis = 600_000,      // default 10 min
    ),
)
```

## Return deeplink (intent-filter)

The hosted page redirects the buyer back to your app via a deeplink. Register the scheme/host your
backend configured as the session's return URL with an intent-filter on the activity that should
receive the return:

```xml
<activity android:name=".CheckoutReturnActivity" android:exported="true">
    <intent-filter android:autoVerify="false">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <!-- match what your backend set as the session return URL -->
        <data android:scheme="myapp" android:host="checkout-return" />
    </intent-filter>
</activity>
```

When that activity is opened by the deeplink, call `checkout.awaitResult(endpoint)` to resolve the
final outcome. Always treat the status API result — not the deeplink itself — as authoritative.

## Notes

- The merchant creates the session **server-side**; this SDK is client-side and never sees a
  secret key.
- The status endpoint is unauthenticated and the SDK attaches no credentials.
- Operators shown in checkout are the operators enabled on your account. In sandbox, examples use
  `["sandbox"]`.

## License

MIT — see [LICENSE](LICENSE).
