package com.dyna.nukima;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import okhttp3.Response;

public class AnimeActivity extends AppCompatActivity {
	private String animeName;
	private String animeImg;
	private String animeUrl;
	private String airing;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_anime);

		Toolbar toolbar = findViewById(R.id.anime_toolbar);
		setSupportActionBar(toolbar);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);

		toolbar.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.ic_back_arrow));
		toolbar.setNavigationOnClickListener(v -> onBackPressed());

		Intent intent = getIntent();
		animeUrl = intent.getStringExtra("animeUrl");
		animeImg = intent.getStringExtra("animeImg");
		animeName = intent.getStringExtra("animeName");
		airing = intent.getStringExtra("airing");

		if (airing == null && !getIntent().getBooleanExtra("favorites", false)) {
			new Thread(() -> {
				try {
					MainActivity.checkUserData(this);
					Document soup = Jsoup.connect(animeUrl).get();
					airing = soup.getElementsByClass("is-success").size() > 0 ? "Airing" : "Finished";
					if (MainActivity.userData.getJSONObject("favorites").has(animeName)) {
						JSONObject animeDataFav = MainActivity.userData.getJSONObject("favorites").getJSONObject(animeName);
						MainActivity.userData.getJSONObject("favorites").put(animeName, animeDataFav);
						animeDataFav.put("airing", airing);
						MainActivity.updateUserData(this);
					}
					if (MainActivity.userData.getJSONObject("history").has(animeName)) {
						JSONObject animeDataHistory = MainActivity.userData.getJSONObject("history").getJSONObject(animeName);
						MainActivity.userData.getJSONObject("history").put(animeName, animeDataHistory);
						animeDataHistory.put("airing", airing);
						MainActivity.updateUserData(this);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}).start();
		}

		RecyclerView animeFavs = findViewById(R.id.animeRecycler);
		animeFavs.setHasFixedSize(true);
		animeFavs.setItemViewCacheSize(20);

		LinearLayoutManager layoutManager = new LinearLayoutManager(this);
		animeFavs.setLayoutManager(layoutManager);

		SearchAdapter mAdapter = new SearchAdapter(true);
		animeFavs.setAdapter(mAdapter);
		toolbar.setTitle(animeName);

		if (intent.getBooleanExtra("favorites", false)) {
			loadFavorites(animeFavs);
		} else {
			loadEpisodes();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!getIntent().getBooleanExtra("favorites", false)) {
			getMenuInflater().inflate(R.menu.anime, menu);
			MenuItem fav = menu.findItem(R.id.fav);
			MenuItem watch = menu.findItem(R.id.watch);
			fav.setOnMenuItemClickListener(item -> {
				updateFavItem(item, true);
				return false;
			});
			watch.setOnMenuItemClickListener(item -> {
				updateWatchItem(item, true);
				return false;
			});
			updateFavItem(fav, false);
			updateWatchItem(watch, false);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onBackPressed() {
		WebView commentsWebView = findViewById(R.id.commentsWebView);
		if (commentsWebView.getVisibility() == View.VISIBLE) {
			commentsWebView.setVisibility(View.INVISIBLE);
			commentsWebView.loadData("", "text/html", null);
		} else if (isTaskRoot()) {
			Intent main = new Intent(this, MainActivity.class);
			startActivity(main);
			finish();
		} else super.onBackPressed();
	}

	private void updateFavItem(MenuItem item, boolean reverse) {
		try {
			MainActivity.checkUserData(this);
			boolean animeExists = MainActivity.userData.getJSONObject("favorites").has(animeName);
			item.setTitle(animeExists ? reverse ? "Add to favorites" : "Remove from favorites"
				: reverse ? "Remove from favorites" : "Add to favorites");
			item.setIcon(animeExists ? reverse ? R.drawable.ic_favorite_disabled : R.drawable.ic_favorite_enabled
				: reverse ? R.drawable.ic_favorite_enabled : R.drawable.ic_favorite_disabled);

			if (reverse) {
				if (animeExists) { MainActivity.userData.getJSONObject("favorites").remove(animeName); }
				else {
					JSONObject animeData = new JSONObject()
						.put("animeUrl", animeUrl)
						.put("animeImg", animeImg)
						.put("airing", airing);
					MainActivity.userData.getJSONObject("favorites").put(animeName, animeData);
				}
			}
			MainActivity.updateUserData(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateWatchItem(MenuItem item, boolean reverse) {
		try {
			MainActivity.checkUserData(this);
			MainActivity.CustomArrayList<String> watchList = MainActivity.getStringArrayList(MainActivity.userData, "watched");
			boolean animeExists = watchList.contains(animeName);
			int colorAccent = getResources().getColor(R.color.colorAccent, null);

			item.setTitle(animeExists ? reverse ? "Mark as completed" : "Mark as watching"
				: reverse ? "Mark as watching" : "Mark as completed");
			item.getIcon().setTint(animeExists ? reverse ? Color.WHITE : colorAccent
				: reverse ? colorAccent : Color.WHITE);

			if (reverse) {
				if (animeExists) { watchList.remove(animeName); }
				else { watchList.add(animeName); }
			}
			MainActivity.updateUserData(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private ArrayList<String[]> orderAnimes(ArrayList<String[]> list) {
		Collections.sort(list, (o1, o2) -> o1[0].compareToIgnoreCase(o2[0]));
		return list;
	}

	private ArrayList<ArrayList<String[]>> filterAnimes() {
		try {
			MainActivity.checkUserData(this);
			MainActivity.CustomArrayList<String> watched = MainActivity.getStringArrayList(MainActivity.userData, "watched");
			JSONObject favorites = MainActivity.userData.getJSONObject("favorites");
			ArrayList<ArrayList<String[]>> filtered = new ArrayList<>();
			ArrayList<String[]> completed = new ArrayList<>();
			ArrayList<String[]> watching = new ArrayList<>();
			filtered.add(completed);
			filtered.add(watching);

			for (Iterator<String> it = favorites.keys(); it.hasNext(); ) {
				String animeName = it.next();
				JSONObject anime = MainActivity.userData.getJSONObject("favorites").getJSONObject(animeName);
				(watched.contains(animeName) ? completed : watching).add(new String[]{
					animeName,
					anime.getString("animeImg"),
					anime.getString("animeUrl"),
					anime.getString("airing")
				});
			}
			return filtered;
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	public void loadFavorites(RecyclerView animeFavs) {
		this.findViewById(R.id.animeRecycler).setVisibility(View.VISIBLE);
		this.findViewById(R.id.animeScrollView).setVisibility(View.INVISIBLE);
		new Thread(() -> {
			try {
				ArrayList<ArrayList<String[]>> filteredData = filterAnimes();
				ArrayList<String[]> completed = orderAnimes(filteredData.get(0));
				ArrayList<String[]> watching = orderAnimes(filteredData.get(1));
				ArrayList<String[]> allData = new ArrayList<>();

				if (watching.size() > 0) allData.add(new String[]{"Watching"});
				for (String[] anime : watching) allData.add(anime);

				if (completed.size() > 0) allData.add(new String[]{"Completed"});
				for (String[] anime : completed) allData.add(anime);

				runOnUiThread(()->((SearchAdapter) animeFavs.getAdapter()).setData(allData));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}

	public void loadEpisodes() {
		Category episodes = new Category(animeName, findViewById(R.id.itemHolder).getRootView().getRootView(), this);
		new Thread(() -> {
			try {
				Response page_data = HttpService.HttpGet(animeUrl);
				Document doc = Jsoup.parse(page_data.body().string());
				for (Element episode : doc.getElementsByClass("d-inline-flex")) {
					episodes.addEpisode(episode.child(0).text(), episode.attr("href"));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}
}