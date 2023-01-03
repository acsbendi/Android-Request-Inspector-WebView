package com.acsbendi.requestinspectorwebview

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.intellij.lang.annotations.Language
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

internal class RequestInspectorJavaScriptInterface(webView: WebView) {

    init {
        webView.addJavascriptInterface(this, INTERFACE_NAME)
    }

    private val recordedRequests = CopyOnWriteArrayList<RecordedRequest>()

    fun findRecordedRequestForUrl(url: String) =
        recordedRequests.find { url.contains(it.url) }

    data class RecordedRequest(
        val type: WebViewRequestType,
        val url: String,
        val method: String,
        val body: String,
        val headers: Map<String, String>,
        val trace: String,
        val enctype: String?
    )

    @JavascriptInterface
    fun recordFormSubmission(
        url: String,
        method: String,
        formParameterList: String,
        headers: String,
        trace: String,
        enctype: String?
    ) {
        val formParameterJsonArray = JSONArray(formParameterList)
        val headerMap = getHeadersAsMap(headers)

        val body = when (enctype) {
            "application/x-www-form-urlencoded" -> {
                headerMap["content-type"] = enctype
                getUrlEncodedFormBody(formParameterJsonArray)
            }
            "multipart/form-data" -> {
                headerMap["content-type"] = "multipart/form-data; boundary=$MULTIPART_FORM_BOUNDARY"
                getMultiPartFormBody(formParameterJsonArray)
            }
            "text/plain" -> {
                headerMap["content-type"] = enctype
                getPlainTextFormBody(formParameterJsonArray)
            }
            else -> {
                Log.e(LOG_TAG, "Incorrect encoding received from JavaScript: $enctype")
                ""
            }
        }

        Log.i(LOG_TAG, "Recorded form submission from JavaScript")
        recordedRequests.add(
            RecordedRequest(WebViewRequestType.FORM, url, method, body, headerMap, trace, enctype)
        )
    }

    @JavascriptInterface
    fun recordXhr(url: String, method: String, body: String, headers: String, trace: String) {
        Log.i(LOG_TAG, "Recorded XHR from JavaScript")
        val headerMap = getHeadersAsMap(headers)
        recordedRequests.add(
            RecordedRequest(WebViewRequestType.XML_HTTP, url, method, body, headerMap, trace, null)
        )
    }

    @JavascriptInterface
    fun recordFetch(url: String, method: String, body: String, headers: String, trace: String) {
        Log.i(LOG_TAG, "Recorded fetch from JavaScript")
        val headerMap = getHeadersAsMap(headers)
        recordedRequests.add(
            RecordedRequest(WebViewRequestType.FETCH, url, method, body, headerMap, trace, null)
        )
    }

    private fun getHeadersAsMap(headersString: String): MutableMap<String, String> {
        val headersObject = JSONObject(headersString)
        val map = HashMap<String, String>()
        for (key in headersObject.keys()) {
            val lowercaseHeader = key.lowercase(Locale.getDefault())
            map[lowercaseHeader] = headersObject.getString(key)
        }
        return map
    }

    private fun getUrlEncodedFormBody(formParameterJsonArray: JSONArray): String {
        val resultStringBuilder = StringBuilder()
        repeat(formParameterJsonArray.length()) { i ->
            val formParameter = formParameterJsonArray.get(i) as JSONObject
            val name = formParameter.getString("name")
            val value = formParameter.getString("value")
            val encodedValue = URLEncoder.encode(value, "UTF-8")
            if (i != 0) {
                resultStringBuilder.append("&")
            }
            resultStringBuilder.append(name)
            resultStringBuilder.append("=")
            resultStringBuilder.append(encodedValue)
        }
        return resultStringBuilder.toString()
    }

    private fun getMultiPartFormBody(formParameterJsonArray: JSONArray): String {
        val resultStringBuilder = StringBuilder()
        repeat(formParameterJsonArray.length()) { i ->
            val formParameter = formParameterJsonArray.get(i) as JSONObject
            val name = formParameter.getString("name")
            val value = formParameter.getString("value")
            resultStringBuilder.append("--")
            resultStringBuilder.append(MULTIPART_FORM_BOUNDARY)
            resultStringBuilder.append("\n")
            resultStringBuilder.append("Content-Disposition: form-data; name=\"$name\"")
            resultStringBuilder.append("\n\n")
            resultStringBuilder.append(value)
            resultStringBuilder.append("\n")
        }
        resultStringBuilder.append("--")
        resultStringBuilder.append(MULTIPART_FORM_BOUNDARY)
        resultStringBuilder.append("--")
        return resultStringBuilder.toString()
    }

