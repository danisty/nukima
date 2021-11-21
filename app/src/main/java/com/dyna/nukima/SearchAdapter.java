package com.dyna.nukima;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
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

class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
	private ArrayList<String[]> data;
	public boolean favorites = false;
	static int TYPE_ANIME = 0;
	static int TYPE_CATEGORY = 1;
	static int completedAnimesIndex;
	static int[] colors;

	public SearchAdapter() {}
	public SearchAdapter(boolean favorites) { this.favorites = favorites; }
	static class AnimeViewHolder extends RecyclerView.ViewHolder {
		public ImageView animeImage;
		public TextView animeName;
		public TextView animeStatus;
		public Button clickListener;
		public Context context;

		public AnimeViewHolder(View v, Context context) {
			super(v);
			this.animeImage = v.findViewById(R.id.animeImage);
			this.animeName = v.findViewById(R.id.animeName);
			this.animeStatus = v.findViewById(R.id.animeStatus);
			this.clickListener = v.findViewById(R.id.clickListener);
			this.context = context;
		}

		public void setDetails(String[] animeInfo, boolean favorites) {
			String animeName = animeInfo[0];
			String animeImg = animeInfo[1];
			String animeUrl = animeInfo[2];
			String airing = animeInfo[3];

			this.animeName.setText(animeName);
			this.animeImage.setImageDrawable(null);
			this.animeStatus.setText(airing);
			this.animeStatus.setTextColor(airing.equals("Airing") ? this.context.getResources().getColor(R.color.colorAccent, null) : Color.WHITE);
			this.clickListener.setOnClickListener(v -> {
				Intent intent = new Intent(this.context, AnimeActivity.class);
				intent.putExtra("animeUrl", animeUrl);
				intent.putExtra("animeImg", animeImg);
				intent.putExtra("animeName", animeName);
				intent.putExtra("airing", !favorites ? airing : null);
				this.context.startActivity(intent);
			});
			Glide.with(this.context).clear(this.animeImage);
			Glide.with(this.context).load(animeImg).fitCenter().transition(DrawableTransitionOptions.withCrossFade(100)).into(this.animeImage);
		}
	}

	 static class CategoryViewHolder extends RecyclerView.ViewHolder {
		public TextView categoryName;
		public Context context;

		public CategoryViewHolder(View v, Context context) {
			super(v);
			this.categoryName = v.findViewById(R.id.categoryName);
			this.context = context;
		}

		public void setDetails(String[] categoryInfo) {
			String categoryName = categoryInfo[0];
			this.categoryName.setText(categoryName);
		}
	}

	@Override
	public int getItemViewType(int position) {
		if (data.get(position).length == 1) {
			return TYPE_CATEGORY;
		} else {
			return TYPE_ANIME;
		}
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		if (colors == null) {
			Resources res = parent.getContext().getResources();
			colors = new int[] {
					res.getColor(R.color.colorPrimary, null),
					res.getColor(R.color.colorPrimaryDark, null)
			};
		}
		if (viewType == TYPE_ANIME) {
			View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.anime_search, parent, false);
			return new AnimeViewHolder(v, parent.getContext());
		} else {
			View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.title, parent, false);
			return new CategoryViewHolder(v, parent.getContext());
		}
	}

	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
		if (getItemViewType(position) == TYPE_ANIME) {
			((AnimeViewHolder) holder).setDetails(data.get(position), this.favorites);
		} else {
			((CategoryViewHolder) holder).setDetails(data.get(position));
		}
		holder.itemView.setBackgroundColor(position >= completedAnimesIndex ? colors[1] : colors[0]);
	}

	@Override
	public int getItemCount() {
		return data != null ? data.size() : 0;
	}

	public void setData(ArrayList<String[]> data) {
		this.data = data;
		for (int i = 0; i < data.size(); i++) {
			if (data.get(i)[0] == "Completed") {
				completedAnimesIndex = i;
				break;
			}
		}
		this.notifyDataSetChanged();
	}
}
