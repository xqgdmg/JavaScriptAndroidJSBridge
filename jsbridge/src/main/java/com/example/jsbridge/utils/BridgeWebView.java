package com.example.jsbridge.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.WebView;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * WebView 的桥梁
 */
@SuppressLint("SetJavaScriptEnabled")
public class BridgeWebView extends WebView implements WebViewJavascriptBridge {
    // 注入的 js 文件
    public static final String toLoadJs = "WebViewJavascriptBridge.js";

    // 存 js 给安卓的 response 回调类，是CallBack回调没有关系
    Map<String, CallBackFunction> responseCallbacks = new HashMap<String, CallBackFunction>();

    // 保存 handler
    Map<String, BridgeHandler> messageHandlers = new HashMap<String, BridgeHandler>();

    // 默认的handler，如果不传递handlerName默认使用这个处理
    BridgeHandler defaultHandler = new DefaultHandler();

    // js未注入的时候，安卓调用js，存Message 信息，防止数据丢失
    private List<Message> startupMessageList = new ArrayList<Message>();

    private long uniqueId = 0;

    public BridgeWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BridgeWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public BridgeWebView(Context context) {
        super(context);
        init();
    }

    /**
     * 只处理未定义handlerName 的
     *
     * @param handler default handler,handle messages send by js without assigned handler name,
     *                if js message has handler name, it will be handled by named handlers registered by native
     */
    public void setDefaultHandler(BridgeHandler handler) {
        this.defaultHandler = handler;
    }

    private void init() {
        this.setVerticalScrollBarEnabled(false);
        this.setHorizontalScrollBarEnabled(false);
        this.getSettings().setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        // 重写 WebViewClient
        this.setWebViewClient(generateBridgeWebViewClient());
    }

    /*
     * 获取 BridgeWebViewClient，用于拦截 url
     */
    protected BridgeWebViewClient generateBridgeWebViewClient() {
        return new BridgeWebViewClient(this);
    }

    /**
     * webView 截取 url 后，处理返回数据
     */
    void handlerReturnData(String url) {
        String functionName = BridgeUtil.getFunctionFromReturnUrl(url);
        CallBackFunction callBackFunction = responseCallbacks.get(functionName);
        String data = BridgeUtil.getDataFromReturnUrl(url);
        if (callBackFunction != null) {
            callBackFunction.onCallBack(data);
            responseCallbacks.remove(functionName); // 移除已经处理过的回调
            return;
        }
    }

    /*
     * webView 给 js 发送消息
     */
    @Override
    public void send(String data) {
        send(data, null);
    }

    /*
     * webView 给 js 发送消息，带callBack
     */
    @Override
    public void send(String data, CallBackFunction responseCallback) {
        doSend(null, data, responseCallback);
    }

    /**
     * 保存message到消息队列
     *
     * @param handlerName      handlerName
     * @param data             data
     * @param responseCallback CallBackFunction
     */
    private void doSend(String handlerName, String data, CallBackFunction responseCallback) {
        Message m = new Message();
        // Message存data
        if (!TextUtils.isEmpty(data)) {
            m.setData(data);
        }
        // Message存callback
        if (responseCallback != null) {
            String callbackStr = String.format(BridgeUtil.CALLBACK_ID_FORMAT, ++uniqueId + (BridgeUtil.UNDERLINE_STR + SystemClock.currentThreadTimeMillis()));
            // 将callBack保存到本地变量 responseCallbacks 中
            responseCallbacks.put(callbackStr, responseCallback);
            m.setCallbackId(callbackStr);
        }
        // Message存handlerName
        if (!TextUtils.isEmpty(handlerName)) {
            m.setHandlerName(handlerName);
        }
        // 查询消息
        queueMessage(m);
    }

    /**
     * list<message> != null 添加到消息集合
     * 否则分发消息
     *
     * @param m Message
     */
    private void queueMessage(Message m) {
        // 一般为空，网页加载完成后会手动清空
        // 主要是用来在JsBridge的js库注入之前，保存Java调用JS的消息，避免消息的丢失或失效。
        // 待页面加载完成后，后续CallHandler的调用，可直接使用loadUrl方法而不需入队。
        // 究其根本，是因为Js代码库必须在onPageFinished（页面加载完成）中才能注入导致的。
        if (startupMessageList != null) {
            startupMessageList.add(m);
        } else { // 网页加载完成后都是走这直接分发消息
            dispatchMessage(m);
        }
    }

