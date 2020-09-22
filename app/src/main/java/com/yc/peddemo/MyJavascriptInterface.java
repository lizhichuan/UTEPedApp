package com.yc.peddemo;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;

public class MyJavascriptInterface {
    private Context context;

    public MyJavascriptInterface(Context context) {
        this.context = context;
    }

    /**
     * 前端代码嵌入js：
     * imageClick 名应和js函数方法名一致
     *
     * @param src 图片的链接
     */
    @JavascriptInterface
    public void imageClick(String src) {
        Log.e("imageClick", "----点击了图片");
        Log.e("src", src);
    }

}
