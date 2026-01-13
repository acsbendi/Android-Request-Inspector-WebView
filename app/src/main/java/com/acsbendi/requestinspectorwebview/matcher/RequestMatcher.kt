package com.acsbendi.requestinspectorwebview.matcher

import com.acsbendi.requestinspectorwebview.RequestInspectorJavaScriptInterface.RecordedRequest
import android.webkit.WebResourceRequest
import com.acsbendi.requestinspectorwebview.WebViewRequest

interface RequestMatcher {
    fun addRecordedRequest(recordedRequest: RecordedRequest)
    fun createWebViewRequest(request: WebResourceRequest): WebViewRequest
}

