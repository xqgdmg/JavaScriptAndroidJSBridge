package com.cWX708605.javascriptandroid;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import com.cWX708605.javascriptandroid.web.JavaScriptMethods;

public class HotelDetailActivity extends Activity {

    private WebView mWebView;
    private TextView mTextView;
    private TextView tvCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
        initView();

        setWebView();
        mWebView.addJavascriptInterface(new JavaScriptMethods(this, mWebView), "jsInterface");
//        mWebView.loadUrl("http://188.188.4.100:8080/html5/orderComp.html");//在线模板，测试阶段使用在线模板
        mWebView.loadUrl("file:///android_asset/html5/index.html");//本地模板，正式发布，省流量，用户体验好
    }

    private void setWebView() {
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
                Toast.makeText(HotelDetailActivity.this,"onPageFinished",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }
        });
    }

    private void initView() {
        mWebView = (WebView) findViewById(R.id.webView);
        mTextView = (TextView) findViewById(R.id.tv);
        tvCallback = (TextView) findViewById(R.id.tvCallback);

        mTextView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    JSONObject json = new JSONObject();
                    json.put("call", "Android调用Js方法");
                    json.put("city", "深圳市1");
                    mWebView.loadUrl("javascript:showMessage("+json.toString()+")");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        tvCallback.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    JSONObject json = new JSONObject();
                    json.put("call", "Android调用Js方法callback");
                    json.put("city", "深圳市2");
                    json.put("callback", "callback");
                    mWebView.loadUrl("javascript:androidWithCallback("+json.toString()+")");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }
}
