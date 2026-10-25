package bd.chilaura.sportsclub;

import android.app.Activity;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.os.Build;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.net.Uri;
import android.content.ActivityNotFoundException;

public class MainActivity extends Activity {
    private static final int FILE_PICKER_REQUEST = 1107;
    private ValueCallback<android.net.Uri[]> uploadCallback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView web = new WebView(this);
        web.setWebViewClient(new WebViewClient() {
            private void launch(Uri uri, String action) {
                try {
                    Intent intent = new Intent(action, uri);
                    intent.addCategory(Intent.CATEGORY_BROWSABLE);
                    startActivity(intent);
                } catch (Exception ex) {
                    android.widget.Toast.makeText(MainActivity.this,
                        "লিংকটি খোলার উপযুক্ত অ্যাপ পাওয়া যায়নি", android.widget.Toast.LENGTH_LONG).show();
                }
            }
            private boolean openExternal(Uri uri) {
                if (uri == null) return false;
                String scheme = uri.getScheme();
                if (scheme == null) return false;
                scheme = scheme.toLowerCase(java.util.Locale.ROOT);
                if (scheme.equals("file") || scheme.equals("about") || scheme.equals("data")
                    || scheme.equals("javascript") || scheme.equals("blob")) return false;
                if (scheme.equals("tel")) { launch(uri, Intent.ACTION_DIAL); return true; }
                if (scheme.equals("mailto") || scheme.equals("sms") || scheme.equals("smsto")) {
                    launch(uri, Intent.ACTION_VIEW); return true;
                }
                if (scheme.equals("http") || scheme.equals("https")) {
                    launch(uri, Intent.ACTION_VIEW); return true;
                }
                if (scheme.equals("whatsapp")) {
                    try {
                        Intent whatsapp = new Intent(Intent.ACTION_VIEW, uri);
                        whatsapp.setPackage("com.whatsapp");
                        startActivity(whatsapp);
                    } catch (Exception ex) {
                        String number = uri.getQueryParameter("phone");
                        if (number != null) {
                            number = number.replaceAll("[^0-9]", "");
                            if (number.startsWith("0") && number.length() == 11)
                                number = "88" + number;
                            if (number.length() >= 10 && number.length() <= 15)
                                launch(Uri.parse("https://wa.me/" + number), Intent.ACTION_VIEW);
                        }
                    }
                    return true;
                }
                if (scheme.equals("intent")) {
                    try {
                        Intent parsed = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                        parsed.addCategory(Intent.CATEGORY_BROWSABLE);
                        parsed.setComponent(null);
                        parsed.setSelector(null);
                        try { startActivity(parsed); }
                        catch (Exception missing) {
                            String fallback = parsed.getStringExtra("browser_fallback_url");
                            if (fallback != null && fallback.startsWith("https://"))
                                launch(Uri.parse(fallback), Intent.ACTION_VIEW);
                        }
                    } catch (Exception ignored) {}
                    return true;
                }
                if (scheme.equals("fb") || scheme.equals("fb-messenger") || scheme.equals("tg")
                    || scheme.equals("geo") || scheme.equals("market") || scheme.equals("viber")
                    || scheme.equals("youtube")) {
                    launch(uri, Intent.ACTION_VIEW); return true;
                }
                android.widget.Toast.makeText(MainActivity.this,
                    "এই লিংকটি খোলা যাচ্ছে না", android.widget.Toast.LENGTH_SHORT).show();
                return true;
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                return openExternal(request.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return openExternal(Uri.parse(url));
            }
        });
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(true);
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<android.net.Uri[]> callback, FileChooserParams params) {
                if (uploadCallback != null) uploadCallback.onReceiveValue(null);
                uploadCallback = callback;
                try {
                    Intent intent = params.createIntent();
                    startActivityForResult(intent, FILE_PICKER_REQUEST);
                    return true;
                } catch (Exception ex) {
                    uploadCallback = null;
                    return false;
                }
            }
        });
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_PICKER_REQUEST && uploadCallback != null) {
            uploadCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            uploadCallback = null;
        }
    }
}
