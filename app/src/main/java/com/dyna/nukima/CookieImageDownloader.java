package com.dyna.nukima;

import android.content.Context;
import android.net.Uri;

import com.squareup.picasso.UrlConnectionDownloader;

import java.io.IOException;
import java.net.HttpURLConnection;

class CookieImageDownloader extends UrlConnectionDownloader {

	public CookieImageDownloader(Context context) {
		super(context);
	}

	@Override
	protected HttpURLConnection openConnection(Uri path) throws IOException {
		HttpURLConnection conn = super.openConnection(path);
		conn.setRequestProperty("Cookie", MainActivity.cookiesRaw);
		return conn;
	}
}