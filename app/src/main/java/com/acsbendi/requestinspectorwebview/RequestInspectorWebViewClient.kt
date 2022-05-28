package com.acsbendi.requestinspectorwebview

import android.graphics.Bitmap
import android.util.Log
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
        val recordedRequest = interceptionJavascriptInterface.findRecordedRequestForUrl(
            request.url.toString()
        )
        val webViewRequest = WebViewRequest.create(request, recordedRequest)
        onWebViewRequest(webViewRequest)
        return null
    }

    open fun onWebViewRequest(webViewRequest: WebViewRequest) {
        logWebViewRequest(webViewRequest)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    protected fun logWebViewRequest(webViewRequest: WebViewRequest) {
        Log.i(LOG_TAG, "Sending request from WebView: $webViewRequest")
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        Log.i(LOG_TAG, "Page started loading, enabling request inspection. URL: $url")
        RequestInspectorJavaScriptInterface.enableInterception(
            view,
            options.extraJavaScriptToInject
        )
        super.onPageStarted(view, url, favicon)
    }

    companion object {
        private const val LOG_TAG = "RequestInspectorWebView"
    }
}
