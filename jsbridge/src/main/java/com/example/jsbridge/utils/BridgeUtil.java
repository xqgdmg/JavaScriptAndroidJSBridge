package com.example.jsbridge.utils;

import android.content.Context;
import android.util.Log;
import android.webkit.WebView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/*
 * 定义 schema
 *
 */
public class BridgeUtil {
    final static String YY_OVERRIDE_SCHEMA = "yy://";
    final static String YY_RETURN_DATA = YY_OVERRIDE_SCHEMA + "return/";//格式为   yy://return/{function}/returncontent
    final static String YY_FETCH_QUEUE = YY_RETURN_DATA + "_fetchQueue/";
    final static String EMPTY_STR = "";
    final static String UNDERLINE_STR = "_";
    final static String SPLIT_MARK = "/";

    final static String CALLBACK_ID_FORMAT = "JAVA_CB_%s";
    // 这是分发消息的时候 webView.load(url);使用的url
    final static String JS_HANDLE_MESSAGE_FROM_JAVA = "javascript:WebViewJavascriptBridge._handleMessageFromNative('%s');";
    final static String JS_FETCH_QUEUE_FROM_JAVA = "javascript:WebViewJavascriptBridge._fetchQueue();";

    // 例子 javascript:WebViewJavascriptBridge._fetchQueue(); --> _fetchQueue
    // 解析 schema 对应 url 的方法
    public static String parseFunctionName(String jsUrl) {
        String result = jsUrl.replace("javascript:WebViewJavascriptBridge.", "").replaceAll("\\(.*\\);", "");
        Log.e("chris", "parseFunctionName==" + result);
        return result;
    }

    // 获取到传递信息的body值
    // url = yy://return/_fetchQueue/[{"responseId":"JAVA_CB_2_3957","responseData":"Javascript Says Right back aka!"}]
    public static String getDataFromReturnUrl(String url) {
        if (url.startsWith(YY_FETCH_QUEUE)) {
            // return = [{"responseId":"JAVA_CB_2_3957","responseData":"Javascript Says Right back aka!"}]
            String result1 = url.replace(YY_FETCH_QUEUE, EMPTY_STR);
            Log.e("chris", "getDataFromReturnUrl的result1==" + result1);
            return result1;
        }

        // temp = _fetchQueue/[{"responseId":"JAVA_CB_2_3957","responseData":"Javascript Says Right back aka!"}]
        String temp = url.replace(YY_RETURN_DATA, EMPTY_STR);
        String[] functionAndData = temp.split(SPLIT_MARK);

        if (functionAndData.length >= 2) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < functionAndData.length; i++) {
                sb.append(functionAndData[i]);
            }
            // return = [{"responseId":"JAVA_CB_2_3957","responseData":"Javascript Says Right back aka!"}]
            Log.e("chris", "getDataFromReturnUrl的result2==" + sb.toString());
            return sb.toString();
        }
        return null;
    }

    // 获取到传递信息的方法
    // url = yy://return/_fetchQueue/[{"responseId":"JAVA_CB_1_360","responseData":"Javascript Says Right back aka!"}]
    public static String getFunctionFromReturnUrl(String url) {
        // temp = _fetchQueue/[{"responseId":"JAVA_CB_1_360","responseData":"Javascript Says Right back aka!"}]
        String temp = url.replace(YY_RETURN_DATA, EMPTY_STR);
        String[] functionAndData = temp.split(SPLIT_MARK);
        if (functionAndData.length >= 1) {
            // functionAndData[0] = _fetchQueue
            Log.e("chris", "getFunctionFromReturnUrl==" + functionAndData[0]);
            return functionAndData[0];
        }
        return null;
    }


    /**
     * 这里只是加载assets中的 WebViewJavascriptBridge.js
     *
     * @param view webview
     * @param path 路径
     */
    public static void webViewLoadLocalJs(WebView view, String path) {
        String jsContent = assetFile2Str(view.getContext(), path);
        view.loadUrl("javascript:" + jsContent);
        Log.e("chris", "webViewLoadLocalJs==" + "javascript:" + jsContent);
    }

    /**
     * 解析assets文件夹里面的代码,去除注释,取可执行的代码
     *
     * @param c      context
     * @param urlStr 路径
     * @return 可执行代码
     */
    public static String assetFile2Str(Context c, String urlStr) {
        InputStream in = null;
        try {
            in = c.getAssets().open(urlStr);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
            String line = null;
            StringBuilder sb = new StringBuilder();
            do {
                line = bufferedReader.readLine();
                if (line != null && !line.matches("^\\s*\\/\\/.*")) { // 去除注释
                    sb.append(line);
                }
            } while (line != null);

            bufferedReader.close();
            in.close();

            Log.e("chris", "assetFile2Str==" + sb.toString());
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }
}
