# Android Request Inspector WebView [![Release](https://jitpack.io/v/acsbendi/Android-Request-Inspector-WebView.svg)](https://jitpack.io/#acsbendi/Android-Request-Inspector-WebView)

Inspect and intercept full HTTP requests (including all headers, cookies and body) sent from Android WebViews.

This project is inspired by [android-post-webview](https://github.com/KeejOow/android-post-webview) and [request_data_webviewclient](https://github.com/KonstantinSchubert/request_data_webviewclient) and some code was taken from both projects.

Installation
===

**Step 1.** Add the JitPack repository to your build file:

```gradle
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

**Step 2.** Add the dependency

```gradle
dependencies {
    implementation 'com.github.acsbendi:Android-Request-Inspector-WebView:1.0.12'
}
```

Get the latest version on [JitPack](https://jitpack.io/#acsbendi/Android-Request-Inspector-WebView)

Usage
===

To log the requests (default functionality):

```kotlin
    val webView = WebView(this)
    webView.webViewClient = RequestInspectorWebViewClient(webView)
```

To manually process requests:

```kotlin
    val webView = WebView(this)
    webView.webViewClient = object : RequestInspectorWebViewClient(webView) {
        override fun shouldInterceptRequest(
            view: WebView,
            webViewRequest: WebViewRequest
        ): WebResourceResponse? {
            TODO("handle request manually based on data from webViewRequest and return custom response")
            return super.shouldInterceptRequest(view, webViewRequest)
        }
    }
```

Known limitations
===

Detailed data (e.g. request body) is not available for requests sent from iframes as it's [not possible to execute JavaScript code in iframes in Android WebViews](https://stackoverflow.com/questions/47820169/android-webview-run-javascript-in-all-frames-including-iframes). One possible workaround to still inspect the requests sent from a specific iframe is to load its URL into a different `WebView` and attach `RequestInspectorWebViewClient` to that.

Contributions
===

All feedback, PRs, and issues are welcome!

License
===
The MIT License

See [LICENSE](LICENSE)
