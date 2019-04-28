package com.example.jsbridge;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

import com.example.jsbridge.utils.BridgeHandler;
import com.example.jsbridge.utils.BridgeWebView;
import com.example.jsbridge.utils.CallBackFunction;
import com.example.jsbridge.utils.DefaultHandler;
import com.google.gson.Gson;


public class MainActivity extends Activity implements OnClickListener {

    private final String TAG = "MainActivity";

    BridgeWebView webView;

    Button button;

    int RESULT_CODE = 0;

    ValueCallback<Uri> mUploadMessage;

    ValueCallback<Uri[]> mUploadMessageArray;

    static class Location {
        String address;
    }

    static class User {
        String name;
        Location location;
        String testStr;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = (BridgeWebView) findViewById(R.id.webView);

        button = (Button) findViewById(R.id.button);

        button.setOnClickListener(this);

        webView.setDefaultHandler(new DefaultHandler());

        webView.setWebChromeClient(new WebChromeClient() {

            @SuppressWarnings("unused")
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String AcceptType, String capture) {
                this.openFileChooser(uploadMsg);
            }

            @SuppressWarnings("unused")
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String AcceptType) {
                this.openFileChooser(uploadMsg);
            }

            // 用于点击选择文件
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                mUploadMessage = uploadMsg;
                pickFile();
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                mUploadMessageArray = filePathCallback;
                pickFile();
                return true;
            }
        });

        webView.loadUrl("file:///android_asset/demo.html");

        // android 注冊给 js 使用的方法
        webView.registerHandler("submitFromWeb", new BridgeHandler() {

            @Override
            public void handler(String data, CallBackFunction function) {
                Log.e("chris", "handler = submitFromWeb, data from web = " + data);
                function.onCallBack("android 注冊给 js 使用的方法，js传递的参数==" + data);
            }

        });

        User user = new User();
        Location location = new Location();
        location.address = "广东深圳";
        user.location = location;
        user.name = "大头鬼";

        webView.callHandler("functionInJs", new Gson().toJson(user), new CallBackFunction() {
            @Override
            public void onCallBack(String data) {

            }
        });

        webView.send("hello");

    }

    public void pickFile() {
        Intent chooserIntent = new Intent(Intent.ACTION_GET_CONTENT);
        chooserIntent.setType("image/*");
        startActivityForResult(chooserIntent, RESULT_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == RESULT_CODE) {
            if (null == mUploadMessage && null == mUploadMessageArray) {
                return;
            }
            if (null != mUploadMessage && null == mUploadMessageArray) {
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;
            }

            if (null == mUploadMessage && null != mUploadMessageArray) {
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                mUploadMessageArray.onReceiveValue(new Uri[]{result});
                mUploadMessageArray = null;
            }

        }
    }

    @Override
    public void onClick(View v) {
        if (button.equals(v)) {
            // 这个方法本质是通过 webView.loadUrl(javascriptCommand);
            // 回调是在 shouldOverrideUrlLoading 处理，处理之后会把data 传递到 CallBackFunction 的 onCallBack()
            webView.callHandler("functionInJs", "data from android webView.callHandler()", new CallBackFunction() {

                @Override
                public void onCallBack(String data) {
                    Log.e("chris", "reponse data from js " + data);
                    Toast.makeText(MainActivity.this,"安卓调用了Js后，Js回调的数据== " + data,Toast.LENGTH_LONG).show();
                }

            });
        }

    }

}
