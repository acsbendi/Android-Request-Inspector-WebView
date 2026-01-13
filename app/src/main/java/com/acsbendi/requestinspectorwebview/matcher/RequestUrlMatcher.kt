package com.acsbendi.requestinspectorwebview.matcher

import com.acsbendi.requestinspectorwebview.RequestInspectorJavaScriptInterface.RecordedRequest
import android.webkit.WebResourceRequest
import com.acsbendi.requestinspectorwebview.WebViewRequest

class RequestUrlMatcher : RequestMatcher {
    private val recordedRequests = ArrayList<RecordedRequest>()

    override fun addRecordedRequest(recordedRequest: RecordedRequest) {
        synchronized(recordedRequests) {
            recordedRequests.add(recordedRequest)
        }
    }

    override fun createWebViewRequest(request: WebResourceRequest): WebViewRequest {
        val recordedRequest = findRecordedRequest(request)
        return WebViewRequest.create(request, recordedRequest)
    }

    private fun findRecordedRequest(request: WebResourceRequest): RecordedRequest? {
        return synchronized(recordedRequests) {
            val url = request.url.toString()
            // use findLast instead of find to find the last added query matching a URL -
            // they are included at the end of the list when written.
            recordedRequests.findLast { recordedRequest ->
                // Added search by exact URL to find the actual request body
                url == recordedRequest.url.toString()
            } ?: recordedRequests.findLast { recordedRequest ->
                // Previously, there was only a search by contains, and because of this, sometimes the wrong request body was found
                url.contains(recordedRequest.url.toString())
            }
        }
    }
}
