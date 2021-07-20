package com.dyna.nukima;

import android.app.Activity;
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

import com.squareup.picasso.Picasso;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class AnimeAdapter extends RecyclerView.Adapter<AnimeAdapter.MyViewHolder> {
	private LayoutInflater inflater;
	private ArrayList<String[]> data;
	private WeakReference<Activity> activity;

	AnimeAdapter(Activity context) {
		this.activity = new WeakReference<>(context);
	}

	public static class MyViewHolder extends RecyclerView.ViewHolder {
		public View parent;
		public ImageView animeImage;
		public TextView animeName;
		public Button clickListener;
		public Context context;

		public MyViewHolder(View v, Context context) {
			super(v);
			this.parent = v;
			this.animeImage = v.findViewById(R.id.img);
			this.animeName = v.findViewById(R.id.name);
			this.clickListener = v.findViewById(R.id.clickListener);
			this.context = context;
		}
	}

	@NonNull
	@Override
	public AnimeAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		if (inflater == null) inflater = LayoutInflater.from(parent.getContext());
		View v = inflater.inflate(R.layout.anime, parent, false);
		return new AnimeAdapter.MyViewHolder(v, parent.getContext());
	}

	@Override
	public void onBindViewHolder(MyViewHolder holder, int position) {
		String[] animeInfo = data.get(position);
		String animeName = animeInfo[0];
		String animeImg = animeInfo[1];
		String animeUrl = animeInfo[2];
		String airing = animeInfo[3];

		holder.clickListener.setLongClickable(true);
		holder.animeName.setText(animeName.length() > 25 ? animeName.substring(0, 22).trim() + "..." : animeName);
		holder.clickListener.setOnClickListener(v -> {
			if (!animeUrl.contains("www.animefenix.com/ver")) {
				Intent intent = new Intent(holder.context, AnimeActivity.class);
				intent.putExtra("animeUrl", animeUrl);
				intent.putExtra("animeImg", animeImg);
				intent.putExtra("animeName", animeName);
				intent.putExtra("airing", airing);
				holder.context.startActivity(intent);
			} else {
				Pattern pattern = Pattern.compile("(.+) (\\d+)");
				Matcher matcher = pattern.matcher(animeName); matcher.find();
				new Thread(new Episode(animeName, animeName, animeUrl, this.activity.get(), null)::watch).start();
			}
		});
		holder.clickListener.setOnLongClickListener(view -> {
			if (animeUrl.contains("www.animefenix.com/ver")) {
				new Thread(() -> {
					try {
						String[] episodeAnimeInfo = MainActivity.getAnimeInfo(animeName, animeUrl);
						Intent intent = new Intent(holder.context, AnimeActivity.class);
						intent.putExtra("animeUrl", episodeAnimeInfo[0]);
						intent.putExtra("animeImg", episodeAnimeInfo[1]);
						intent.putExtra("animeName", episodeAnimeInfo[2]);
						intent.putExtra("airing", episodeAnimeInfo[3]);
						holder.context.startActivity(intent);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}).start();
			}
			return false;
		});

		Picasso.with(holder.context).cancelRequest(holder.animeImage);
		//Picasso picasso = new Picasso.Builder(holder.context).downloader(new CookieImageDownloader(holder.context)).build();
		Picasso.with(holder.context).load(animeImg).fit().centerCrop().into(holder.animeImage);

		float scale = holder.context.getResources().getDisplayMetrics().density;
		RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) holder.parent.getLayoutParams();
		lp.setMargins(0, 0, position == (data.size() - 1) ? 0 : (int) (5 * scale + 0.5f), 0);
		holder.parent.setLayoutParams(lp);
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
