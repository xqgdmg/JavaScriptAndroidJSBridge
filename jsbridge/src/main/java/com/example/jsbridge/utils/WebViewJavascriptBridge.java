package com.example.jsbridge.utils;

/*
 * webView 给js 调用的桥梁
 */
public interface WebViewJavascriptBridge {

    public void send(String data);

    public void send(String data, CallBackFunction responseCallback);

}
