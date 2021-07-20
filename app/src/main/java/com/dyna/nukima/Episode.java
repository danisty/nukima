package com.dyna.nukima;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Episode {
	private String name, url, animeName;
	private Activity context;
	private View layout = null;
	
	public Episode(String animeName, String name, String url, Activity context, View layout) {
		this.animeName = animeName;
		this.name = name;
		this.url = url;
		this.context = context;
		this.layout = layout;
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
		final AlertDialog.Builder mBuilder = new AlertDialog.Builder(context);

		context.runOnUiThread(() -> {
			mBuilder.setTitle("Choose a server");
			mBuilder.setSingleChoiceItems(availableServers, -1, (dialogInterface, i) -> {
				ServersManager serverManager = new ServersManager(availableServers[i], servers.get(ogAvailableServers[i]), self.url, this.context);
				updateSeenStatus(this.layout != null ? this.layout.findViewById(R.id.name) : null, true, true);
				dialogInterface.dismiss();

				new Thread(() -> {
					String streamingUrl = serverManager.getStreamingLink();
					if (streamingUrl != null) {
						Intent intent = new Intent(Intent.ACTION_VIEW);
						if (serverManager.server.equals("Mega"))
							intent.setData(Uri.parse(streamingUrl));
						else
							intent.setDataAndType(Uri.parse(streamingUrl), "video/mp4");
						intent.putExtra("title", this.animeName);
						context.startActivity(intent);
					} else {
						context.runOnUiThread(()->Toast.makeText(context, "Error on server", Toast.LENGTH_SHORT).show());
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

			String animeName = this.animeName;
			Integer episodeNum = Integer.parseInt(matcher.group(1));

			MainActivity.checkUserData(this.context);
			ArrayList<Integer> episodesList =  MainActivity.getIntegerArrayList(MainActivity.userData.getJSONObject("animes"), animeName);
			int colorAccent = this.context.getResources().getColor(R.color.colorAccent, null);
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
			MainActivity.updateUserData(this.context);

			if (watch != null) {
				new Thread(()-> {
					try {
						String animeNameEp = this.animeName + " " + episodeNum.toString();
						JSONObject history = MainActivity.userData.getJSONObject("history");
						boolean animeExistsOnHistory = history.has(animeNameEp);
						if (animeExistsOnHistory) { history.remove(animeNameEp); }

						String[] animeInfo = this.getData();
						JSONObject animeData = new JSONObject()
							.put("animeName", this.animeName)
							.put("animeImg", animeInfo[0])
							.put("episodeNum", episodeNum.toString())
							.put("animeUrl", this.url.substring(0, this.url.lastIndexOf("-")).replace("ver/", ""))
							.put("airing", animeInfo[1]);
						history.put(animeNameEp, animeData);
						MainActivity.updateUserData(this.context);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}).start();
			}
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this.context, "Something went wrong: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	private String[] getData() throws IOException {
		Document doc = Jsoup.connect(this.url.substring(0, this.url.lastIndexOf("-")).replace("ver/", "")).get();
		return new String[]{
			doc.getElementsByClass("image").get(0).child(0).attr("src"),
			doc.getElementsByClass("is-success").size() > 0 ? "Airing" : "Finished"
		};
	}
	
	public View configure(final EpisodeAdapter.ViewHolder episode) {
		try {
			final Episode self = this;
			this.context.runOnUiThread(() -> {
				TextView ep = episode.name;
				ep.setText(self.name);
				ep.setActivated(false);

				episode.play.setOnClickListener(v -> new Thread(self::watch).start());
				episode.name.setOnClickListener(v -> updateSeenStatus(ep, true, null));
				episode.comments.setOnClickListener(v -> {
					WebView commentsWebView = this.layout.getRootView().getRootView().findViewById(R.id.commentsWebView);
					commentsWebView.setVisibility(View.VISIBLE);

					String disqusPost = this.getDisqusPost();
					commentsWebView.getSettings().setJavaScriptEnabled(true);

					commentsWebView.setWebViewClient(new WebViewClient());
					commentsWebView.setWebChromeClient(new WebChromeClient());
					commentsWebView.loadData(disqusPost, "text/html", null);
				});
				updateSeenStatus(ep, false, null);
			});
		} catch (Exception e) { e.printStackTrace(); }
		return null;
	}

	private String getDisqusPost() {
		return String.format("<div id='disqus_thread'></div><script>var disqus_config=function (){this.page.url='%s';this.page.identifier='%s';};(function(){var d=document, s=d.createElement('script');s.src='https://animefenix.disqus.com/embed.js';s.setAttribute('data-timestamp', + new Date());(d.head || d.body).appendChild(s);})();</script>", this.url, this.url.substring(23));
	}
}