    /**
     * 分发message 必须在主线程才分发成功
     *
     * @param m Message eg: "callbackId":"JAVA_CB_2_368","data":"data from android webView.callHandler()","handlerName":"functionInJs"}
     */
    void dispatchMessage(Message m) {
        String messageJson = m.toJson();
        Log.e("chris", "messageJson==" + messageJson);
        //为json字符串转义特殊字符，格式处理
        messageJson = messageJson.replaceAll("(\\\\)([^utrn])", "\\\\\\\\$1$2");
        messageJson = messageJson.replaceAll("(?<=[^\\\\])(\")", "\\\\\"");
        messageJson = messageJson.replaceAll("(?<=[^\\\\])(\')", "\\\\\'");
        messageJson = messageJson.replaceAll("%7B", URLEncoder.encode("%7B"));
        messageJson = messageJson.replaceAll("%7D", URLEncoder.encode("%7D"));
        messageJson = messageJson.replaceAll("%22", URLEncoder.encode("%22"));
        // javascriptCommand==javascript:WebViewJavascriptBridge._handleMessageFromNative('{\"callbackId\":\"JAVA_CB_2_368\",\"data\":\"data from android webView.callHandler()\",\"handlerName\":\"functionInJs\"}');
        String javascriptCommand = String.format(BridgeUtil.JS_HANDLE_MESSAGE_FROM_JAVA, messageJson);
        // 必须要找主线程才会将数据传递出去
        // webView在主线程中加载该 url schema
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            // 到这里安卓部分已经完成，拦截url 等待js回调，剩下的交给JsBridge处理
            this.loadUrl(javascriptCommand);
        }
    }

    /**
     * 刷新消息队列；与 js 通信；在 shouldOverrideUrlLoading 截取到网址之后调用。
     * 通过loadUrl调用到WebViewJavascriptBridge.js中的_fetchQueue()方法
     */
    void flushMessageQueue() {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            // JS_FETCH_QUEUE_FROM_JAVA = "javascript:WebViewJavascriptBridge._fetchQueue();";
            loadUrl(BridgeUtil.JS_FETCH_QUEUE_FROM_JAVA, new CallBackFunction() {// jsBridge抓取消息_fetchQueue

                @Override
                public void onCallBack(String data) {
                    // deserializeMessage 反序列化消息
                    List<Message> list = null;
                    try {
                        // json格式的data转为 list
                        list = Message.toArrayList(data);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                    if (list == null || list.size() == 0) {
                        return;
                    }
                    // 处理消息
                    for (int i = 0; i < list.size(); i++) {
                        Message message = list.get(i);
                        String responseId = message.getResponseId();

                        if (!TextUtils.isEmpty(responseId)) {// 是response
                            CallBackFunction function = responseCallbacks.get(responseId);
                            String responseData = message.getResponseData();
                            function.onCallBack(responseData);
                            responseCallbacks.remove(responseId);
                        } else {// 是CallBack
                            CallBackFunction responseFunction = null;

                            final String callbackId = message.getCallbackId();
                            if (!TextUtils.isEmpty(callbackId)) {// 如果有回调Id
                                responseFunction = new CallBackFunction() {
                                    @Override
                                    public void onCallBack(String data) {// 组装回调数据
                                        Message responseMsg = new Message();
                                        responseMsg.setResponseId(callbackId);
                                        responseMsg.setResponseData(data);
                                        queueMessage(responseMsg);// 再dispatchMessage(message);再 _handleMessageFromNative
                                    }
                                };
                            } else {
                                responseFunction = new CallBackFunction() {
                                    @Override
                                    public void onCallBack(String data) {
                                        // 没有回调Id，do nothing
                                    }
                                };
                            }
                            // Handler 操作
                            BridgeHandler handler;
                            if (!TextUtils.isEmpty(message.getHandlerName())) {
                                handler = messageHandlers.get(message.getHandlerName());
                            } else {
                                handler = defaultHandler;
                            }
                            if (handler != null) {
                                // callBackFunction.onCallBack()
                                handler.handler(message.getData(), responseFunction);
                            }
                        }
                    }
                }
            });
        }
    }


    public void loadUrl(String jsUrl, CallBackFunction returnCallback) {
        this.loadUrl(jsUrl);
        // 添加至 Map<String, CallBackFunction>
        responseCallbacks.put(BridgeUtil.parseFunctionName(jsUrl), returnCallback);
    }

    /**
     * register handler,so that javascript can call it
     * 注册处理程序,以便javascript调用它
     *
     * @param handlerName handlerName
     * @param handler     BridgeHandler
     */
    public void registerHandler(String handlerName, BridgeHandler handler) {
        if (handler != null) {
            // 添加至 Map<String, BridgeHandler>
            messageHandlers.put(handlerName, handler);
        }
    }

    /**
     * call javascript registered handler
     * 调用javascript处理程序注册
     *
     * @param handlerName handlerName
     * @param data        data
     * @param callBack    CallBackFunction
     */
    public void callHandler(String handlerName, String data, CallBackFunction callBack) {
        doSend(handlerName, data, callBack);
    }

    /**
     * unregister handler
     *
     * @param handlerName
     */
    public void unregisterHandler(String handlerName) {
        if (handlerName != null) {
            messageHandlers.remove(handlerName);
        }
    }

    public List<Message> getStartupMessageList() {
        return startupMessageList;
    }

    public void setStartupMessageList(List<Message> startupMessageList) {
        this.startupMessageList = startupMessageList;
    }

}
