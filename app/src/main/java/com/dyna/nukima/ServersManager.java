package com.dyna.nukima;

import android.content.Context;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Response;

class ServersManager {
	public String server;
	public String serverUrl;
	public String episode;
	public Context context;
	public static String[] availableServers = {
		"Amazon", "AmazonEs", "M:Mega", "Sendvid", "Mp4upload"
	};

	public ServersManager(String server, String serverUrl, String episode, Context context) {
		this.server = server;
		this.serverUrl = serverUrl;
		this.episode = episode;
		this.context = context;
	}

	public static HashMap<String, String> getServers(String episodeUrl) {
		HashMap<String, String> servers = new HashMap<>();
		try {
			Response page_data = HttpService.HttpGet(episodeUrl);
			String page_raw_html = page_data.body().string();

			Document doc = Jsoup.parse(page_raw_html);
			Pattern p = Pattern.compile(" src='(.+)' frame");
			Matcher m = p.matcher(page_raw_html.substring(page_raw_html.indexOf("tabsArray")));

			for (Element server : doc.getElementsByClass("episode-page__servers-list").get(0).children()) {
				m.find();
				servers.put(server.child(0).attr("title"), m.group(1));
			}
		} catch (Exception e) { e.printStackTrace(); }
		return servers;
	}

	private String getOriginalLink(String redirectUrl) throws IOException {
		Response page_data = HttpService.HttpGet(redirectUrl.replace("amp;", ""));
		Pattern p = Pattern.compile("playerContainer.innerHTML = '.+src=\"(.+)\" frame.+'");

		Matcher m = p.matcher(page_data.body().string());
		return m.find() ? m.group(1) : null;
	}

	public String Amazon(String serverUrl) throws IOException {
		Response page_data = HttpService.HttpGet("https://www.animefenix.com" + serverUrl.substring(2));
		Pattern p = Pattern.compile("file\":\"([^\"]+)?");

		Matcher m = p.matcher(page_data.body().string());
		return m.find() ? StringEscapeUtils.unescapeJava(m.group(1)) : null;
	}

	public String AmazonEs(String serverUrl) throws IOException {
		Response page_data = HttpService.HttpGet(("https://www.animefenix.com" + serverUrl).replace("amp;", ""));
		Pattern p = Pattern.compile("file\":\"([^\"]+)?");

		String content = page_data.body().string();
		Matcher m = p.matcher(content);

		return m.find() ? StringEscapeUtils.unescapeJava(m.group(1)) : null;
	}

	public String Mega(String serverUrl) {
		return serverUrl.replace("embed", "file");
	}

	public String Sendvid(String serverUrl) throws IOException {
		Response page_data = HttpService.HttpGet(serverUrl);
		Document doc = Jsoup.parse(page_data.body().string());

		Elements source = doc.getElementsByTag("source");
		return source.size() > 0 ? source.get(0).attr("src") : null;
	}

	public String Mp4upload(String serverUrl) throws IOException {
		Response page_data = HttpService.HttpGet(serverUrl);
		Pattern pattern = Pattern.compile("eval(\\([^*]+\\))");
		Matcher matcher = pattern.matcher(page_data.body().string());

		if (matcher.find()) {
			String script = matcher.group(1);
			Pattern argsPattern = Pattern.compile("'(.+)',(\\d+),(\\d+),'(.+)'\\.");
			Matcher args = argsPattern.matcher(script);
			args.find();

			Pattern urlPattern = Pattern.compile("player.src\\(\"([^\"]+)");
			Matcher urlMatch = urlPattern.matcher(Mp4UploadLink(
				args.group(1),
				Integer.parseInt(args.group(2)),
				Integer.parseInt(args.group(3)),
				args.group(4).split("\\|")
			));
			return urlMatch.find() ? urlMatch.group(1) : null;
		}
		return null;
	}

	private String get(String[] list, int index) {
		try {
			return list[index];
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private String Mp4UploadLink(String p, int a, int c, String[] k) {
		while (c-- > 0) {
			if (get(k, c) != null) {
				p = p.replaceAll("\\b" + Integer.toString(c, a) + "\\b", get(k, c));
			}
		}
		return p;
	}

	public String getStreamingLink() {
		try {
			String serverUrl = getOriginalLink(this.serverUrl);
			return serverUrl != null ? (String) ServersManager.class.getMethod(this.server, String.class).invoke(this, serverUrl) : null;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
