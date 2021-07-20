package com.dyna.nukima;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.ViewHolder> {
	private ArrayList<String[]> data = new ArrayList<>();
	private ArrayList<Episode> episodes = new ArrayList<>();
	private WeakReference<Activity> activity;
	public String animeName;

	EpisodeAdapter(String animeName, Activity context) {
		this.animeName = animeName;
		this.activity = new WeakReference<>(context);
	}

	public static class ViewHolder extends RecyclerView.ViewHolder {
		public Context context;
		public TextView name;
		public Button play;
		public Button comments;

		public ViewHolder(View view, Context context) {
			super(view);
			this.context = context;
			this.name = view.findViewById(R.id.name);
			this.play = view.findViewById(R.id.play);
			this.comments = view.findViewById(R.id.comments);
		}
	}

	@NonNull
	@Override
	public EpisodeAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.episode, parent, false);
		return new EpisodeAdapter.ViewHolder(v, parent.getContext());
	}

	@Override
	public void onBindViewHolder(@NonNull EpisodeAdapter.ViewHolder holder, int position) {
		String[] animeInfo = data.get(position);
		String name = animeInfo[0];
		String url = animeInfo[1];

		Episode episode = episodes.size() > position ? episodes.get(position) : null;
		if (episode == null) {
			episode = new Episode(this.animeName, name, url, this.activity.get(), holder.name.getRootView());
			episodes.add(position, episode);
		}

		episode.configure(holder);
	}

	@Override
	public int getItemCount() {
		return data != null ? data.size() : 0;
	}

	public void setData(ArrayList<String[]> data) {
		this.data = data;
		this.notifyDataSetChanged();
	}
}
