package com.dyna.nukima;

import android.os.Bundle;

import org.json.JSONObject;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class HistoryActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_history);

		Toolbar toolbar = findViewById(R.id.history_toolbar);
		setSupportActionBar(toolbar);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);

		toolbar.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.ic_back_arrow));
		toolbar.setNavigationOnClickListener(v -> finish());

		RecyclerView historyRecycler = findViewById(R.id.historyRecycler);
		historyRecycler.setHasFixedSize(true);

		LinearLayoutManager layoutManager = new LinearLayoutManager(this);
		historyRecycler.setLayoutManager(layoutManager);

		RecyclerView.Adapter mAdapter = new HistoryAdapter();
		historyRecycler.setAdapter(mAdapter);
		loadHistory(historyRecycler);
	}

	private void loadHistory(RecyclerView historyRecycler) {
		new Thread(()->{
			try {
				MainActivity.checkUserData(this);
				JSONObject history = MainActivity.userData.getJSONObject("history");
				if (history.names() == null) { return; }
				ArrayList<String[]> animes = new ArrayList<>();
				for (int i = history.length(); i > Math.max(history.length()-100, 0); i--) {
					String animeName = history.names().getString(i-1);
					JSONObject anime = history.getJSONObject(animeName);
					animes.add(new String[]{
						anime.getString("animeName"),
						anime.getString("animeImg"),
						anime.getString("episodeNum"),
						anime.getString("animeUrl"),
						anime.getString("airing")
					});
				}
				runOnUiThread(()->((HistoryAdapter) historyRecycler.getAdapter()).setData(animes));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}
}