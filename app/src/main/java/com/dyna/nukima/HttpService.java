package com.dyna.nukima;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.HashMap;

class HttpService {
	private static String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Safari/537.36 OPR/68.0.3618.197";

	static Response HttpGet(String url) throws IOException {
		Request request = new Request.Builder() .url(url) .header("user-agent", userAgent).build();
		return new OkHttpClient().newCall(request).execute();
	}

	static Response HttpGet(String url, HashMap<String, String> headers) throws IOException {
		Request.Builder request = new Request.Builder() .url(url) .header("user-agent", userAgent);
		for (String key : headers.keySet()) {
			request.header(key, headers.get(key));
		}
		return new OkHttpClient().newCall(request.build()).execute();
	}

	static Response HttpPost(String url, HashMap<String, String> data, HashMap<String, String> headers) throws IOException {
		Request.Builder request = new Request.Builder() .url(url) .header("user-agent", userAgent);
		for (String key : headers.keySet()) {
			request.header(key, headers.get(key));
		}
		FormEncodingBuilder formBody = new FormEncodingBuilder();
		for (String key : data.keySet()) {
			formBody.add(key, data.get(key));
		}
		return new OkHttpClient().newCall(request.post(formBody.build()).build()).execute();
	}

	static Response HttpPost(String url, HashMap<String, String> data) throws IOException {
		FormEncodingBuilder formBody = new FormEncodingBuilder();
		for (String key : data.keySet()) {
			formBody.add(key, data.get(key));
		}
		Request request = new Request.Builder() .url(url) .post(formBody.build()) .header("user-agent", userAgent) .build();
		return new OkHttpClient().newCall(request).execute();
	}
}
