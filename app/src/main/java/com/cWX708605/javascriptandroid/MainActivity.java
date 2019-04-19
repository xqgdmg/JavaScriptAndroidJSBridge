package com.cWX708605.javascriptandroid;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

import com.cWX708605.javascriptandroid.view.BridgeWebView;
import com.cWX708605.javascriptandroid.web.JavaScriptMethods;

public class MainActivity extends Activity {

    private BridgeWebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
        initView();

        /*setWebView();
        mWebView.addJavascriptInterface(new JavaScriptMethods(this, mWebView), "jsInterface");*/
//        mWebView.loadUrl("http://188.188.4.100:8080/html5/index.html");//在线模板，测试阶段使用在线模板
//        mWebView.loadUrl("file:///android_asset/html5/index.html");//本地模板，正式发布，省流量，用户体验好
        mWebView.addBridgeInterface(new JavaScriptMethods(MainActivity.this), "file:///android_asset/html5/index.html");

    }

    /*
     * 原始的设置方法，现在不用了，抽取桥梁
     */
    /*private void setWebView() {
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);


        //设置浏览器
        mWebView.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
            }
        });

        mWebView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                try {
                    JSONObject json = new JSONObject();
                    json.put("onPageFinished", "执行了");
                    json.put("building", "富基大厦");
                    mWebView.loadUrl("javascript:showMessage("+json.toString()+")");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }
        });
    }*/

    private void initView() {
        mWebView = (BridgeWebView) findViewById(R.id.webView);
    }
}
