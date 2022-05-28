package com.acsbendi.requestinspectorwebview

import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebResourceRequest

data class WebViewRequest(
    val type: WebViewRequestType,
    val url: String,
    val method: String,
    val body: String,
    val headers: Map<String, String>,
    val trace: String,
    val enctype: String?,
    val isForMainFrame: Boolean,
    val isRedirect: Boolean,
    val hasGesture: Boolean
) {
    override fun toString(): String {
        val headersString = headers.entries.joinToString("\n") { (key, value) ->
            "   $key: $value"
        }
        val traceWithIndent = "   $trace"
        return """
            Type: $type
            URL: $url
            Method: $method
            Body: $body
            Headers: 
                $headersString
            Trace:
                $traceWithIndent
            Encoding type (form submissions only): $enctype
            Is for main frame? $isForMainFrame
            Is redirect? $isRedirect
            Has gesture? $hasGesture
        """.trimIndent()
    }

    companion object {
        internal fun create(
            webResourceRequest: WebResourceRequest,
            recordedRequest: RequestInspectorJavaScriptInterface.RecordedRequest?
        ): WebViewRequest {
            val type = recordedRequest?.type ?: WebViewRequestType.HTML
            val url = webResourceRequest.url.toString()
            val cookies = CookieManager.getInstance().getCookie(url)
            val headers = HashMap<String, String>()
            headers["cookie"] = cookies
            if (recordedRequest != null) {
                headers.putAll(recordedRequest.headers)
            }
            headers.putAll(webResourceRequest.requestHeaders)

            val isRedirect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                webResourceRequest.isRedirect
            } else {
                false
            }
            return WebViewRequest(
                type = type,
                url = url,
                method = webResourceRequest.method,
                body = recordedRequest?.body ?: "",
                headers = headers,
                trace = recordedRequest?.trace ?: "",
                enctype = recordedRequest?.enctype,
                isForMainFrame = webResourceRequest.isForMainFrame,
                isRedirect = isRedirect,
                hasGesture = webResourceRequest.hasGesture()
            )
        }
    }
}
