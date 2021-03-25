package com.dyna.nukima;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.franmontiel.persistentcookiejar.ClearableCookieJar;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.CookieCache;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import okhttp3.Cookie;
import okhttp3.FormBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

class HttpService {
	private final static String userAgent = "Mozilla/5.0 (X11; Linux x86_64; rv:85.0) Gecko/20100101 Firefox/85.0";
	private static final OkHttpClient client = new OkHttpClient();

	static Response HttpGet(String url) throws IOException {
		Request.Builder request = new Request.Builder() .url(url) .addHeader("user-agent", userAgent);
		return client.newCall(request.build()).execute();
	}

	static Response HttpGet(String url, HashMap<String, String> headers) throws IOException {
		Request.Builder request = new Request.Builder() .url(url) .addHeader("user-agent", userAgent);
		for (String key : headers.keySet()) {
			request.addHeader(key, headers.get(key));
		}
		return client.newCall(request.build()).execute();
	}

	static Response HttpPost(String url, HashMap<String, String> data, HashMap<String, String> headers) throws IOException {
		Request.Builder request = new Request.Builder() .url(url) .addHeader("user-agent", userAgent);
		for (String key : headers.keySet()) {
			request.addHeader(key, headers.get(key));
		}
		FormBody.Builder formBody = new FormBody.Builder();
		for (String key : data.keySet()) {
			formBody.add(key, data.get(key));
		}
		return client.newCall(request.post(formBody.build()).build()).execute();
	}

	static Response HttpPost(String url, HashMap<String, String> data) throws IOException {
		FormBody.Builder formBody = new FormBody.Builder();
		for (String key : data.keySet()) {
			formBody.add(key, data.get(key));
		}
		Request request = new Request.Builder() .url(url) .post(formBody.build()) .addHeader("user-agent", userAgent) .build();
		return client.newCall(request).execute();
	}
}
