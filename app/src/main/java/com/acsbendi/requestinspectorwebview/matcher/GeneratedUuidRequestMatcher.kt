package com.acsbendi.requestinspectorwebview.matcher

import android.webkit.WebResourceRequest
import com.acsbendi.requestinspectorwebview.RequestInspectorJavaScriptInterface.RecordedRequest
import com.acsbendi.requestinspectorwebview.WebViewRequest

abstract class GeneratedUuidRequestMatcher : RequestMatcher {

    private val recordedRequests = mutableMapOf<String, RecordedRequest>()

    abstract fun getUuidFromRequest(recordedRequest: RecordedRequest): String?
    abstract fun getUuidFromRequest(webResourceRequest: WebResourceRequest): String?
    abstract fun removeUuidFromRequests(
        request: WebResourceRequest,
        recordedRequest: RecordedRequest?
    ): Pair<WebResourceRequest, RecordedRequest?>

    final override fun addRecordedRequest(recordedRequest: RecordedRequest) {
        val id = getUuidFromRequest(recordedRequest) ?: return

        synchronized(recordedRequests) {
            recordedRequests[id] = recordedRequest
        }
    }

    override fun createWebViewRequest(request: WebResourceRequest): WebViewRequest {
        val recordedRequest = findRecordedRequest(request)
        val (cleanedRequest, cleanedRecordedRequest) = removeUuidFromRequests(request, recordedRequest)
        return WebViewRequest.create(cleanedRequest, cleanedRecordedRequest)
    }


    private  fun findRecordedRequest(request: WebResourceRequest): RecordedRequest? {
        val id = getUuidFromRequest(request) ?: return null
        val recordedRequest = synchronized(recordedRequests) {
            recordedRequests.remove(id)
        }
        return recordedRequest
    }

    override fun onPageStarted(url: String) {}

    companion object {
        const val REQUEST_INSPECTOR_ID = "x-request-inspector-id"
        const val LOG_TAG = "RequestGeneratedUuidMatcher"
    }
}
