package com.example.jsbridge.utils;

/*
 * 默认的 Handler ，如果不传 handlerName，默认使用这个处理
 */
public class DefaultHandler implements BridgeHandler {

    String TAG = "DefaultHandler";

    @Override
    public void handler(String data, CallBackFunction callBackFunction) {
        if (callBackFunction != null) {
            callBackFunction.onCallBack("DefaultHandler response data");
        }
    }

}
