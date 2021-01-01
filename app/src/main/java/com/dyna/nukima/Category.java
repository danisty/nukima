package com.dyna.nukima;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

class Category {
	public String name;
	public Activity context;
	public View layout;
	public ArrayList<View> items;

	public Category(String name, @Nullable View customCategory, Activity context) {
		this.name = name;
		this.context = context;
		this.items = new ArrayList<>();

		this.layout = customCategory == null ? context.getLayoutInflater().inflate(R.layout.category, null) : customCategory;
		if (customCategory == null) { configure(this.layout); }
	}

	private void configure(final View category) {
		final Category self = this;
		this.context.runOnUiThread(() -> {
			TextView categoryName = category.findViewById(R.id.categoryName);
			categoryName.setText(self.name);

			RecyclerView recycler = category.findViewById(R.id.category);
			LinearLayoutManager layoutManager = new LinearLayoutManager(self.context, LinearLayoutManager.HORIZONTAL, false);
			AnimeAdapter mAdapter = new AnimeAdapter();

			recycler.setHasFixedSize(true);
			recycler.setLayoutManager(layoutManager);
			recycler.setAdapter(mAdapter);
		});
	}

	public void addEpisode(String name, String url) {
		Episode episode = new Episode(name, url, this);
		episode.inflateView(this.context);
	}

	public void inflateView(@Nullable View parent) {
		((ViewGroup) parent).addView(layout);
	}
}
