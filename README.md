# Android 调用 JS 的方法：<br/>


![](https://i.imgur.com/MfwfN4L.png)


## 实现原理：Android使用loadUrl方法，向js传递数据。js通过Iframe给Android回调。

* 1.html调用js的registerHandler方法  ：

		//html调用registerHandler()
		WebViewJavascriptBridge.registerHandler("functionInJs", function(data, responseCallback) {
		    document.getElementById("show").innerHTML = ("data from Java: = " + data);
		    var responseData = "Javascript Write back something!";
		    responseCallback(responseData);
		});

* 2.Android调用callHandler()

		//Java调用注册的方法functionInJs【Java代码】
		webView.callHandler("functionInJs", new Gson().toJson(user), new CallBackFunction() {
	        @Override
	        public void onCallBack(String data) {

	        }
		});

* 3.Android调用doSend()，将callBack 保存到BridgeWebView 本地变量 responseCallbacks 中：

		Map<String, CallBackFunction> responseCallbacks = new HashMap<String, CallBackFunction>();

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

* 4.Android调用queueMessage()

		private void queueMessage(Message m) {
		    //此时startupMessageList为空，网页加载完成后shouldOverrideUrlLoading会清空它，它是用来在JsBridge的js库注入之前，保存Java调用JS的消息，避免消息的丢失。
		    // 究其根本，是因为Js代码库必须在onPageFinished（页面加载完成）中才能注入导致的。
		    if (startupMessageList != null) {
		        startupMessageList.add(m);
		    } else {
				// 网页加载完成后都是走这直接分发消息
		        dispatchMessage(m);
		    }
		}

* 5.Android调用dispatchMessage()，最终调用webView.load(url)。
使用url："javascript:WebViewJavascriptBridge._handleMessageFromNative('%s');"，百分号里面是messageJson。
eg：javascript:WebViewJavascriptBridge._handleMessageFromNative('{\"callbackId\":\"JAVA_CB_2_368\",\"data\":\"data from android webView.callHandler()\",\"handlerName\":\"functionInJs\"}');

		void dispatchMessage(Message m) {
		    String messageJson = m.toJson();

		    //为json字符串转义特殊字符，格式处理
		    messageJson = messageJson.replaceAll("(\\\\)([^utrn])", "\\\\\\\\$1$2");
		    messageJson = messageJson.replaceAll("(?<=[^\\\\])(\")", "\\\\\"");
		    messageJson = messageJson.replaceAll("(?<=[^\\\\])(\')", "\\\\\'");
		    messageJson = messageJson.replaceAll("%7B", URLEncoder.encode("%7B"));
		    messageJson = messageJson.replaceAll("%7D", URLEncoder.encode("%7D"));
		    messageJson = messageJson.replaceAll("%22", URLEncoder.encode("%22"));
		    // javascriptCommand==javascript:WebViewJavascriptBridge._handleMessageFromNative('{\"callbackId\":\"JAVA_CB_2_368\",\"data\":\"data from android webView.callHandler()\",\"handlerName\":\"functionInJs\"}');
		    String javascriptCommand = String.format(BridgeUtil.JS_HANDLE_MESSAGE_FROM_JAVA, messageJson);

		    // webView在主线程中加载该 url schema
		    if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
		        // 到这里安卓部分已经完成，拦截url 等待js回调，剩下的交给JsBridge的_handleMessageFromNative处理
		        this.loadUrl(javascriptCommand);
		    }
		}

* 6.webView通过loadUrl()调用的是 JsBridge 的 _handleMessageFromNative()：

		function _handleMessageFromNative(messageJSON) {
		    console.log(messageJSON);
		    if (receiveMessageQueue) {
		        receiveMessageQueue.push(messageJSON);
		    }
		    _dispatchMessageFromNative(messageJSON);
		}

