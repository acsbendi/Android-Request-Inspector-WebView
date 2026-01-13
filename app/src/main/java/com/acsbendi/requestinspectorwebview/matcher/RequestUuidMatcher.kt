package com.acsbendi.requestinspectorwebview.matcher

import android.net.Uri
import android.util.Log
import android.webkit.WebResourceRequest
import com.acsbendi.requestinspectorwebview.RequestInspectorJavaScriptInterface.RecordedRequest
import com.acsbendi.requestinspectorwebview.WebViewRequest
import org.json.JSONObject
import java.util.UUID
import androidx.core.net.toUri

/**
 * This matcher only works for NON CORS requests. It adds a unique UUID header to each request
 * originating from the WebView, and matches recorded requests based on that header.
 *
 * It doesn't work for CORS requests, because it changes the headers of the request, which influences the preflight
 * request checking for allowed headers. Even when cleaning up the headers after the request is matched with it's body,
 * the CORS request will fail because the browser engine only knows about the adapted header and doesn't execute the
 * CORS request, because the preflight check doesn't return the custom header as allowed.
 */
class RequestUuidMatcher() : RequestMatcher {

    private val recordedRequests = mutableMapOf<String, RecordedRequest>()
    private var origin: String = ""

    override fun addRecordedRequest(recordedRequest: RecordedRequest) {
        val id = getUuidFromRequest(recordedRequest) ?: return

        synchronized(recordedRequests) {
            recordedRequests[id] = recordedRequest
        }
    }

    private fun getUuidFromRequest(recordedRequest: RecordedRequest): String? =
        recordedRequest.headers[REQUEST_ID_HEADER]

    override fun createWebViewRequest(request: WebResourceRequest): WebViewRequest {
        val recordedRequest = findRecordedRequest(request)
        val (cleanedRequest, cleanedRecordedRequest) = cleanupRequests(request, recordedRequest)
        return WebViewRequest.create(cleanedRequest, cleanedRecordedRequest)
    }

    private fun cleanupRequests(request: WebResourceRequest, recordedRequest: RecordedRequest?): Pair<WebResourceRequest, RecordedRequest?> {
        // Clean up headers by removing REQUEST_ID_HEADER from both requests
        val cleanedRequest = object : WebResourceRequest by request {
            override fun getRequestHeaders(): Map<String, String> =
                request.requestHeaders.filter { (key, _) -> key != REQUEST_ID_HEADER }
        }
        val cleanedRecordedRequest = recordedRequest?.copy(
            headers = recordedRequest.headers.filter { (key, _) -> key != REQUEST_ID_HEADER }
        )
        return cleanedRequest to cleanedRecordedRequest
    }

    private  fun findRecordedRequest(request: WebResourceRequest): RecordedRequest? {
        val id = request.requestHeaders[REQUEST_ID_HEADER] ?: return null
        val recordedRequest = synchronized(recordedRequests) {
            recordedRequests.remove(id)
        }
        return recordedRequest
    }

    override fun additionalHeaders(url: String): JSONObject {
        val headersJson = JSONObject()
        if (url.startsWith(origin)) {
            headersJson.put(REQUEST_ID_HEADER, UUID.randomUUID().toString())
        } else {
            Log.i(LOG_TAG, "Recorded CORS to $url, not adding $REQUEST_ID_HEADER")
        }
        return headersJson
    }

    override fun setOrigin(url: String) {
        val uri = url.toUri()
        val port = if (uri.port != -1) ":${uri.port}" else ""
        origin = "${uri.scheme}://${uri.host}$port"

    }

    companion object {
        private const val LOG_TAG = "RequestUuidMatcher"
        private const val REQUEST_ID_HEADER = "x-request-inspector-id"
    }
}
