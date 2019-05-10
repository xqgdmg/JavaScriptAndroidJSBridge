package com.cWX708605.javascriptandroid.bridge;

import android.content.Context;
import android.os.Handler;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

/*
 * 4.2 之后必须使用@JavascriptInterface注释方法，不然js找不到方法
 */
public class JavaScriptMethods {
    private WebView mWebView;
    private Context context;
    private Handler mHandler = new Handler() {};

    public JavaScriptMethods(Context context, WebView webView) {
        this.context = context;
        this.mWebView = webView;
    }


    @JavascriptInterface
    public void showToast(String json) {
        Toast.makeText(context, json, Toast.LENGTH_SHORT).show();
    }

    /**
     * 获取酒店详情数据
     * <p>
     * 这是安卓的方法
     *
     * @param json
     */
    @JavascriptInterface //android4.2之后，必须加上，不然js不能调用安卓方法
    public void getHotelData(final String json) {
        try {
            Toast.makeText(context, "安卓先收到js消息:" + json, Toast.LENGTH_SHORT).show();

            JSONObject jsJsonObject = new JSONObject(json);
            final String callback = jsJsonObject.optString("callback");//js回调方法（callback）

            // 这里假装已经访问了网络获取数据
            final JSONObject backJson = new JSONObject();
            backJson.put("name", "安卓getHotelData()被js调用了");
            backJson.put("detail", "这是安卓返回的酒店详情数据");

            // android调用js，必须在主线程
            invokeJavaSctiptMethod(callback, backJson.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 安卓调用js后，js再调用安卓使用（android调js后，js回调android）
     */
    @JavascriptInterface
    public void callbackToJs(String jsJson) {
        try {
            // 获取js callback方法名
            JSONObject jsonObject = new JSONObject(jsJson);

            Toast.makeText(context, "js返回的callback" + jsonObject.toString(), Toast.LENGTH_SHORT).show();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 调用js方法，统一管理
     *
     * @param callback
     * @param backJson
     */
    private void invokeJavaSctiptMethod(final String callback, final String backJson) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mWebView.loadUrl("javascript:" + callback + "(" + backJson + ")");
            }
        });
    }

}