* 7 JS的 _dispatchMessageFromNative(messageJSON)会从 var messageHandlers = {} 取出handler，执行handler(message.data, responseCallback)，将回调信息传给html。

		//将回调信息传给html
	    function _dispatchMessageFromNative(messageJSON) {
	        setTimeout(function() {
	            var message = JSON.parse(messageJSON);
	            var responseCallback;

				//根据CallHandler调用过程中Message的创建代码，其responseId为null，也就是android调用js的时候是没有responseId的
	            if (message.responseId) {
	                responseCallback = responseCallbacks[message.responseId];
	                if (!responseCallback) {
	                    return;
	                }
	                responseCallback(message.responseData);
	                delete responseCallbacks[message.responseId];
	            } else {
	                if (message.callbackId) {//根据CallHandler调用过程中Message的创建代码，callbackId已经赋值
	                    var callbackResponseId = message.callbackId;

						//responseCallback是一个方法，初始化回调
	                    responseCallback = function(responseData) {
	                        _doSend({//_doSend方法参数为json，通过更改iFrame触发安卓接收数据
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
                    /*handler开始处理消息*/
                    handler(message.data, responseCallback);
                } catch (exception) {
                    if (typeof console != 'undefined') {
                        console.log("WebViewJavascriptBridge: WARNING: javascript handler threw.", message, exception);
                    }
                }
            }
        });
    	}


* 8.handle(message.data, responseCallback)，也就是html的registerHandler()的第二个参数，也就是 name为 functionInJs的这个handler：

		//注册方法供java调用
		bridge.registerHandler("functionInJs", function(data, responseCallback) {
			document.getElementById("show").innerHTML = ("data from Java: = " + data);
			var responseData = "Javascript Says Right back aka!";
			responseCallback(responseData);
		});


* 9 responseCallback调用js的_doSend()保存消息到sendMessageQueue

		function _doSend(message, responseCallback) {

		    // 保存responseCallback信息
		    if (responseCallback) {
		        var callbackId = 'cb_' + (uniqueId++) + '_' + new Date().getTime();
		        responseCallbacks[callbackId] = responseCallback;
		        message.callbackId = callbackId;
		    }

		    // 保存消息到sendMessageQueue
		    sendMessageQueue.push(message);
		    messagingIframe.src = CUSTOM_PROTOCOL_SCHEME + '://' + QUEUE_HAS_MESSAGE;
		}

* 10 shouldOverrideUrlLoading拦截后，调用webView.flushMessageQueue()

		/**
		 * 与 js 通信，获取js消息；
		 * 通过loadUrl调用到WebViewJavascriptBridge.js中的_fetchQueue()方法
		 */
		void flushMessageQueue() {
		    MyCallBackFunction myCallBackFunction = new MyCallBackFunction();
		    // jsBridge抓取消息 _fetchQueue 即返回messageQueueString的数据 ，抓取之后回调数据到 myCallBackFunction
		    if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
		        // JS_FETCH_QUEUE_FROM_JAVA = "javascript:WebViewJavascriptBridge._fetchQueue();";
		        loadUrl(BridgeUtil.JS_FETCH_QUEUE_FROM_JAVA, myCallBackFunction);
		    }
		}

* 11 _fetchQueue()获取sendMessageQueue返回给android

		// 提供给native调用,该函数作用:获取sendMessageQueue返回给native,由于android不能直接获取返回的内容,所以使用url shouldOverrideUrlLoading 的方式返回内容
		function _fetchQueue() {
		    var messageQueueString = JSON.stringify(sendMessageQueue);
		    sendMessageQueue = [];

		    if (messageQueueString !== '[]') {
		        //触发安卓的 shouldOverrideUrlLoading
		        bizMessagingIframe.src = CUSTOM_PROTOCOL_SCHEME + '://return/_fetchQueue/' + encodeURIComponent(messageQueueString);
		    }
		}

* 12 shouldOverrideUrlLoading拦截后，调用handlerReturnData()

		/**
		 * webView 截取 url 后，处理js返回数据
		 */
		void handlerReturnData(String url) {
		    String functionName = BridgeUtil.getFunctionFromReturnUrl(url);
		    Log.e("chris","handlerReturnData functionName==" + functionName);
		    CallBackFunction callBackFunction = responseCallbacks.get(functionName);
		    Log.e("chris","handlerReturnData callBackFunction==" + callBackFunction);
		    String data = BridgeUtil.getDataFromReturnUrl(url);
		    Log.e("chris","handlerReturnData data==" + data);
		    if (callBackFunction != null) {
		        callBackFunction.onCallBack(data);// 执行回调
		        responseCallbacks.remove(functionName); // 移除已经处理过的回调
		        return;
		    }
		}

* 13 callBackFunction.onCallBack(data)执行回调，Android接收js传来的data

		//Java调用注册的方法functionInJs【Java代码】
		webView.callHandler("functionInJs", new Gson().toJson(user), new CallBackFunction() {
		        @Override
		        public void onCallBack(String data) {
		            // 这里处理 js返回的 data
		        }
		});


<br/>


# JS 调用 Android 的方法：<br/>

![](https://i.imgur.com/oaD6VP1.png)

## 实现原理：利用js的iFrame（不显示）的src（url）动态变化，触发java层WebViewClient的shouldOverrideUrlLoading方法，然后让本地去调用js。js代码执行完成后，最终调用_doSend方法处理回调。<br/>

## a：没有传 handlerName 的方式：使用 defaultHandler 处理，不用注册 Handler。

* 1.demo.html页面中点击"发消息给Native"按钮，触发WebViewJavascriptBridge.js中send方法的调用:（第二个参数是responseCallback）

		function testClick() {
		    //send message to native，通过 send()，只是少了一个参数 handlerName
		    var data = {id: 1, content: "这是一个图片 <img src=\"a.png\"/> test\r\nhahaha"};
		    window.WebViewJavascriptBridge.send(
		        data
		        , function(responseData) {
		            document.getElementById("show").innerHTML = "repsonseData from android, data = " + responseData
		        }
		    );
		}

* 2.然后调用 _doSend()，更换iFrame的src，触发BridgeWebViewClient的shouldOverrideUrlLoading方法。

		//sendMessage add message, 触发native处理 sendMessage
		function _doSend(message, responseCallback) {
		    if (responseCallback) {
		        var callbackId = 'cb_' + (uniqueId++) + '_' + new Date().getTime();
		        responseCallbacks[callbackId] = responseCallback;
		        message.callbackId = callbackId;
		    }

		    // 添加消息到 sendMessageQueue
		    sendMessageQueue.push(message);
		    messagingIframe.src = CUSTOM_PROTOCOL_SCHEME + '://' + QUEUE_HAS_MESSAGE;
		}

* 3.shouldOverrideUrlLoading方法根据url的前缀，进入了BridgeWebView的flushMessageQueue方法。

		//BridgeWebViewClient.java
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
		    try {
		        url = URLDecoder.decode(url, "UTF-8");
		    } catch (UnsupportedEncodingException e) {
		        e.printStackTrace();
		    }

		    if (url.startsWith(BridgeUtil.YY_RETURN_DATA)) {
		         //url以yy://return/开头
		        webView.handlerReturnData(url);
		        return true;
		    } else if (url.startsWith(BridgeUtil.YY_OVERRIDE_SCHEMA)) { //
		         //url以yy://开头
		        webView.flushMessageQueue();
		        return true;
		    } else {
		        return super.shouldOverrideUrlLoading(view, url);
		    }
		}

* 4.Android的flushMessageQueue()方法，通过loadUrl调用到WebViewJavascriptBridge.js中的_fetchQueue()方法

		void flushMessageQueue() {
		    MyCallBackFunction myCallBackFunction = new MyCallBackFunction();
		    // jsBridge抓取消息 _fetchQueue 即返回messageQueueString的数据 ，抓取之后回调数据到 myCallBackFunction
		    if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
		        // JS_FETCH_QUEUE_FROM_JAVA = "javascript:WebViewJavascriptBridge._fetchQueue();";
		        loadUrl(BridgeUtil.JS_FETCH_QUEUE_FROM_JAVA, myCallBackFunction);
		    }
		}

* 5._fetchQueue方法将sendMessageQueue数组中的所有消息，序列化为json字符串，通过更改iFrame的src，触发shouldOverrideUrlLoading方法

		// 提供给native调用,该函数作用:获取sendMessageQueue返回给native,由于android不能直接获取返回的内容,所以使用url shouldOverrideUrlLoading 的方式返回内容
		function _fetchQueue() {
		    var messageQueueString = JSON.stringify(sendMessageQueue);// 读取 js发送的消息
		    sendMessageQueue = [];//置空
		    //android can't read directly the return data, so we can reload iframe src to communicate with java
		    if (messageQueueString !== '[]') {
		        /*触发安卓的 shouldOverrideUrlLoading，真正把消息传给 Android*/
		        bizMessagingIframe.src = CUSTOM_PROTOCOL_SCHEME + '://return/_fetchQueue/' + encodeURIComponent(messageQueueString);
		    }
		}

* 6.shouldOverrideUrlLoading方法根据url的前缀，进入了BridgeWebView的handlerReturnData方法。结果回调到 onCallBack()。

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

* 7.handlerReturnData，处理数据。调用callBackFunction.onCallBack()。

	    void handlerReturnData(String url) {
	        String functionName = BridgeUtil.getFunctionFromReturnUrl(url);
	        CallBackFunction callBackFunction = responseCallbacks.get(functionName);
	        String data = BridgeUtil.getDataFromReturnUrl(url);
	
	         // 这里主要处理android回调数据给js，如果js调android有传递 handlername 的话会执行
	        if (callBackFunction != null) {
	            callBackFunction.onCallBack(data);// 调用 MyCallBackFunction 的 onCallBack
	            responseCallbacks.remove(functionName); // 移除已经处理过的回调
	            return;
	        }
	    }

* 8.handler.handler()，registerHandler的 onCallback()，收到js传递的data。再通过new CallBackFunction() 组装回调数据。这是回调数据的开始，registerHandler的 onCallback()传递了android回到给js的data。

		class MyCallBackFunction implements CallBackFunction{
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

					if (!TextUtils.isEmpty(responseId)) {// 处理response
						CallBackFunction function = responseCallbacks.get(responseId);
						String responseData = message.getResponseData();
						function.onCallBack(responseData);
						responseCallbacks.remove(responseId);
					} else {// 处理CallBack
						// 创建回调对象
						CallBackFunction callBackFunction = null;

						final String callbackId = message.getCallbackId();
						if (!TextUtils.isEmpty(callbackId)) {
							// 如果有回调Id，组装回调数据
							callBackFunction = new CallBackFunction() {
								@Override
								public void onCallBack(String data) {
									Message responseMsg = new Message();
									responseMsg.setResponseId(callbackId);
									responseMsg.setResponseData(data);
									queueMessage(responseMsg);// 再dispatchMessage(message);再 _handleMessageFromNative
								}
							};
						} else {
							callBackFunction = new CallBackFunction() {
								@Override
								public void onCallBack(String data) {
									// 没有回调Id，不回调数据
								}
							};
						}
						// 处理Handler
						BridgeHandler handler;
						if (!TextUtils.isEmpty(message.getHandlerName())) {
							handler = messageHandlers.get(message.getHandlerName());
						} else {
							handler = defaultHandler;
						}
						if (handler != null) {
							// c执行完注册的handler之后，执行callBackFunction里面的onCallBack方法
							handler.handler(message.getData(), callBackFunction);
						}
					}
				}
			}
		}