    private fun getPlainTextFormBody(formParameterJsonArray: JSONArray): String {
        val resultStringBuilder = StringBuilder()
        repeat(formParameterJsonArray.length()) { i ->
            val formParameter = formParameterJsonArray.get(i) as JSONObject
            val name = formParameter.getString("name")
            val value = formParameter.getString("value")
            if (i != 0) {
                resultStringBuilder.append("\n")
            }
            resultStringBuilder.append(name)
            resultStringBuilder.append("=")
            resultStringBuilder.append(value)
        }
        return resultStringBuilder.toString()
    }

    companion object {
        private const val LOG_TAG = "RequestInspectorJs"
        private const val MULTIPART_FORM_BOUNDARY = "----WebKitFormBoundaryU7CgQs9WnqlZYKs6"
        private const val INTERFACE_NAME = "RequestInspection"

        @Language("JS")
        private const val JAVASCRIPT_INTERCEPTION_CODE = """
function getFullUrl(url) {
    if (url.startsWith("/")) {
        return location.protocol + '//' + location.host + url;
    } else {
        return url;
    }
}

function recordFormSubmission(form) {
    var jsonArr = [];
    for (i = 0; i < form.elements.length; i++) {
        var parName = form.elements[i].name;
        var parValue = form.elements[i].value;
        var parType = form.elements[i].type;

        jsonArr.push({
            name: parName,
            value: parValue,
            type: parType
        });
    }

    const path = form.attributes['action'] === undefined ? "/" : form.attributes['action'].nodeValue;
    const method = form.attributes['method'] === undefined ? "GET" : form.attributes['method'].nodeValue;
    const url = getFullUrl(path);
    const encType = form.attributes['enctype'] === undefined ? "application/x-www-form-urlencoded" : form.attributes['enctype'].nodeValue;
    const err = new Error();
    $INTERFACE_NAME.recordFormSubmission(
        url,
        method,
        JSON.stringify(jsonArr),
        "{}",
        err.stack,
        encType
    );
}

function handleFormSubmission(e) {
    const form = e ? e.target : this;
    recordFormSubmission(form);
    form._submit();
}

HTMLFormElement.prototype._submit = HTMLFormElement.prototype.submit;
HTMLFormElement.prototype.submit = handleFormSubmission;
window.addEventListener('submit', function (submitEvent) {
    handleFormSubmission(submitEvent);
}, true);

let lastXmlhttpRequestPrototypeMethod = null;
let xmlhttpRequestHeaders = {};
let xmlhttpRequestUrl = null;
XMLHttpRequest.prototype._open = XMLHttpRequest.prototype.open;
XMLHttpRequest.prototype.open = function (method, url, async, user, password) {
    lastXmlhttpRequestPrototypeMethod = method;
    xmlhttpRequestUrl = url;
    const asyncWithDefault = async === undefined ? true : async;
    this._open(method, url, asyncWithDefault, user, password);
};
XMLHttpRequest.prototype._setRequestHeader = XMLHttpRequest.prototype.setRequestHeader;
XMLHttpRequest.prototype.setRequestHeader = function (header, value) {
    xmlhttpRequestHeaders[header] = value;
    this._setRequestHeader(header, value);
};
XMLHttpRequest.prototype._send = XMLHttpRequest.prototype.send;
XMLHttpRequest.prototype.send = function (body) {
    const err = new Error();
    const url = getFullUrl(xmlhttpRequestUrl);
    $INTERFACE_NAME.recordXhr(
        url,
        lastXmlhttpRequestPrototypeMethod,
        body || "",
        JSON.stringify(xmlhttpRequestHeaders),
        err.stack
    );
    lastXmlhttpRequestPrototypeMethod = null;
    xmlhttpRequestUrl = null;
    xmlhttpRequestHeaders = {};
    this._send(body);
};

window._fetch = window.fetch;
window.fetch = function () {
    const url = arguments[1] && 'url' in arguments[1] ? arguments[1]['url'] : "/";
    const fullUrl = getFullUrl(url);
    const method = arguments[1] && 'method' in arguments[1] ? arguments[1]['method'] : "GET";
    const body = arguments[1] && 'body' in arguments[1] ? arguments[1]['body'] : "";
    const headers = JSON.stringify(arguments[1] && 'headers' in arguments[1] ? arguments[1]['headers'] : {});
    let err = new Error();
    $INTERFACE_NAME.recordFetch(fullUrl, method, body, headers, err.stack);
    return window._fetch.apply(this, arguments);
}
        """

        fun enabledRequestInspection(webView: WebView, extraJavaScriptToInject: String) {
            webView.evaluateJavascript(
                "javascript: $JAVASCRIPT_INTERCEPTION_CODE\n$extraJavaScriptToInject",
                null
            )
        }
    }
}
