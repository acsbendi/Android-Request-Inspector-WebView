package com.acsbendi.requestinspectorwebview

import android.graphics.Bitmap
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

open class RequestInspectorWebViewClient @JvmOverloads constructor(
    webView: WebView,
    private val options: RequestInspectorOptions = RequestInspectorOptions()
) : WebViewClient() {

    private val interceptionJavascriptInterface = RequestInspectorJavaScriptInterface()

    init {
        webView.addJavascriptInterface(interceptionJavascriptInterface, "RequestInspection")
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        Log.i(LOG_TAG, "Received request: " + request.toJsonStringWithCookies())
        val recordedRequest = interceptionJavascriptInterface.findRecordedRequestForUrl(
            request.url.toString()
        )
        Log.i(LOG_TAG, "Corresponding recorded request: $recordedRequest")
        return null
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        Log.i(LOG_TAG, "Page started loading, enabling request inspection. URL: $url")
        RequestInspectorJavaScriptInterface.enableInterception(
            view,
            options.extraJavaScriptToInject
        )
        super.onPageStarted(view, url, favicon)
    }

    private fun WebResourceRequest.toJsonStringWithCookies(): String {
        val cookies = CookieManager.getInstance().getCookie(url.toString())
        val headers = HashMap(requestHeaders)
        headers["Cookie"] = cookies
        return "{ " +
                "\"url\": \"" + url.toString().replace("\"", "\\\"") + '"' +
                ", \"method\": \"" + method.replace("\"", "\\\"") + '"' +
                ", \"isForMainFrame\": " + isForMainFrame +
                ", \"hasGesture\": " + hasGesture() +
                ", \"headers\": \"" + headers.toString().replace("\"", "\\\"") + '"' +
                " }"
    }

    companion object {
        private const val LOG_TAG = "RequestInspectorWebView"
    }
}
