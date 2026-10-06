package com.guruvasishta.teacherassistant;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final String APP_URL = "file:///android_asset/index.html";
    private static final String NOTICE_AI_URL = "https://school-teacher-gfsg.hatchable.site/api/notice-ai";

    public class AndroidShare {
        @JavascriptInterface
        public void shareImage(String dataUrl, String fileName) {
            runOnUiThread(() -> {
                try {
                    int comma = dataUrl.indexOf(",");
                    if (comma < 0) throw new Exception("Invalid image data");
                    String base64 = dataUrl.substring(comma + 1);
                    byte[] bytes = Base64.decode(base64, Base64.DEFAULT);

                    // Always use the app's internal cache. This is reliably covered by
                    // FileProvider and avoids OEM/Android-version differences with
                    // external-cache URI grants.
                    File dir = new File(getCacheDir(), "shared");
                    if (!dir.exists() && !dir.mkdirs()) throw new Exception("Cannot create share folder");

                    String safeName = (fileName == null || fileName.trim().isEmpty())
                            ? "school-notice.png" : fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
                    if (!safeName.toLowerCase().endsWith(".png")) safeName += ".png";

                    File file = new File(dir, safeName);
                    try (FileOutputStream out = new FileOutputStream(file)) {
                        out.write(bytes);
                        out.flush();
                    }

                    Uri uri = androidx.core.content.FileProvider.getUriForFile(
                            MainActivity.this,
                            getPackageName() + ".fileprovider",
                            file
                    );

                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("image/png");
                    share.putExtra(Intent.EXTRA_STREAM, uri);
                    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    share.setClipData(ClipData.newUri(getContentResolver(), "School Notice", uri));

                    Intent chooser = Intent.createChooser(share, "Share Notice Image");
                    chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(chooser);
                } catch (Exception e) {
                    e.printStackTrace();
                    android.widget.Toast.makeText(
                            MainActivity.this,
                            "Could not share notice image",
                            android.widget.Toast.LENGTH_LONG
                    ).show();
                }
            });
        }
    }

    public class AndroidSaveImage {
        @JavascriptInterface
        public void saveImage(String dataUrl, String fileName) {
            runOnUiThread(() -> {
                try {
                    int comma = dataUrl.indexOf(",");
                    if (comma < 0) throw new Exception("Invalid image data");
                    byte[] bytes = Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT);
                    String safeName = (fileName == null || fileName.trim().isEmpty())
                            ? "school-notice.png" : fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
                    if (!safeName.toLowerCase().endsWith(".png")) safeName += ".png";

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        android.content.ContentValues values = new android.content.ContentValues();
                        values.put(MediaStore.Images.Media.DISPLAY_NAME, safeName);
                        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                        values.put(MediaStore.Images.Media.RELATIVE_PATH,
                                android.os.Environment.DIRECTORY_PICTURES + "/Teacher Assistant");
                        values.put(MediaStore.Images.Media.IS_PENDING, 1);
                        Uri uri = getContentResolver().insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                        if (uri == null) throw new Exception("Could not create Gallery image");
                        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                            if (out == null) throw new Exception("Could not open image");
                            out.write(bytes);
                        }
                        values.clear();
                        values.put(MediaStore.Images.Media.IS_PENDING, 0);
                        getContentResolver().update(uri, values, null, null);
                        android.widget.Toast.makeText(MainActivity.this,
                                "Notice PNG saved in Gallery → Teacher Assistant",
                                android.widget.Toast.LENGTH_LONG).show();
                    } else {
                        pendingImageBytes = bytes;
                        pendingImageName = safeName;
                        Intent create = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        create.setType("image/png");
                        create.putExtra(Intent.EXTRA_TITLE, safeName);
                        startActivityForResult(create, CREATE_IMAGE_REQUEST);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    android.widget.Toast.makeText(MainActivity.this,
                            "Could not save notice image",
                            android.widget.Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public class AndroidWhatsApp {
        @JavascriptInterface
        public void shareText(String text, String title) {
            final String message = text == null ? "" : text;
            final String shareTitle = title;
            runOnUiThread(() -> {
                try {
                    Intent send = new Intent(Intent.ACTION_SEND);
                    send.setType("text/plain");
                    send.putExtra(Intent.EXTRA_TEXT, message);
                    if (shareTitle != null && !shareTitle.trim().isEmpty()) {
                        send.putExtra(Intent.EXTRA_TITLE, shareTitle);
                    }

                    // Prefer WhatsApp Messenger, then WhatsApp Business.
                    if (isPackageInstalled("com.whatsapp")) {
                        send.setPackage("com.whatsapp");
                    } else if (isPackageInstalled("com.whatsapp.w4b")) {
                        send.setPackage("com.whatsapp.w4b");
                    } else {
                        android.widget.Toast.makeText(
                                MainActivity.this,
                                "WhatsApp is not installed",
                                android.widget.Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    startActivity(send);
                } catch (Exception e) {
                    e.printStackTrace();
                    android.widget.Toast.makeText(
                            MainActivity.this,
                            "Could not open WhatsApp",
                            android.widget.Toast.LENGTH_LONG
                    ).show();
                }
            });
        }

        private boolean isPackageInstalled(String packageName) {
            try {
                getPackageManager().getPackageInfo(packageName, 0);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    public class AndroidAI {
        @JavascriptInterface
        public void rewriteNotice(String source, String type, String school) {
            new Thread(() -> {
                HttpURLConnection conn = null;
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("text", safe(source, 2500));
                    payload.put("type", safe(type, 80));
                    payload.put("school", safe(school, 120));

                    URL url = new URL(NOTICE_AI_URL);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(20000);
                    conn.setReadTimeout(60000);
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("x-teacher-assistant-app", "teacher-assistant-v1");
                    byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
                    try (OutputStream out = conn.getOutputStream()) { out.write(body); }

                    int code = conn.getResponseCode();
                    InputStream stream = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
                    String responseBody = readAll(stream);
                    if (code < 200 || code >= 300) {
                        String msg = "AI notice service unavailable";
                        try {
                            JSONObject err = new JSONObject(responseBody);
                            msg = err.optString("error", msg);
                        } catch (Exception ignored) {}
                        postAiError(msg);
                        return;
                    }

                    JSONObject response = new JSONObject(responseBody);
                    String english = response.optString("english", "");
                    String hindi = response.optString("hindi", "");
                    if (english.isEmpty() || hindi.isEmpty()) {
                        postAiError("AI returned an incomplete notice");
                        return;
                    }
                    postAiResult("ENGLISH:\n" + english + "\nHINDI:\n" + hindi);
                } catch (Exception e) {
                    postAiError(e.getMessage() == null ? "AI request failed" : e.getMessage());
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }).start();
        }

        private String safe(String value, int max) {
            if (value == null) return "";
            String s = value.trim();
            return s.length() > max ? s.substring(0, max) : s;
        }

        private String readAll(InputStream stream) throws Exception {
            if (stream == null) return "";
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
            return sb.toString();
        }

        private String extractOutputText(JSONObject response) {
            StringBuilder sb = new StringBuilder();
            JSONArray output = response.optJSONArray("output");
            if (output == null) return "";
            for (int i = 0; i < output.length(); i++) {
                JSONObject item = output.optJSONObject(i);
                if (item == null) continue;
                JSONArray content = item.optJSONArray("content");
                if (content == null) continue;
                for (int j = 0; j < content.length(); j++) {
                    JSONObject part = content.optJSONObject(j);
                    if (part != null && "output_text".equals(part.optString("type"))) {
                        String t = part.optString("text", "");
                        if (!t.isEmpty()) {
                            if (sb.length() > 0) sb.append("\n");
                            sb.append(t);
                        }
                    }
                }
            }
            return sb.toString().trim();
        }

        private void postAiResult(String text) {
            runOnUiThread(() -> {
                try {
                    String quoted = JSONObject.quote(text);
                    webView.evaluateJavascript("window.onNativeAiNotice('OK'," + quoted + ")", null);
                } catch (Exception e) {
                    webView.evaluateJavascript("window.onNativeAiNotice('ERROR','Could not return AI result')", null);
                }
            });
        }

        private void postAiError(String message) {
            runOnUiThread(() -> {
                String safe = message == null ? "AI request failed" : message.replace("\\", "\\\\").replace("'", "\\'");
                webView.evaluateJavascript("window.onNativeAiNotice('ERROR','" + safe + "')", null);
            });
        }
    }

    private static final int FILE_CHOOSER_REQUEST = 1001;
    private static final int CREATE_IMAGE_REQUEST = 1002;
    private byte[] pendingImageBytes;
    private String pendingImageName;
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;

    @SuppressLint("SetJavaScriptEnabled")
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webview);

        android.webkit.WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadsImagesAutomatically(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setSupportZoom(false);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        // Keep the local APK page isolated from arbitrary file/HTTP content.
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            s.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(false);
        }
        s.setUserAgentString(s.getUserAgentString() + " TeacherAssistantAndroid/3.0");

        CookieManager.getInstance().setAcceptCookie(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("http".equals(scheme) || "https".equals(scheme) || "file".equals(scheme)) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception ignored) {}
                return true;
            }
        });

        webView.addJavascriptInterface(new AndroidShare(), "AndroidShare");
        webView.addJavascriptInterface(new AndroidWhatsApp(), "AndroidWhatsApp");
        webView.addJavascriptInterface(new AndroidSaveImage(), "AndroidSaveImage");
        webView.addJavascriptInterface(new AndroidAI(), "AndroidAI");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onConsoleMessage(android.webkit.ConsoleMessage message) {
                // Keep JavaScript console warnings/errors out of the user-facing app.
                return true;
            }

            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    Intent intent = params.createIntent();
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                    return true;
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
            }
        });

        webView.loadUrl(APP_URL);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack();
                else finish();
            }
        });
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREATE_IMAGE_REQUEST) {
            if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null && pendingImageBytes != null) {
                try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                    if (out == null) throw new Exception("Could not open destination");
                    out.write(pendingImageBytes);
                    android.widget.Toast.makeText(this, "Notice PNG saved", android.widget.Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    android.widget.Toast.makeText(this, "Could not save notice image", android.widget.Toast.LENGTH_LONG).show();
                }
            }
            pendingImageBytes = null;
            pendingImageName = null;
            return;
        }
        if (requestCode == FILE_CHOOSER_REQUEST && fileCallback != null) {
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK && data != null) {
                Uri uri = data.getData();
                if (uri != null) results = new Uri[]{uri};
            }
            fileCallback.onReceiveValue(results);
            fileCallback = null;
        }
    }

    @Override protected void onDestroy() {
        if (fileCallback != null) fileCallback.onReceiveValue(null);
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}