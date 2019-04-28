//notation: js file can only use this kind of comments
//since comments will cause error when use in webview.loadurl,
//comments will be remove by java use regexp
(function() {
    if (window.WebViewJavascriptBridge) {
        return;
    }

    var messagingIframe;
    var bizMessagingIframe;

    var sendMessageQueue = [];/*发送消息队列*/
    var receiveMessageQueue = [];/*接受消息队列*/

    var messageHandlers = {};/*{}会比new Object()快，是一个数组*/

    var CUSTOM_PROTOCOL_SCHEME = 'yy';
    var QUEUE_HAS_MESSAGE = '__QUEUE_MESSAGE__/';/*消息队列有消息*/

    var responseCallbacks = {};/*定义了一个 Object，是一个数组*/

    var uniqueId = 1;

    // 创建消息index队列iframe，创建好的一个隐藏iframe来触发scheme
    function _createQueueReadyIframe(doc) {
        messagingIframe = doc.createElement('iframe');
        messagingIframe.style.display = 'none';
        doc.documentElement.appendChild(messagingIframe);
    }

    //创建消息体队列iframe，创建好的一个隐藏iframe来触发scheme
    function _createQueueReadyIframe4biz(doc) {
        bizMessagingIframe = doc.createElement('iframe');
        bizMessagingIframe.style.display = 'none';
        doc.documentElement.appendChild(bizMessagingIframe);
    }

    //set default messageHandler  初始化默认的消息线程
    function init(messageHandler) {
        if (WebViewJavascriptBridge._messageHandler) {
            throw new Error('WebViewJavascriptBridge.init called twice');
        }
        /*初始化 _messageHandler*/
        WebViewJavascriptBridge._messageHandler = messageHandler;
        var receivedMessages = receiveMessageQueue;
        receiveMessageQueue = null;
        for (var i = 0; i < receivedMessages.length; i++) {
            _dispatchMessageFromNative(receivedMessages[i]);
        }
    }

    // 发送
    function send(data, responseCallback) {
        _doSend({
            data: data
        }, responseCallback);
    }

    // 注册线程 往数组里面添加值
    function registerHandler(handlerName, handler) {
        messageHandlers[handlerName] = handler;
    }

    // 调用线程
    function callHandler(handlerName, data, responseCallback) {
        _doSend({
            handlerName: handlerName,
            data: data
        }, responseCallback);
    }

    //sendMessage add message, 触发native处理 sendMessage
    function _doSend(message, responseCallback) {
        if (responseCallback) {
            var callbackId = 'cb_' + (uniqueId++) + '_' + new Date().getTime();
            responseCallbacks[callbackId] = responseCallback;
            message.callbackId = callbackId;
        }

        // push() 方法可向数组的末尾添加一个或多个元素，并返回新的长度。
        sendMessageQueue.push(message);
        messagingIframe.src = CUSTOM_PROTOCOL_SCHEME + '://' + QUEUE_HAS_MESSAGE;
    }

    // 提供给native调用,该函数作用:获取sendMessageQueue返回给native,由于android不能直接获取返回的内容,所以使用url shouldOverrideUrlLoading 的方式返回内容
    function _fetchQueue() {/*抓取messageQueueString*/
        var messageQueueString = JSON.stringify(sendMessageQueue);
        sendMessageQueue = [];/*只是返回这个数组返回给安卓，安卓填充数据，安卓再一次 loadUrl()，调用_handleMessageFromNative(),X */
        //android can't read directly the return data, so we can reload iframe src to communicate with java
        if (messageQueueString !== '[]') {
            /*触发安卓的 shouldOverrideUrlLoading*/
            bizMessagingIframe.src = CUSTOM_PROTOCOL_SCHEME + '://return/_fetchQueue/' + encodeURIComponent(messageQueueString);
        }
    }

    //提供给native使用  //将回调信息传给H5
    function _dispatchMessageFromNative(messageJSON) {
        setTimeout(function() {
            var message = JSON.parse(messageJSON);
            var responseCallback;
            //java call finished, now need to call js callback function
            if (message.responseId) {/*根据CallHandler调用过程中Message的创建代码，其responseId为null，也就是android调用js的时候是没有responseId的*/
                responseCallback = responseCallbacks[message.responseId];
                if (!responseCallback) {
                    return;
                }
                responseCallback(message.responseData);
                delete responseCallbacks[message.responseId];/*删除已处理*/
            } else {
                //直接发送
                if (message.callbackId) {/*处理callback*/
                    var callbackResponseId = message.callbackId;
                    responseCallback = function(responseData) {
                        _doSend({
                            responseId: callbackResponseId,
                            responseData: responseData
                        });
                    };
                }

                /* 获取默认handler。若message设置了handlerName，则在messageHandlers中依据名字获取 */
                var handler = WebViewJavascriptBridge._messageHandler;
                /*查找指定handler*/
                if (message.handlerName) {
                    handler = messageHandlers[message.handlerName];
                }

                try {
                    /*handler处理消息*/
                    handler(message.data, responseCallback);
                } catch (exception) {
                    if (typeof console != 'undefined') {
                        console.log("WebViewJavascriptBridge: WARNING: javascript handler threw.", message, exception);
                    }
                }
            }
        });
    }

    //提供给native调用,receiveMessageQueue 在会在页面加载完后赋值为null,所以这个json消息会添加到 receiveMessageQueue
    function _handleMessageFromNative(messageJSON) {
        console.log(messageJSON);
        if (receiveMessageQueue) {
            receiveMessageQueue.push(messageJSON);
        }
        _dispatchMessageFromNative(messageJSON);
    }

    /*定义WebViewJavascriptBridge*/
    var WebViewJavascriptBridge = window.WebViewJavascriptBridge = {
        init: init,
        send: send,
        registerHandler: registerHandler,
        callHandler: callHandler,
        _fetchQueue: _fetchQueue,
        _handleMessageFromNative: _handleMessageFromNative
    };

    var doc = document;
    _createQueueReadyIframe(doc);
    _createQueueReadyIframe4biz(doc);
    var readyEvent = doc.createEvent('Events');
    /*注册html事件*/
    readyEvent.initEvent('WebViewJavascriptBridgeReady');
    readyEvent.bridge = WebViewJavascriptBridge;
    doc.dispatchEvent(readyEvent);
})();
