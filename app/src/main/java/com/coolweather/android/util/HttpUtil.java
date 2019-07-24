package com.coolweather.android.util;

import android.util.Log;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class HttpUtil {
    public static final String TAG="util";
    public static void sendOkHttpRequest(String address ,okhttp3.Callback callback){
        Log.d(TAG, "sendOkHttpRequest: ");
        OkHttpClient okHttpClient=new OkHttpClient();
        Request request =new Request.Builder().url(address).build();
        okHttpClient.newCall(request).enqueue(callback);
    }
}
