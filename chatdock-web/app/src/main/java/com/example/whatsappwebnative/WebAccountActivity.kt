package com.example.whatsappwebnative

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.print.PrintManager
import android.view.View
import android.webkit.WebView.HitTestResult
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.MimeTypeMap
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import java.net.URLEncoder

abstract class WebAccountActivity : AppCompatActivity() {

    protected abstract val accountSlot: AccountSlot

    private lateinit var webView: WebView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var titleView: TextView
    private lateinit var fileChooserLauncher: ActivityResultLauncher<android.content.Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var pendingWebRtcPermissionRequest: PermissionRequest? = null
    private var pendingDownload: PendingDownload? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        configureIsolatedWebViewProfile()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_account)

        webView = findViewById(R.id.webView)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        titleView = findViewById(R.id.accountTitle)
        titleView.text = AccountNameStore.getName(this, accountSlot)
        findViewById<View>(R.id.switchAccountsButton).setOnClickListener { finish() }
        findViewById<View>(R.id.accountMenuButton).apply {
            setOnClickListener { openOfficialWhatsAppChatMenu() }
            setOnLongClickListener {
                showAccountMenu(this)
                true
            }
        }

        registerActivityResultLaunchers()
        configureWebView()
        configureBackNavigation()

        if (savedInstanceState == null) {
            webView.loadUrl(WHATSAPP_WEB_URL)
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onResume() {
        super.onResume()
        enforceScreenLock()
        webView.onResume()
    }

    override fun onPause() {
        webView.onPause()
        CookieManager.getInstance().flush()
        super.onPause()
    }

    override fun onDestroy() {
        filePathCallback?.onReceiveValue(null)
        filePathCallback = null
        pendingWebRtcPermissionRequest = null
        webView.destroy()
        super.onDestroy()
    }

    private fun configureIsolatedWebViewProfile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !dataDirectoryConfigured) {
            WebView.setDataDirectorySuffix(accountSlot.suffix)
            dataDirectoryConfigured = true
        }
    }

    private fun registerActivityResultLaunchers() {
        fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val callback = filePathCallback ?: return@registerForActivityResult
            filePathCallback = null
            val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            callback.onReceiveValue(uris)
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            handleWebRtcPermissionResult(grants)
            handlePendingDownloadAfterPermission()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webView.addJavascriptInterface(
            WebAppInterface(this) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !hasPermission(Manifest.permission.POST_NOTIFICATIONS)
                ) {
                    runOnUiThread {
                        permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                    }
                }
            },
            WebAppInterface.JS_NAME
        )

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            enableLegacyAppCacheIfAvailable()
            cacheMode = WebSettings.LOAD_DEFAULT
            loadsImagesAutomatically = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowContentAccess = true
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = DESKTOP_USER_AGENT
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(webView.settings, WebSettingsCompat.FORCE_DARK_OFF)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                return if (uri.host == "web.whatsapp.com" || uri.host?.endsWith(".whatsapp.com") == true) {
                    false
                } else {
                    view.loadUrl(uri.toString())
                    true
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                loadingIndicator.visibility = View.GONE
                injectNotificationBridge()
                CookieManager.getInstance().flush()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                loadingIndicator.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread { handleWebRtcPermissionRequest(request) }
            }

            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: WebChromeClient.FileChooserParams
            ): Boolean {
                this@WebAccountActivity.filePathCallback?.onReceiveValue(null)
                this@WebAccountActivity.filePathCallback = filePathCallback

                val intent = fileChooserParams.createIntent().apply {
                    addCategory(android.content.Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(
                        android.content.Intent.EXTRA_MIME_TYPES,
                        arrayOf("image/*", "video/*", "audio/*", "application/pdf", "text/*", "application/zip")
                    )
                    putExtra(android.content.Intent.EXTRA_ALLOW_MULTIPLE, fileChooserParams.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE)
                }

                return try {
                    fileChooserLauncher.launch(intent)
                    true
                } catch (_: Exception) {
                    this@WebAccountActivity.filePathCallback = null
                    filePathCallback.onReceiveValue(null)
                    Toast.makeText(this@WebAccountActivity, R.string.no_file_picker, Toast.LENGTH_SHORT).show()
                    false
                }
            }
        }

        webView.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            val download = PendingDownload(url, userAgent, contentDisposition, mimeType)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && !hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                pendingDownload = download
                permissionLauncher.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            } else {
                enqueueDownload(download)
            }
        })

        webView.setOnLongClickListener {
            val result = webView.hitTestResult
            val extra = result.extra
            if (!extra.isNullOrBlank() && isDownloadableHit(result.type)) {
                enqueueDownload(PendingDownload(extra, webView.settings.userAgentString, "", guessMimeType(extra)))
                true
            } else {
                false
            }
        }
    }

    private fun showAccountMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add(R.string.open_whatsapp_chat_menu)
            menu.add(R.string.open_in_browser)
            menu.add(R.string.print_page)
            menu.add(R.string.translate_page)
            menu.add(R.string.reload_page)
            menu.add(R.string.share_page)
            menu.add(R.string.lock_now)
            setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    getString(R.string.open_whatsapp_chat_menu) -> openOfficialWhatsAppChatMenu()
                    getString(R.string.open_in_browser) -> openCurrentPageInBrowser()
                    getString(R.string.print_page) -> printCurrentPage()
                    getString(R.string.translate_page) -> translateCurrentPage()
                    getString(R.string.reload_page) -> webView.reload()
                    getString(R.string.share_page) -> shareCurrentPage()
                    getString(R.string.lock_now) -> lockNow()
                }
                true
            }
            show()
        }
    }

    private fun openOfficialWhatsAppChatMenu() {
        webView.evaluateJavascript(OPEN_WHATSAPP_CHAT_MENU_SCRIPT) { result ->
            if (result != "true") {
                Toast.makeText(this, R.string.whatsapp_chat_menu_unavailable, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun lockNow() {
        ScreenLockStore.lockNow()
        startActivity(Intent(this, LockActivity::class.java))
    }

    private fun enforceScreenLock() {
        if (ScreenLockStore.isEnabled(this) && !ScreenLockStore.sessionUnlocked) {
            startActivity(Intent(this, LockActivity::class.java))
        }
    }

    private fun openCurrentPageInBrowser() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webView.url ?: WHATSAPP_WEB_URL)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.open_in_browser)))
    }

    private fun printCurrentPage() {
        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        val adapter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webView.createPrintDocumentAdapter("${getString(R.string.app_name)} ${accountSlot.number}")
        } else {
            @Suppress("DEPRECATION")
            webView.createPrintDocumentAdapter()
        }
        printManager.print(getString(R.string.print_job_name), adapter, null)
    }

    private fun translateCurrentPage() {
        val encodedUrl = URLEncoder.encode(webView.url ?: WHATSAPP_WEB_URL, "UTF-8")
        val translateUrl = "https://translate.google.com/translate?sl=auto&tl=en&u=$encodedUrl"
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(translateUrl)))
    }

    private fun shareCurrentPage() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, webView.url ?: WHATSAPP_WEB_URL)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_page)))
    }

    private fun handleWebRtcPermissionRequest(request: PermissionRequest) {
        val neededPermissions = mutableListOf<String>()

        request.resources.forEach { resource ->
            when (resource) {
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> if (!hasPermission(Manifest.permission.CAMERA)) {
                    neededPermissions += Manifest.permission.CAMERA
                }
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> if (!hasPermission(Manifest.permission.RECORD_AUDIO)) {
                    neededPermissions += Manifest.permission.RECORD_AUDIO
                }
            }
        }

        if (neededPermissions.isEmpty()) {
            val officialWebRtcResources = request.resources.filter { resource ->
                resource == PermissionRequest.RESOURCE_VIDEO_CAPTURE ||
                    resource == PermissionRequest.RESOURCE_AUDIO_CAPTURE
            }.toTypedArray()

            if (officialWebRtcResources.isEmpty()) {
                request.deny()
            } else {
                request.grant(officialWebRtcResources)
            }
        } else {
            pendingWebRtcPermissionRequest = request
            permissionLauncher.launch(neededPermissions.distinct().toTypedArray())
        }
    }

    private fun handleWebRtcPermissionResult(grants: Map<String, Boolean>) {
        val webRtcRequest = pendingWebRtcPermissionRequest ?: return
        pendingWebRtcPermissionRequest = null

        val resourcesToGrant = webRtcRequest.resources.filter { resource ->
            when (resource) {
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> grants[Manifest.permission.CAMERA] == true || hasPermission(Manifest.permission.CAMERA)
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> grants[Manifest.permission.RECORD_AUDIO] == true || hasPermission(Manifest.permission.RECORD_AUDIO)
                else -> false
            }
        }.toTypedArray()

        if (resourcesToGrant.isNotEmpty()) {
            webRtcRequest.grant(resourcesToGrant)
        } else {
            webRtcRequest.deny()
        }
    }

    private fun handlePendingDownloadAfterPermission() {
        pendingDownload?.let { download ->
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P || hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                enqueueDownload(download)
                pendingDownload = null
            }
        }
    }

    private fun configureBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun enqueueDownload(download: PendingDownload) {
        val cookie = CookieManager.getInstance().getCookie(download.url).orEmpty()
        val guessedName = URLUtil.guessFileName(download.url, download.contentDisposition, download.mimeType)
        val mimeType = download.mimeType.ifBlank { guessMimeType(guessedName) }

        val request = DownloadManager.Request(Uri.parse(download.url)).apply {
            setTitle(guessedName)
            setDescription(getString(R.string.download_description))
            setMimeType(mimeType)
            addRequestHeader("Cookie", cookie)
            addRequestHeader("User-Agent", download.userAgent)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "ChatDock Web/$guessedName")
        }

        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        Toast.makeText(this, R.string.download_started, Toast.LENGTH_SHORT).show()
    }

    private fun injectNotificationBridge() {
        webView.evaluateJavascript(NOTIFICATION_BRIDGE_SCRIPT, null)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun WebSettings.enableLegacyAppCacheIfAvailable() {
        try {
            val booleanType = Boolean::class.javaPrimitiveType ?: Boolean::class.java
            javaClass.getMethod("setAppCacheEnabled", booleanType).invoke(this, true)
        } catch (_: ReflectiveOperationException) {
            // Modern Android WebView removed AppCache; DOM/database storage and HTTP cache remain enabled.
        }
    }

    private fun guessMimeType(fileName: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(fileName)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: "application/octet-stream"
    }

    private fun isDownloadableHit(type: Int): Boolean {
        return type == HitTestResult.IMAGE_TYPE ||
            type == HitTestResult.SRC_IMAGE_ANCHOR_TYPE ||
            type == HitTestResult.SRC_ANCHOR_TYPE
    }

    private data class PendingDownload(
        val url: String,
        val userAgent: String,
        val contentDisposition: String,
        val mimeType: String
    )

    companion object {
        private var dataDirectoryConfigured = false

        private const val WHATSAPP_WEB_URL = "https://web.whatsapp.com/"

        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"

        private const val OPEN_WHATSAPP_CHAT_MENU_SCRIPT = """
            (function() {
                function rectOf(element) {
                    return element ? element.getBoundingClientRect() : null;
                }

                function isVisible(element) {
                    if (!element) return false;
                    var rect = rectOf(element);
                    var style = window.getComputedStyle(element);
                    return rect &&
                        rect.width >= 20 &&
                        rect.height >= 20 &&
                        style.visibility !== "hidden" &&
                        style.display !== "none" &&
                        style.opacity !== "0";
                }

                function normalizedText(element) {
                    return [
                        element.getAttribute("aria-label") || "",
                        element.getAttribute("title") || "",
                        element.getAttribute("data-testid") || "",
                        element.getAttribute("data-icon") || "",
                        element.textContent || ""
                    ].join(" ").toLowerCase();
                }

                function hasMenuIcon(element) {
                    return !!element.querySelector("[data-icon='menu'],[data-icon='down-context'],[data-icon='more'],svg[aria-label*='menu' i]");
                }

                function dispatchRealClick(element) {
                    var rect = rectOf(element);
                    var x = rect.left + rect.width / 2;
                    var y = rect.top + rect.height / 2;
                    element.focus && element.focus();
                    ["pointerdown", "mousedown", "pointerup", "mouseup", "click"].forEach(function(type) {
                        element.dispatchEvent(new MouseEvent(type, {
                            bubbles: true,
                            cancelable: true,
                            view: window,
                            clientX: x,
                            clientY: y
                        }));
                    });
                }

                var main = document.querySelector("#main") ||
                    document.querySelector("[data-testid='conversation-panel-wrapper']") ||
                    Array.from(document.querySelectorAll("[role='main']")).sort(function(a, b) {
                        return rectOf(b).right - rectOf(a).right;
                    })[0];
                if (!main || !isVisible(main)) return false;

                var mainRect = rectOf(main);
                var headers = Array.from(main.querySelectorAll("header")).filter(isVisible);
                headers.sort(function(a, b) {
                    var ar = rectOf(a);
                    var br = rectOf(b);
                    return Math.abs(ar.top - mainRect.top) - Math.abs(br.top - mainRect.top);
                });
                var header = headers[0] || main;
                var headerRect = rectOf(header);

                var actions = Array.from(header.querySelectorAll("button,[role='button'],[tabindex='0']")).filter(function(action) {
                    if (!isVisible(action)) return false;
                    var rect = rectOf(action);
                    return rect.top >= headerRect.top - 4 &&
                        rect.bottom <= headerRect.bottom + 8 &&
                        rect.left >= mainRect.left + (mainRect.width * 0.50) &&
                        rect.right <= mainRect.right + 8;
                });

                if (actions.length === 0) {
                    actions = Array.from(main.querySelectorAll("button,[role='button'],[tabindex='0']")).filter(function(action) {
                        if (!isVisible(action)) return false;
                        var rect = rectOf(action);
                        return rect.top >= mainRect.top &&
                            rect.top <= mainRect.top + 96 &&
                            rect.left >= mainRect.left + (mainRect.width * 0.50) &&
                            rect.right <= mainRect.right + 8;
                    });
                }

                var menuActions = actions.filter(function(action) {
                    var text = normalizedText(action);
                    return text.indexOf("menu") >= 0 ||
                        text.indexOf("more") >= 0 ||
                        text.indexOf("options") >= 0 ||
                        hasMenuIcon(action);
                });

                var pool = menuActions.length > 0 ? menuActions : actions;
                pool.sort(function(a, b) {
                    var ar = rectOf(a);
                    var br = rectOf(b);
                    return br.right - ar.right || ar.top - br.top;
                });

                var target = pool[0];
                if (!target) return false;
                dispatchRealClick(target);
                return true;
            })();
        """

        private const val NOTIFICATION_BRIDGE_SCRIPT = """
            (function() {
                if (window.__androidWebBridgeInstalled) return;
                window.__androidWebBridgeInstalled = true;

                function send(title, options) {
                    try {
                        var body = options && options.body ? String(options.body) : "";
                        var tag = options && options.tag ? String(options.tag) : String(Date.now());
                        window.AndroidWhatsAppBridge.showNotification(String(title || "ChatDock Web"), body, tag);
                    } catch (e) {}
                }

                var NativeNotification = window.Notification;
                if (typeof NativeNotification === "function") {
                    function AndroidNotification(title, options) {
                        send(title, options || {});
                        try {
                            return new NativeNotification(title, options || {});
                        } catch (e) {
                            return {};
                        }
                    }

                    AndroidNotification.permission = "granted";
                    AndroidNotification.requestPermission = function(callback) {
                        try {
                            window.AndroidWhatsAppBridge.requestNotificationPermission();
                        } catch (e) {}
                        if (typeof callback === "function") callback("granted");
                        return Promise.resolve("granted");
                    };
                    AndroidNotification.maxActions = NativeNotification.maxActions || 0;
                    AndroidNotification.prototype = NativeNotification.prototype;
                    window.Notification = AndroidNotification;
                }
            })();
        """
    }
}
