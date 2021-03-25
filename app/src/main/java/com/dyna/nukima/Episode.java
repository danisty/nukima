package com.dyna.nukima;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.okhttp.Response;

import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

class Episode {
	private String name, url;
	private Category category;
	private View layout = null;
	
	public Episode(String name, String url, Category category) {
		this.name = name;
		this.url = url;
		this.category = category;
	}

	public String[] filterServers(String[] availableServers, HashMap<String, String> servers, boolean og) {
		ArrayList<String> results = new ArrayList<>();
		for (String s : availableServers) {
			String[] n = s.split(":");
			if (servers.get(n[0]) != null) {
				results.add((n.length > 1 && !og) ? n[1] : n[0]);
			}
		}
		return results.toArray(new String[]{});
	}

	public void watch() {
		final Episode self = this;
		final HashMap<String, String> servers = ServersManager.getServers(self.url);
		final String[] availableServers = filterServers(ServersManager.availableServers, servers, false);
		final String[] ogAvailableServers = filterServers(ServersManager.availableServers, servers, true);
		final AlertDialog.Builder mBuilder = new AlertDialog.Builder(self.category.context);

		self.category.context.runOnUiThread(() -> {
			mBuilder.setTitle("Choose a server");
			mBuilder.setSingleChoiceItems(availableServers, -1, (dialogInterface, i) -> {
				ServersManager serverManager = new ServersManager(availableServers[i], servers.get(ogAvailableServers[i]), self.url, this.category.context);
				updateSeenStatus(this.layout != null ? this.layout.findViewById(R.id.name) : null, true, true);
				dialogInterface.dismiss();

				new Thread(() -> {
					String streamingUrl = serverManager.getStreamingLink();
					if (streamingUrl != null) {
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setDataAndType(Uri.parse(streamingUrl), "video/mp4");
						self.category.context.startActivity(intent);
					} else {
						self.category.context.runOnUiThread(()->Toast.makeText(self.category.context, "Error on server", Toast.LENGTH_SHORT).show());
					}
				}).start();
			}).create().show();
		});
	}

	private void updateSeenStatus(View ep, boolean reverse, Boolean watch) {
		try {
			TextView episode = ep != null ? (TextView) ep : null;
			Pattern pattern = Pattern.compile(".+ (\\d+)");
			Matcher matcher = pattern.matcher(this.name);
			matcher.find();

			String animeName = this.category.name.trim();
			Integer episodeNum = Integer.parseInt(matcher.group(1));

			MainActivity.checkUserData(this.category.context);
			ArrayList<Integer> episodesList =  MainActivity.getIntegerArrayList(MainActivity.userData.getJSONObject("animes"), animeName);
			int colorAccent = this.category.context.getResources().getColor(R.color.colorAccent, null);
			boolean episodeSeen = watch == null && episodesList.contains(episodeNum);
			if (episode != null) {
				episode.setTextColor(episodeSeen ? reverse ? Color.WHITE : colorAccent : reverse ? colorAccent : Color.WHITE);
				episode.setActivated(episodeSeen == reverse);
			}

			if (reverse) {
				if (episodeSeen) { episodesList.remove(episodeNum); }
				else if (!episodesList.contains(episodeNum)) { episodesList.add(episodeNum); }
			}
			if (episodesList.size() == 0) {
				MainActivity.userData.getJSONObject("animes").remove(animeName);
			}
			MainActivity.updateUserData(this.category.context);

			if (watch != null) {
				new Thread(()-> {
					try {
						String animeNameEp = this.category.name + " " + episodeNum.toString();
						JSONObject history = MainActivity.userData.getJSONObject("history");
						boolean animeExistsOnHistory = history.has(animeNameEp);
						if (animeExistsOnHistory) { history.remove(animeNameEp); }

						String[] animeInfo = this.getData();
						JSONObject animeData = new JSONObject()
							.put("animeName", this.category.name)
							.put("animeImg", animeInfo[0])
							.put("episodeNum", episodeNum.toString())
							.put("animeUrl", this.url.substring(0, this.url.lastIndexOf("-")).replace("ver/", ""))
							.put("airing", animeInfo[1]);
						history.put(animeNameEp, animeData);
						MainActivity.updateUserData(this.category.context);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}).start();
			}
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this.category.context, "Something went wrong: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	private String[] getData() throws IOException {
		Document doc = Jsoup.connect(this.url.substring(0, this.url.lastIndexOf("-")).replace("ver/", "")).get();
		return new String[]{
			doc.getElementsByClass("image").get(0).child(0).attr("src"),
			doc.getElementsByClass("is-success").size() > 0 ? "Airing" : "Finished"
		};
	}
	
	public View configure(final View episode) {
		try {
			final Episode self = this;
			this.category.context.runOnUiThread(() -> {
				TextView ep = episode.findViewById(R.id.name);
				ep.setText(self.name);
				ep.setActivated(false);

				episode.findViewById(R.id.play).setOnClickListener(v -> new Thread(self::watch).start());
				episode.findViewById(R.id.name).setOnClickListener(v -> updateSeenStatus(ep, true, null));
				episode.findViewById(R.id.comment).setOnClickListener(v -> {
					WebView commentsWebView = this.category.layout.getRootView().findViewById(R.id.commentsWebView);
					commentsWebView.setVisibility(View.VISIBLE);

					String disqusPost = this.getDisqusPost();
					commentsWebView.getSettings().setJavaScriptEnabled(true);

					commentsWebView.setWebViewClient(new WebViewClient());
					commentsWebView.setWebChromeClient(new WebChromeClient());
					commentsWebView.loadData(disqusPost, "text/html", null);
				});
				updateSeenStatus(ep, false, null);
			});
			return episode;
		} catch (Exception e) { e.printStackTrace(); }
		return null;
	}

	private String getDisqusPost() {
		return String.format("<div id='disqus_thread'></div><script>var disqus_config=function (){this.page.url='%s';this.page.identifier='%s';};(function(){var d=document, s=d.createElement('script');s.src='https://animefenix.disqus.com/embed.js';s.setAttribute('data-timestamp', + new Date());(d.head || d.body).appendChild(s);})();</script>", this.url, this.url.substring(23));
	}
	
	public void inflateView(Activity context) {
		final View layout = this.category.layout;
		final View episode = configure(context.getLayoutInflater().inflate(R.layout.episode, null));
		this.layout = episode;
		category.context.runOnUiThread(() -> {
			((ViewGroup) layout.findViewById(R.id.itemHolder)).addView(episode);
		});
	}
}
