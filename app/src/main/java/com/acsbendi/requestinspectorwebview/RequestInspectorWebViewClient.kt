package com.acsbendi.requestinspectorwebview

import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

class RequestInspectorWebViewClient() : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        Log.w(LOG_TAG, request.toJsonStringWithCookies())
        return null
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
