package com.cWX708605.javascriptandroid.web;

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
    private Handler mHandler = new Handler() {
    };

    public JavaScriptMethods(Context context, WebView webView) {
        this.context = context;
        this.mWebView = webView;
    }

    public JavaScriptMethods(Context context) {
        this.context = context;
    }

    @JavascriptInterface
    public void showToast(String json) {
        Toast.makeText(context, json, Toast.LENGTH_SHORT).show();
    }

    /**
     * 获取酒店详情数据
     *
     * 这是安卓的方法
     *
     * @param json
     */
    @JavascriptInterface //android4.2之后，必须加上，不然js不能调用安卓方法
    public void getHotelData(final String json) {
        try {
            Toast.makeText(context, "安卓收到js消息:" + json, Toast.LENGTH_SHORT).show();
            //访问网络获取数据
            JSONObject jsJsonObject = new JSONObject(json);
            final String callback = jsJsonObject.optString("callback");//js回调方法（callback）


            final JSONObject backJson = new JSONObject();
            backJson.put("name", "安卓getHotelData()");
            backJson.put("price", "888.88");
            backJson.put("address", "深圳市罗湖区富基大厦13楼");

            //android调用js，必须在主线程
            invokeJavaSctiptMethod(callback, backJson.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 调用js方法，统一管理
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

    /**
     * 获取酒店订单详情数据
     */
    @JavascriptInterface
    public void getHotelOrder(String jsJson) {

        try {
            //获取js callback方法名
            JSONObject jsonObject = new JSONObject(jsJson);
            String callback = jsonObject.optString("callback");
            //从服务器获取数据
            JSONObject json = new JSONObject();
            json.put("orderId", "20160517848284327");
            json.put("order_status", "等待确认");
            json.put("price", "108");
            json.put("back_price", "39");
            json.put("seller", "携程");
            json.put("phone", "18888888888");
            json.put("contact", "杨康");
            json.put("pay_tpye", "到店付款");
            json.put("expire_time", "20:30");
            json.put("room_size", "标准双人间  双床（120cm*200cm）");
            json.put("in_date", "2016年5月17日");
            json.put("out_date", "2016年5月18日");
            json.put("root_count", "2间");
            json.put("hotel_name", "宝立方国际大酒店");
            json.put("server_phone", "010-536724567");

            //返回给js
//            mWebView.loadUrl("javascript:"+callback+"("+json.toString()+")");
            invokeJavaSctiptMethod(callback, json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
