package com.example.jsbridge;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.example.jsbridge.utils.BridgeHandler;
import com.example.jsbridge.utils.BridgeWebView;
import com.example.jsbridge.utils.CallBackFunction;
import com.example.jsbridge.utils.DefaultHandler;
import com.google.gson.Gson;


public class MainActivity extends Activity {

    BridgeWebView webView;

    Button buttonCallHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        initWebView();

    }

    private void initView() {
        buttonCallHandler = (Button) findViewById(R.id.buttonCallHandler);
        webView = (BridgeWebView) findViewById(R.id.webView);

        buttonCallHandler.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                callHandler();
            }
        });


    }

    private void initWebView() {
        webView.setDefaultHandler(new DefaultHandler());

        webView.loadUrl("file:///android_asset/demo.html");

        // android 注冊给 js 使用的方法
        webView.registerHandler("submitFromWeb", new BridgeHandler() {

            @Override
            public void handler(String data, CallBackFunction function) {
                function.onCallBack("android 注冊给 js 使用的方法，js传递的参数==" + data);
            }

        });
    }

    /*
     * 这个方法本质是通过 webView.loadUrl(javascriptCommand);
     * 回调是在 shouldOverrideUrlLoading 处理；
     * 处理之后会把data 传递到 CallBackFunction 的 onCallBack()
     */
    private void callHandler() {

        webView.callHandler("functionInJs", "data from android webView.callHandler()", new CallBackFunction() {

            @Override
            public void onCallBack(String data) {
                Toast.makeText(MainActivity.this, "安卓调用了Js后，Js回调的数据== " + data, Toast.LENGTH_LONG).show();
            }
        });
    }











    static class Location {
        String address;
    }

    static class User {
        String name;
        Location location;
    }


}
