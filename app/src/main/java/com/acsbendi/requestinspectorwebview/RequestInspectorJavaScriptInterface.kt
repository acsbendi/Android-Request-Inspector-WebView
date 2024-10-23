package com.acsbendi.requestinspectorwebview

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.intellij.lang.annotations.Language
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale

internal class RequestInspectorJavaScriptInterface(webView: WebView) {

    init {
        webView.addJavascriptInterface(this, INTERFACE_NAME)
    }

    private val recordedRequests = ArrayList<RecordedRequest>()

    fun findRecordedRequestForUrl(url: String): RecordedRequest? {
        return synchronized(recordedRequests) {
            // use findLast instead of find to find the last added query matching a URL -
            // they are included at the end of the list when written.
            recordedRequests.findLast { recordedRequest ->
                // Added search by exact URL to find the actual request body
                url == recordedRequest.url
            } ?: recordedRequests.findLast { recordedRequest ->
                // Previously, there was only a search by contains, and because of this, sometimes the wrong request body was found
                url.contains(recordedRequest.url)
            }
        }
    }

    data class RecordedRequest(
        val type: WebViewRequestType,
        val url: String,
        val method: String,
        val body: String,
        val formParameters: Map<String, String>,
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
        val formParameterMap = getFormParametersAsMap(formParameterJsonArray)

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
        addRecordedRequest(
            RecordedRequest(
                WebViewRequestType.FORM,
                url,
                method,
                body,
                formParameterMap,
                headerMap,
                trace,
                enctype
            )
        )
    }

    @JavascriptInterface
    fun recordXhr(url: String, method: String, body: String, headers: String, trace: String) {
        Log.i(LOG_TAG, "Recorded XHR from JavaScript")
        val headerMap = getHeadersAsMap(headers)
        addRecordedRequest(
            RecordedRequest(
                WebViewRequestType.XML_HTTP,
                url,
                method,
                body,
                mapOf(),
                headerMap,
                trace,
                null
            )
        )
    }

    @JavascriptInterface
    fun recordFetch(url: String, method: String, body: String, headers: String, trace: String) {
        Log.i(LOG_TAG, "Recorded fetch from JavaScript")
        val headerMap = getHeadersAsMap(headers)
        addRecordedRequest(
            RecordedRequest(
                WebViewRequestType.FETCH,
                url,
                method,
                body,
                mapOf(),
                headerMap,
                trace,
                null
            )
        )
    }

    private fun addRecordedRequest(recordedRequest: RecordedRequest) {
        synchronized(recordedRequests) {
            recordedRequests.add(recordedRequest)
        }
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

    private fun getFormParametersAsMap(formParameterJsonArray: JSONArray): Map<String, String> {
        val map = HashMap<String, String>()
        repeat(formParameterJsonArray.length()) { i ->
            val formParameter = formParameterJsonArray.get(i) as JSONObject
            val name = formParameter.getString("name")
            val value = formParameter.optString("value")
            val checked = formParameter.optBoolean("checked")
            val type = formParameter.optString("type")
            if (!isExcludedFormParameter(type, checked)) {
                map[name] = value
            }
        }
        return map
    }


    private fun getUrlEncodedFormBody(formParameterJsonArray: JSONArray): String {
        val resultStringBuilder = StringBuilder()
        repeat(formParameterJsonArray.length()) { i ->
            val formParameter = formParameterJsonArray.get(i) as JSONObject
            val name = formParameter.getString("name")
            val value = formParameter.optString("value")
            val checked = formParameter.optBoolean("checked")
            val type = formParameter.optString("type")
            val encodedValue = URLEncoder.encode(value, "UTF-8")

            if (!isExcludedFormParameter(type, checked)) {
                if (i != 0) {
                    resultStringBuilder.append("&")
                }
                resultStringBuilder.append(name)
                resultStringBuilder.append("=")
                resultStringBuilder.append(encodedValue)
            }


        }
        return resultStringBuilder.toString()
    }

    private fun getMultiPartFormBody(formParameterJsonArray: JSONArray): String {
        val resultStringBuilder = StringBuilder()
        repeat(formParameterJsonArray.length()) { i ->
            val formParameter = formParameterJsonArray.get(i) as JSONObject
            val name = formParameter.getString("name")
            val value = formParameter.optString("value")
            val checked = formParameter.optBoolean("checked")
            val type = formParameter.optString("type")

            if (!isExcludedFormParameter(type, checked)) {
                resultStringBuilder.append("--")
                resultStringBuilder.append(MULTIPART_FORM_BOUNDARY)
                resultStringBuilder.append("\n")
                resultStringBuilder.append("Content-Disposition: form-data; name=\"$name\"")
                resultStringBuilder.append("\n\n")
                resultStringBuilder.append(value)
                resultStringBuilder.append("\n")
            }

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
            val value = formParameter.optString("value")
            val checked = formParameter.optBoolean("checked")
            val type = formParameter.optString("type")

            if (!isExcludedFormParameter(type, checked)) {
                if (i != 0) {
                    resultStringBuilder.append("\n")
                }
                resultStringBuilder.append(name)
                resultStringBuilder.append("=")
                resultStringBuilder.append(value)
            }

        }
        return resultStringBuilder.toString()
    }

    private fun isExcludedFormParameter(type: String, checked: Boolean): Boolean {
        return (type == "radio" || type == "checkbox") && !checked
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
        var parChecked = form.elements[i].checked;
        var parId = form.elements[i].id;

        jsonArr.push({
            name: parName,
            value: parValue,
            type: parType,
            checked:parChecked,
            id:parId
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
    const form = submitEvent ? submitEvent.target : this;
    recordFormSubmission(form);
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
    const firstArgument = arguments[0];
    let url;
    let method;
    let body;
    let headers;
    if (typeof firstArgument === 'string') {
        url = firstArgument;
        method = arguments[1] && 'method' in arguments[1] ? arguments[1]['method'] : "GET";
        body = arguments[1] && 'body' in arguments[1] ? arguments[1]['body'] : "";
        headers = JSON.stringify(arguments[1] && 'headers' in arguments[1] ? arguments[1]['headers'] : {});
    } else {
        // Request object
        url = firstArgument.url;
        method = firstArgument.method;
        body = firstArgument.body;
        headers = JSON.stringify(Object.fromEntries(firstArgument.headers.entries()));
    }
    const fullUrl = getFullUrl(url);
    const err = new Error();
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