* 9.Android通过queueMessage()，dispatchMessage()，loadUrl()写入回调数据。再经过js的_handleMessageFromNative()，_dispatchMessageFromNative()，Js收到android回调的数据。

	    void handlerReturnData(String url) {
	        String functionName = BridgeUtil.getFunctionFromReturnUrl(url);
	        CallBackFunction callBackFunction = responseCallbacks.get(functionName);
	        String data = BridgeUtil.getDataFromReturnUrl(url);
	
	         // 这里主要处理android回调数据给js，如果js调android有传递 handlername 的话会执行
	        if (callBackFunction != null) {
	            callBackFunction.onCallBack(data);// 调用 MyCallBackFunction 的 onCallBack
	            responseCallbacks.remove(functionName); // 移除已经处理过的回调
	            return;
	        }
	    }

--------------------------

## b：传 handlerName 的方式：需要注册 Handler。

* 1.传 handlerName 的方式：注册可供js调用的handler。最终handler在java端存放在webview的messageHandlers变量中

		//MainActivity.java
		webView.registerHandler("submitFromWeb", new BridgeHandler() {

		    @Override
		    public void handler(String data, CallBackFunction function) {
		        Log.i(TAG, "handler = submitFromWeb, data from web = " + data);
		                function.onCallBack("submitFromWeb exe, response data 中文 from Java");
		    }

		});

		//BridgeWebView.java
		public void registerHandler(String handlerName, BridgeHandler handler) {
		    if (handler != null) {
		        messageHandlers.put(handlerName, handler);
		    }
		}

* 2.html中调用Native端提供的方法

		//demo.html
		function testClick1() {
		    var str1 = document.getElementById("text1").value;
		    var str2 = document.getElementById("text2").value;

		    //call native method
		    window.WebViewJavascriptBridge.callHandler(
		        'submitFromWeb'
		         , {'param': '中文测试'}
		         , function(responseData) { // 这个参数是responseCallback
		                    document.getElementById("show").innerHTML = "js send get responseData from java, data = " + responseData
		        }
		    );
		}

* 3.callHandler最终调用_doSend方法

	//WebViewJavascriptBridge.js
	function callHandler(handlerName, data, responseCallback) {
	    _doSend({//上面的a2
	        handlerName: handlerName,
	        data: data
	    }, responseCallback);
	}

后面是和a一样的流程，回到a的第二个步骤。