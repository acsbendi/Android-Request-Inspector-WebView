package com.acsbendi.requestinspectorwebview

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.acsbendi.requestinspectorwebview.RequestInspectorJavaScriptInterface.RecordedRequest
import com.acsbendi.requestinspectorwebview.matcher.RequestMatcher
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class JavaScriptInterfaceTest {

    private lateinit var webView: WebView
    private lateinit var matcher: CapturingRequestMatcher

    @SuppressLint("SetJavaScriptEnabled")
    @Before
    fun setUp() {
        val pageLatch = CountDownLatch(1)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            webView = WebView(context)
            matcher = CapturingRequestMatcher()
            webView.webViewClient = object : RequestInspectorWebViewClient(webView, matcher) {
                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    pageLatch.countDown()
                }
            }
            webView.loadUrl("file:///android_asset/test_page.html")
        }
        assertTrue("Test page failed to load", pageLatch.await(5, TimeUnit.SECONDS))
    }

    @After
    fun tearDown() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { webView.destroy() }
    }

    // - fetch (string URL) -----------------------------------------

    @Test
    fun fetch_stringUrl_recordsTypeUrlMethodBodyAndHeaders() {
        val latch = matcher.expectRequest()
        runJs("triggerFetchStringUrl()")
        assertTrue("fetch (string URL) not recorded", latch.await(5, TimeUnit.SECONDS))

        val req = matcher.lastRequest!!
        assertEquals(WebViewRequestType.FETCH, req.type)
        assertEquals("https://example.com/api/data", req.url.toString())
        assertEquals("POST", req.method)
        assertEquals("{\"key\":\"value\"}", req.body)
        assertEquals("application/json", req.headers["content-type"])
        assertEquals("test-value", req.headers["x-custom"])
    }

    @Test
    fun fetch_stringUrl_injectsAdditionalHeaders() {
        matcher.additionalHeaders = mapOf("X-Injected" to "injected-value")
        val latch = matcher.expectRequest()
        runJs("triggerFetchStringUrl()")
        assertTrue("fetch (string URL) with extra header not recorded", latch.await(5, TimeUnit.SECONDS))

        assertEquals("injected-value", matcher.lastRequest!!.headers["x-injected"])
    }

    @Test
    fun fetch_stringUrl_appendsAdditionalQueryParam() {
        matcher.additionalQueryParam = "debug=true"
        val latch = matcher.expectRequest()
        runJs("triggerFetchStringUrl()")
        assertTrue("fetch (string URL) with query param not recorded", latch.await(5, TimeUnit.SECONDS))

        val url = matcher.lastRequest!!.url.toString()
        assertTrue("Expected URL to contain 'debug=true', was: $url", url.contains("debug=true"))
    }

    // - fetch (request object) -------------------------------------

    @Test
    fun fetch_requestObject_recordsTypeUrlMethodAndHeaders() {
        val latch = matcher.expectRequest()
        runJs("triggerFetchRequestObject()")
        assertTrue("fetch (Request object) not recorded", latch.await(5, TimeUnit.SECONDS))

        val req = matcher.lastRequest!!
        assertEquals(WebViewRequestType.FETCH, req.type)
        assertEquals("https://example.com/api/request", req.url.toString())
        assertEquals("PUT", req.method)
        assertEquals("request-value", req.headers["x-request-header"])
    }

    @Test
    fun fetch_requestObject_injectsAdditionalHeaders() {
        matcher.additionalHeaders = mapOf("X-Injected" to "injected-value")
        val latch = matcher.expectRequest()
        runJs("triggerFetchRequestObject()")
        assertTrue("fetch (Request object) with extra header not recorded", latch.await(5, TimeUnit.SECONDS))

        assertEquals("injected-value", matcher.lastRequest!!.headers["x-injected"])
    }

    // - XHR --------------------------------------------------------

    @Test
    fun xhr_recordsTypeUrlMethodBodyAndHeaders() {
        val latch = matcher.expectRequest()
        runJs("triggerXhr()")
        assertTrue("XHR not recorded", latch.await(5, TimeUnit.SECONDS))

        val req = matcher.lastRequest!!
        assertEquals(WebViewRequestType.XML_HTTP, req.type)
        assertEquals("https://example.com/api/xhr", req.url.toString())
        assertEquals("POST", req.method)
        assertEquals("xhr-body-content", req.body)
        assertEquals("text/plain", req.headers["content-type"])
        assertEquals("xhr-value", req.headers["x-xhr-header"])
    }

    @Test
    fun xhr_injectsAdditionalHeaders() {
        matcher.additionalHeaders = mapOf("X-Injected" to "injected-value")
        val latch = matcher.expectRequest()
        runJs("triggerXhr()")
        assertTrue("XHR with extra header not recorded", latch.await(5, TimeUnit.SECONDS))

        assertEquals("injected-value", matcher.lastRequest!!.headers["x-injected"])
    }

    // - form input -------------------------------------------------
    @Test
    fun formGet_recordsTypeUrlMethodAndFormParameters() {
        val latch = matcher.expectRequest()
        runJs("submitFormGet()")
        assertTrue("Form GET not recorded", latch.await(5, TimeUnit.SECONDS))

        val req = matcher.lastRequest!!
        assertEquals(WebViewRequestType.FORM, req.type)
        assertEquals("https://example.com/form-get", req.url.toString())
        assertEquals("GET", req.method)
        assertEquals("hello", req.formParameters["query"])
        assertEquals("test", req.formParameters["source"])
    }

    @Test
    fun formPostUrlEncoded_recordsBodyAndContentType() {
        val latch = matcher.expectRequest()
        runJs("submitFormPostUrlEncoded()")
        assertTrue("Form POST (url-encoded) not recorded", latch.await(5, TimeUnit.SECONDS))

        val req = matcher.lastRequest!!
        assertEquals(WebViewRequestType.FORM, req.type)
        assertEquals("https://example.com/form-post-urlencoded", req.url.toString())
        assertEquals("POST", req.method)
        assertEquals("username=testuser&password=secret123", req.body)
        assertEquals("application/x-www-form-urlencoded", req.headers["content-type"])
    }

    @Test
    fun formPostMultipart_recordsBodyAndContentType() {
        val latch = matcher.expectRequest()
        runJs("submitFormPostMultipart()")
        assertTrue("Form POST (multipart) not recorded", latch.await(5, TimeUnit.SECONDS))

        val req = matcher.lastRequest!!
        assertEquals(WebViewRequestType.FORM, req.type)
        assertEquals("https://example.com/form-post-multipart", req.url.toString())
        assertEquals("POST", req.method)
        assertTrue(
            "content-type should start with multipart/form-data",
            req.headers["content-type"]!!.startsWith("multipart/form-data; boundary=")
        )
        assertTrue(req.body.contains("field1") && req.body.contains("value1"))
        assertTrue(req.body.contains("field2") && req.body.contains("value2"))
    }

    @Test
    fun formPostPlainText_recordsBodyAndContentType() {
        val latch = matcher.expectRequest()
        runJs("submitFormPostPlainText()")
        assertTrue("Form POST (plain text) not recorded", latch.await(5, TimeUnit.SECONDS))

        val req = matcher.lastRequest!!
        assertEquals(WebViewRequestType.FORM, req.type)
        assertEquals("https://example.com/form-post-plaintext", req.url.toString())
        assertEquals("POST", req.method)
        assertEquals("message=hello world", req.body)
        assertEquals("text/plain", req.headers["content-type"])
    }

    private fun runJs(script: String) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            webView.evaluateJavascript(script, null)
        }
    }
}

