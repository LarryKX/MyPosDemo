package com.example.larry.myposdemo.utils;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class POSHttpConnector {

    protected static POSHttpConnector instance = null;

    private String token;

    public void setRefresh_token(String refresh_token) {
        this.refresh_token = refresh_token;
    }

    private String refresh_token;

    public static final MediaType JSON = MediaType.get("application/json");

    public void setToken(String token) {
        this.token = token;
    }

    private POSHttpConnector(){

    }

    public static synchronized POSHttpConnector getInstance() {
        if(instance == null)
            instance = new POSHttpConnector();
        return instance;
    }

    private Request.Builder addToken(String url) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(url).header("content-type", "application/json");
        if(token != null && !token.isEmpty())
            requestBuilder.addHeader("Authorization", "Bearer "+token);
        return requestBuilder;
    }

    public void get(String url, Callback callback) {
        final Request request = addToken(url).build();

        OkHttpClient client = new OkHttpClient();

        client.newCall(request).enqueue(callback);
    }

    public void post(String url, JsonObject json, Callback callback) {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = addToken(url).post(body).build();

        OkHttpClient client = new OkHttpClient();

        client.newCall(request).enqueue(callback);
    }

    public void put(String url, JsonObject json, Callback callback) {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = addToken(url).put(body).build();
        client.newCall(request).enqueue(callback);
    }

    private String mapToString(Map<String, String> map) {
        String begin = "{";
        StringBuilder str = new StringBuilder(begin);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if(!begin.equals(str.toString())) str.append(",");
            str.append("\"").append(entry.getKey()).append("\":");
            if(entry.getValue().equalsIgnoreCase("true")){
                str.append("true");
            }
            else str.append("\"").append(entry.getValue()).append("\"");
        }
        str.append("}");
        return str.toString();
    }

}
