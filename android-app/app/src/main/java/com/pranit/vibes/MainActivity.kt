package com.pranit.vibes

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webChromeClient = WebChromeClient()
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                return if (uri.scheme == "https" && (uri.host?.endsWith("jiosaavn.com") == true || uri.host == "pranitdandewarofficial.github.io")) {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                    true
                } else false
            }
        }
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
    }
}
