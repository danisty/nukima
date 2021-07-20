package com.dyna.nukima;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.ArrayList;

class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.MyViewHolder> {
	private ArrayList<String[]> data = new ArrayList<>();

	public static class MyViewHolder extends RecyclerView.ViewHolder {
		private TextView animeName;
		private ImageView animeImg;
		private TextView episodeNum;
		private Button clickListener;
		private Context context;

		public MyViewHolder(View view, Context context) {
			super(view);
			this.animeName = view.findViewById(R.id.animeName);
			this.animeImg = view.findViewById(R.id.animeImage);
			this.episodeNum = view.findViewById(R.id.episodeNum);
			this.clickListener = view.findViewById(R.id.clickListener);
			this.context = context;
		}
	}

	@NonNull
	@Override
	public HistoryAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.anime_history, parent, false);
		return new HistoryAdapter.MyViewHolder(v, parent.getContext());
	}

	@Override
	public void onBindViewHolder(@NonNull HistoryAdapter.MyViewHolder holder, int position) {
		String[] animeInfo = data.get(position);
		String animeName = animeInfo[0];
		String animeImg = animeInfo[1];
		String episodeNum = animeInfo[2];
		String animeUrl = animeInfo[3];
		String airing = animeInfo[4];

		holder.animeName.setText(animeName);
		holder.episodeNum.setText(episodeNum);
		Glide.with(holder.context).clear(holder.animeImg);
		Glide.with(holder.context).load(animeImg).transition(DrawableTransitionOptions.withCrossFade(100)).into(holder.animeImg);
		holder.clickListener.setOnClickListener(v -> {
			Intent intent = new Intent(holder.context, AnimeActivity.class);
			intent.putExtra("animeUrl", animeUrl);
			intent.putExtra("animeImg", animeImg);
			intent.putExtra("animeName", animeName);
			holder.context.startActivity(intent);
		});
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
