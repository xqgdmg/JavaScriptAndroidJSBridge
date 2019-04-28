package com.example.jsbridge.utils;

/*
 * android 注冊给 js 使用的方法
 */
public interface BridgeHandler {

    void handler(String data, CallBackFunction function);

}
