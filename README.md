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
    implementation 'com.github.acsbendi:Android-Request-Inspector-WebView:1.1.1'
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

For both cases you can choose between different strategies for how the recorded requests (including 
the body) and the intercepted requests are matched together. By default, only the url is used. If 
you want to use a different strategy, for example if you have parallel requests to the same url with
different bodies (e.g. GraphQL queries), you can pass a custom `RequestMatcher` to the constructor 
of `RequestInspectorWebViewClient`:

```kotlin
    val webView = WebView(this)
    webView.webViewClient = RequestInspectorWebViewClient(
        webView,
        matcher = RequestGeneratedUuidInHeaderMatcher()
    )
```

Currently available matchers are `GeneratedUuidInHeaderRequestMatcher` and 
`GeneratedUuidInUrlRequestMatcher`, which both create an UUID and add it to the request before it's 
recorded and sent from JavaScript. They only differ by how they attach the UUID to the request, as 
an additional header or as an additional query param. But both clean up the request when it's 
intecepted on native side, so before it's really sent out to the target. 

If you want to implement your own matching strategy, you can implement the `RequestMatcher` 
interface and pass an instance of it to the constructor of `RequestInspectorWebViewClient`.

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
