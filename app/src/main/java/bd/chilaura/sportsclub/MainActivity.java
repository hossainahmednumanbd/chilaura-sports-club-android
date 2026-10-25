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
            private boolean openExternal(Uri uri) {
                String scheme = uri.getScheme();
                if (scheme == null) return false;
                if (!scheme.equalsIgnoreCase("tel") && !scheme.equalsIgnoreCase("mailto")
                        && !scheme.equalsIgnoreCase("sms") && !scheme.equalsIgnoreCase("smsto")) return false;
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    if (scheme.equalsIgnoreCase("tel")) intent.setAction(Intent.ACTION_DIAL);
                    startActivity(intent);
                } catch (ActivityNotFoundException ex) {
                    android.widget.Toast.makeText(MainActivity.this, "এই কাজের জন্য উপযুক্ত অ্যাপ পাওয়া যায়নি", android.widget.Toast.LENGTH_LONG).show();
                }
                return true;
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
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
