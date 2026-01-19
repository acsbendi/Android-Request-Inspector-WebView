package com.acsbendi.requestinspectorwebview.matcher

import android.util.Log
import android.webkit.WebResourceRequest
import androidx.core.net.toUri
import com.acsbendi.requestinspectorwebview.RequestInspectorJavaScriptInterface.RecordedRequest
import org.json.JSONObject
import java.util.UUID

/**
 * This matcher only works for NON CORS requests. It adds a unique UUID header to each request
 * originating from the WebView, and matches recorded requests based on that header.
 *
 * It doesn't work for CORS requests, because it changes the headers of the request, which influences the preflight
 * request checking for allowed headers. Even when cleaning up the headers after the request is matched with it's body,
 * the CORS request will fail because the browser engine only knows about the adapted header and doesn't execute the
 * CORS request, because the preflight check doesn't return the custom header as allowed.
 */
class RequestGeneratedUuidInHeaderMatcher() : RequestGeneratedUuidMatcher() {

    private var origin: String = ""

    override fun getUuidFromRequest(recordedRequest: RecordedRequest): String? =
        recordedRequest.headers[REQUEST_INSPECTOR_ID]

    override fun getUuidFromRequest(webResourceRequest: WebResourceRequest): String? =
        webResourceRequest.requestHeaders[REQUEST_INSPECTOR_ID]

    override fun removeUuidFromRequests(request: WebResourceRequest, recordedRequest: RecordedRequest?): Pair<WebResourceRequest, RecordedRequest?> {
        // Clean up headers by removing REQUEST_ID_HEADER from both requests
        val cleanedRequest = object : WebResourceRequest by request {
            override fun getRequestHeaders(): Map<String, String> =
                request.requestHeaders.filter { (key, _) -> key != REQUEST_INSPECTOR_ID }
        }
        val cleanedRecordedRequest = recordedRequest?.copy(
            headers = recordedRequest.headers.filter { (key, _) -> key != REQUEST_INSPECTOR_ID }
        )
        return cleanedRequest to cleanedRecordedRequest
    }

    override fun getAdditionalHeaders(url: String): JSONObject {
        val headersJson = JSONObject()
        if (getOrigin(url) == origin) {
            val uuid = UUID.randomUUID().toString()
            headersJson.put(REQUEST_INSPECTOR_ID, uuid)
        } else {
            Log.i(LOG_TAG, "Recorded CORS to $url, not adding $REQUEST_INSPECTOR_ID")
        }
        return headersJson
    }

    override fun onPageStarted(url: String) {
        origin = getOrigin(url)
    }

    private fun getOrigin(url: String): String {
        val uri = url.toUri()
        val port = if (uri.port != -1) ":${uri.port}" else ""
        return "${uri.scheme}://${uri.host}$port"
    }
}
