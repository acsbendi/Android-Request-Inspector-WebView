package com.acsbendi.requestinspectorwebview.matcher

import android.webkit.WebResourceRequest
import com.acsbendi.requestinspectorwebview.RequestInspectorJavaScriptInterface
import java.util.UUID

class RequestGeneratedUuidInQueryParamMatcher : RequestGeneratedUuidMatcher() {

    override fun getUuidFromRequest(recordedRequest: RequestInspectorJavaScriptInterface.RecordedRequest): String? =
        recordedRequest.url.getQueryParameter(REQUEST_INSPECTOR_ID)

    override fun getUuidFromRequest(webResourceRequest: WebResourceRequest): String? =
        webResourceRequest.url.getQueryParameter(REQUEST_INSPECTOR_ID)

    override fun removeUuidFromRequests(
        request: WebResourceRequest,
        recordedRequest: RequestInspectorJavaScriptInterface.RecordedRequest?
    ): Pair<WebResourceRequest, RequestInspectorJavaScriptInterface.RecordedRequest?> {
        val originalUrl = request.url
        val cleanedUrlBuilder = originalUrl.buildUpon().clearQuery()
        for (key in originalUrl.queryParameterNames.filter { it != REQUEST_INSPECTOR_ID }) {
            originalUrl.getQueryParameters(key).forEach { paramValue ->
                cleanedUrlBuilder.appendQueryParameter(key, paramValue)
            }
        }
        val cleanedUrl = cleanedUrlBuilder.build()

        val cleanedWebResourceRequest = object : WebResourceRequest by request {
            override fun getUrl() = cleanedUrl
        }
        val cleanedRecordedRequest = recordedRequest?.copy(url = cleanedUrl)
        return cleanedWebResourceRequest to cleanedRecordedRequest
    }

    override fun getAdditionalQueryParams(): String {
        val uuid = UUID.randomUUID().toString()
        return "$REQUEST_INSPECTOR_ID=$uuid"
    }
}
