package com.elia.messifan;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

public class MainActivity extends Activity {
    private static final String APP_ORIGIN = "https://app.local";
    private static final String HOME = APP_ORIGIN + "/index.html";
    private WebView webView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean backPressedOnce = false;
    private final Runnable resetBackPress = () -> backPressedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(true);
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            settings.setSafeBrowsingEnabled(true);
        }

        webView.addJavascriptInterface(new AndroidBridge(this), "AndroidBridge");
        webView.setWebViewClient(new LocalWebViewClient());

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Uri data = intent == null ? null : intent.getData();
        if (data != null
                && "messifan".equalsIgnoreCase(data.getScheme())
                && "payment-return".equalsIgnoreCase(data.getHost())) {
            String checkout = safe(data.getQueryParameter("checkout"));
            String orderRef = safe(data.getQueryParameter("order_ref"));
            String url = HOME + "?checkout=" + Uri.encode(checkout)
                    + "&order_ref=" + Uri.encode(orderRef);
            webView.loadUrl(url);
            return;
        }
        webView.loadUrl(HOME);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    @Override
    public void onBackPressed() {
        if (webView == null) {
            super.onBackPressed();
            return;
        }
        webView.evaluateJavascript("window.messiHandleBack ? window.messiHandleBack() : false", result -> {
            if ("true".equals(result)) return;
            if (webView.canGoBack()) {
                backPressedOnce = false;
                webView.goBack();
                return;
            }
            if (!backPressedOnce) {
                backPressedOnce = true;
                Toast.makeText(this, "اضغط رجوع مرة أخرى للخروج", Toast.LENGTH_SHORT).show();
                handler.removeCallbacks(resetBackPress);
                handler.postDelayed(resetBackPress, 1800);
                return;
            }
            handler.removeCallbacks(resetBackPress);
            MainActivity.super.onBackPressed();
        });
    }

    private class LocalWebViewClient extends WebViewClient {
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            if ("https".equalsIgnoreCase(uri.getScheme()) && "app.local".equalsIgnoreCase(uri.getHost())) {
                String path = uri.getPath();
                if (path == null || path.equals("/")) path = "/index.html";
                if (path.contains("..")) return null;
                String assetPath = "www" + path;
                try {
                    InputStream input = getAssets().open(assetPath);
                    String mime = mimeType(path);
                    String encoding = mime.startsWith("text/") || mime.contains("javascript") || mime.contains("json")
                            ? "UTF-8" : null;
                    return new WebResourceResponse(mime, encoding, input);
                } catch (IOException ignored) {
                    return null;
                }
            }
            return null;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            if ("https".equalsIgnoreCase(uri.getScheme()) && "app.local".equalsIgnoreCase(uri.getHost())) {
                return false;
            }
            openExternalUri(uri);
            return true;
        }
    }

    private static String mimeType(String path) {
        String ext = MimeTypeMap.getFileExtensionFromUrl(path);
        String mime = ext == null ? null : MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());
        if (mime != null) return mime;
        String guessed = URLConnection.guessContentTypeFromName(path);
        return guessed != null ? guessed : "application/octet-stream";
    }

    private void openExternalUri(Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "لا يوجد تطبيق مناسب لفتح الرابط", Toast.LENGTH_SHORT).show();
        }
    }

    public class AndroidBridge {
        private final Context context;

        AndroidBridge(Context context) {
            this.context = context;
        }

        @JavascriptInterface
        public void openExternal(String url) {
            if (url == null) return;
            Uri uri;
            try {
                uri = Uri.parse(url);
            } catch (Exception e) {
                return;
            }
            String scheme = uri.getScheme();
            if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) return;
            runOnUiThread(() -> openExternalUri(uri));
        }

        @JavascriptInterface
        public String platform() {
            return "android";
        }
    }
}
