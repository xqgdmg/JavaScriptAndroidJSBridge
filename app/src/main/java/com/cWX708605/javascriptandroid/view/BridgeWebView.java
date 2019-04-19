package com.cWX708605.javascriptandroid.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import com.cWX708605.javascriptandroid.web.JavaScriptMethods;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * TODO
 *
 * @author cWX708605
 * @version [V6.0.0.1, 2019/4/19]
 * @since V6.0.0.1
 */
public class BridgeWebView extends WebView {

    /***
     * js调用android方法的映射字符串
     **/
    private static final String JSINTERFACE = "jsInterface";

    public BridgeWebView(Context context) {
        super(context);
    }

    public BridgeWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BridgeWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 注册js和android通信桥梁对象
     *
     * @param obj 桥梁类对象,该对象提供方法让js调用,默认开启JavaScriptEnabled=true
     */
    public void addBridgeInterface(Object obj) {
        this.getSettings().setJavaScriptEnabled(true);
        addJavascriptInterface(new JavaScriptMethods(getContext(),BridgeWebView.this), JSINTERFACE);
//        addJavascriptInterface(new MyJavaScriptMethod(obj), JSINTERFACE);
    }

    /**
     * 注册js和android通信桥梁对象
     *
     * @param obj 桥梁类对象,该对象提供方法让js调用
     * @param url 默认开启JavaScriptEnabled=true
     */
    public void addBridgeInterface(Object obj, String url) {
        this.getSettings().setJavaScriptEnabled(true);
        addJavascriptInterface(new JavaScriptMethods(getContext(),BridgeWebView.this), JSINTERFACE);
//        addJavascriptInterface(new MyJavaScriptMethod(obj), JSINTERFACE);
        loadUrl(url);
    }

    /**
     * 回调js方法
     *
     * @param json 参数，json格式字符串
     */
    public void callbackJavaScript(String json) {

    }

    private void invokeJavaScript(String callback, String params) {

    }

    /**
     * 内置js桥梁类
     */
    public class MyJavaScriptMethod {

        private Object mTarget;
        private Method targetMethod;


        public MyJavaScriptMethod(Object targer) {
            this.mTarget = targer;
        }

        /**
         * 内置桥梁方法
         *
         * @param method 方法名
         * @param json   js传递参数，json格式
         */
        @JavascriptInterface
        public void invokeMethod(String method, String[] json) {
            Log.e("chris","method:" + method + "-----MyJavaScriptMethod:" + json[0].toString());
            Class<?>[] params = new Class[]{String[].class};
            try {
                Method targetMethod = this.mTarget.getClass().getDeclaredMethod(method, params);
                targetMethod.invoke(mTarget, new Object[]{json});//反射调用js传递过来的方法，传参

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(),"异常==" + e.getCause(),Toast.LENGTH_LONG).show();
            }

        }
    }
}
