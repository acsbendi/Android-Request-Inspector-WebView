package com.acsbendi.requestinspectorwebview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewFeature
import com.acsbendi.requestinspectorwebview.matcher.RequestMatcher
import com.acsbendi.requestinspectorwebview.matcher.RequestUrlMatcher

@SuppressLint("SetJavaScriptEnabled")
open class RequestInspectorWebViewClient @JvmOverloads constructor(
    webView: WebView,
    val matcher: RequestMatcher = RequestUrlMatcher(),
    private val options: RequestInspectorOptions = RequestInspectorOptions()
) : WebViewClient() {

    private val interceptionJavascriptInterface = RequestInspectorJavaScriptInterface(webView, matcher)

    private val isDocumentStartScriptSupported = WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)

    init {
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        if (isDocumentStartScriptSupported) {
            RequestInspectorJavaScriptInterface.registerDocumentStartScript(webView, options.extraJavaScriptToInject)
        }
    }

    final override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        if (request.isForMainFrame) {
            matcher.onLoadMainFrame(request.url.toString())
        }
        val webViewRequest = interceptionJavascriptInterface.createWebViewRequest(request)
        return shouldInterceptRequest(view, webViewRequest)
    }

    open fun shouldInterceptRequest(
        view: WebView,
        webViewRequest: WebViewRequest
    ): WebResourceResponse? {
        logWebViewRequest(webViewRequest)
        return null
    }

    @Suppress("MemberVisibilityCanBePrivate")
    protected fun logWebViewRequest(webViewRequest: WebViewRequest) {
        Log.i(LOG_TAG, "Sending request from WebView: $webViewRequest")
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        Log.i(LOG_TAG, "Page started loading, enabling request inspection. URL: $url")
        if (!isDocumentStartScriptSupported) {
            RequestInspectorJavaScriptInterface.enabledRequestInspection(
                view,
                options.extraJavaScriptToInject
            )
        }
        super.onPageStarted(view, url, favicon)
    }

    companion object {
        private const val LOG_TAG = "RequestInspectorWebView"
    }
}
