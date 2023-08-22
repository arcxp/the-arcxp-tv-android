package com.arcxp.thearcxptv.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.arcxp.thearcxptv.databinding.FragmentWebBinding

/**
 * Fragment with WebView.
 */
class WebFragment : Fragment() {

    private var _binding: FragmentWebBinding? = null
    private val binding get() = _binding!!

    /**
     * @url full URL to a web page
     */
    fun openUrl(url: String?, name: String?): WebFragment {
        val arg = arguments ?: Bundle()
        arg.putString(ARG_URL, url)
        arguments = arg
        return this
    }

    private fun getUrl(): String {
        return arguments?.getString(ARG_URL) ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWebBinding.inflate(inflater, container, false)

        binding.progressBar.visibility = View.VISIBLE
        onWebViewReady(binding.root, binding.webView)
        return binding.root
    }

    private fun onWebViewReady(view: View, webView: WebView) {
        binding.webView.webChromeClient = createWebChromeClient()
        binding.webView.webViewClient = WebViewClient()
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.allowContentAccess = true
        binding.webView.settings.domStorageEnabled = true
        binding.webView.settings.loadWithOverviewMode = true
        binding.webView.settings.useWideViewPort = true
        binding.webView.loadUrl(getUrl())
        binding.webView.requestFocus()
    }

    private fun createWebChromeClient(): WebChromeClient {
        return object : WebChromeClient() {

            var customView: View? = null
            private var originalSystemUiVisibility: Int = 0
            private var originalOrientation: Int = 0
            private var customViewCallback: CustomViewCallback? = null

//            override fun onProgressChanged(view: WebView?, newProgress: Int) {
//                super.onProgressChanged(view, newProgress)
//                onProgressChanged(newProgress)
//            }

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                super.onShowCustomView(view, callback)
                customView = view
                val activity = activity ?: return

                originalSystemUiVisibility = activity.window.decorView.systemUiVisibility
                originalOrientation = activity.requestedOrientation
                customViewCallback = callback
                (activity.window.decorView as FrameLayout).addView(customView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
                activity.window.decorView.systemUiVisibility =
                        SYSTEM_UI_FLAG_IMMERSIVE or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                                SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or SYSTEM_UI_FLAG_LAYOUT_STABLE or
                                SYSTEM_UI_FLAG_FULLSCREEN or SYSTEM_UI_FLAG_HIDE_NAVIGATION
            }

            override fun onHideCustomView() {
                super.onHideCustomView()
                val activity = activity ?: return
                (activity.window.decorView as FrameLayout).removeView(customView)
                customView = null
                activity.window.decorView.systemUiVisibility = this.originalSystemUiVisibility
                activity.requestedOrientation = this.originalOrientation
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

//    fun onProgressChanged(newProgress: Int) {
//        binding.progressBar.progress = newProgress
//        if (newProgress == 100) {
//            binding.progressBar.visibility = View.GONE
//        } else {
//            binding.progressBar.visibility = View.VISIBLE
//        }
//    }

    companion object {
        const val ARG_URL = "ARG_URL"
    }
}