class CapturingRequestMatcher : RequestMatcher {

    private val requests = ArrayBlockingQueue<RecordedRequest>(64)
    val lastRequest: RecordedRequest? get() = requests.lastOrNull()

    private val pendingLatches = ArrayDeque<CountDownLatch>()

    var additionalHeaders: Map<String, String> = emptyMap()
    var additionalQueryParam: String = ""

    /** Returns a latch that counts down when the next request is recorded. */
    fun expectRequest(): CountDownLatch {
        val latch = CountDownLatch(1)
        synchronized(pendingLatches) { pendingLatches.addLast(latch) }
        return latch
    }

    override fun addRecordedRequest(recordedRequest: RecordedRequest) {
        requests.add(recordedRequest)
        synchronized(pendingLatches) { pendingLatches.removeFirstOrNull()?.countDown() }
    }

    override fun createWebViewRequest(request: WebResourceRequest): WebViewRequest {
        val recorded = requests.lastOrNull { it.url.toString() == request.url.toString() }
        return WebViewRequest.create(request, recorded)
    }

    override fun getAdditionalHeaders(url: String): JSONObject {
        val obj = JSONObject()
        additionalHeaders.forEach { (k, v) -> obj.put(k, v) }
        return obj
    }

    override fun getAdditionalQueryParams(): String = additionalQueryParam
}
