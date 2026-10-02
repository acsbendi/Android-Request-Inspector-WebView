package com.acsbendi.requestinspectorwebview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.webkit.WebViewFeature
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class EarlyRequestTest {

    private lateinit var webView: WebView
    private lateinit var matcher: CapturingRequestMatcher

    @After
    fun tearDown() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { webView.destroy() }
    }

    /**
     * Verifies that a fetch() fired by an inline <script> — which executes before
     * onPageStarted() can inject the interceptor via evaluateJavascript() — is still
     * recorded with full JS details when DOCUMENT_START_SCRIPT is supported.
     *
     * The 500 ms delay in onPageStarted() forces the race deterministically: inline
     * scripts always fire before the fallback evaluateJavascript() injection runs.
     * The test therefore only passes when the fix registers the hooks at document
     * creation time via addDocumentStartJavaScript() in the constructor, before any
     * page script executes.
     */
    @SuppressLint("SetJavaScriptEnabled")
    @Test
    fun earlyFetch_withDelayedFallbackInjection_isCapturedByDocumentStartScript() {
        assumeTrue(
            "Skipping: DOCUMENT_START_SCRIPT not supported on this device",
            WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
        )

        matcher = CapturingRequestMatcher()
        val earlyRequestLatch = matcher.expectRequest()
        val pageFinishedLatch = CountDownLatch(1)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            webView = WebView(context)

            webView.webViewClient = object : RequestInspectorWebViewClient(webView, matcher) {
                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    // Delay the evaluateJavascript fallback injection so that inline
                    // page scripts always fire before the hooks could be installed that
                    // way. On devices with DOCUMENT_START_SCRIPT the hooks were already
                    // registered in the constructor, so this delay is irrelevant there.
                    Handler(Looper.getMainLooper()).postDelayed({
                        super.onPageStarted(view, url, favicon)
                    }, 500)
                }

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    pageFinishedLatch.countDown()
                }
            }
            webView.loadUrl("file:///android_asset/test_page_early_request.html")
        }

        assertTrue("Page failed to load", pageFinishedLatch.await(15, TimeUnit.SECONDS))
        assertTrue(
            "Early fetch JS details were not recorded — hooks were not installed before inline scripts ran",
            earlyRequestLatch.await(5, TimeUnit.SECONDS)
        )

        val req = matcher.lastRequest!!
        assertEquals(WebViewRequestType.FETCH, req.type)
        assertEquals("https://example.com/api/early-fetch", req.url.toString())
        assertEquals("POST", req.method)
        assertEquals("{\"early\":true}", req.body)
        assertEquals("application/json", req.headers["content-type"])
    }
}
