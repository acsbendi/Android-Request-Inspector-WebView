package com.acsbendi.requestinspectorwebview.matcher

import com.acsbendi.requestinspectorwebview.RequestInspectorJavaScriptInterface.RecordedRequest
import android.webkit.WebResourceRequest
import com.acsbendi.requestinspectorwebview.WebViewRequest
import org.json.JSONObject

interface RequestMatcher {
    fun addRecordedRequest(recordedRequest: RecordedRequest)
    fun createWebViewRequest(request: WebResourceRequest): WebViewRequest
    fun getAdditionalHeaders(url: String): JSONObject = JSONObject()
    fun getAdditionalQueryParams(): String = ""
    fun onLoadMainFrame(url: String) {
        // Delegate to onPageStarted() so that existing implementations overriding the deprecated
        // method continue to work without modification.
        @Suppress("DEPRECATION")
        onPageStarted(url)
    }

    @Deprecated(
        message = "Renamed to onLoadMainFrame(). Override onLoadMainFrame() instead.",
        replaceWith = ReplaceWith("onLoadMainFrame(url)")
    )
    fun onPageStarted(url: String) {}
}

