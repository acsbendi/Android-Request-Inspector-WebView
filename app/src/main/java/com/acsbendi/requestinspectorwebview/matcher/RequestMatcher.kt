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
    fun onPageStarted(url: String) {}
}

