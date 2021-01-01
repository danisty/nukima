package com.dyna.nukima;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

class ChangelogAdapter extends RecyclerView.Adapter<ChangelogAdapter.ViewHolder> {
	private ArrayList<Tuple<String, ArrayList<String>>> data; // Don't event question it
	private static HashMap<String, Integer> TagColors = new HashMap<>();
	static {
		TagColors.put("NUEVO", Color.parseColor("#FF3BC31D"));
		TagColors.put("CAMBIO", Color.parseColor("#FFD8AB24"));
		TagColors.put("ARREGLO", Color.parseColor("#FFC31D22"));
	}

	public ChangelogAdapter(ArrayList<Tuple<String, ArrayList<String>>> data) {
		this.data = data;
	}

	public static class ViewHolder extends RecyclerView.ViewHolder {
		private TextView versionName;
		private TextView changeVersion;
		private LinearLayout changesList;
		private Context context;

		public ViewHolder(@NonNull View view, Context context) {
			super(view);
			this.versionName = view.findViewById(R.id.versionName);
			this.changeVersion = view.findViewById(R.id.changeVersion);
			this.changesList = view.findViewById(R.id.changesList);
			this.context = context;
		}
	}

	@NonNull
	@Override
	public ChangelogAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.changelog_version, parent, false);
		return new ViewHolder(view, parent.getContext());
	}

	@Override
	public void onBindViewHolder(@NonNull ChangelogAdapter.ViewHolder holder, int position) {
		position = data.size() - 1 - position;
		Tuple<String, ArrayList<String>> change = data.get(position);

		LinearLayout changes = holder.changesList;
		changes.removeAllViews();

		holder.changeVersion.setText(String.valueOf(position+1));
		holder.versionName.setText(change.key);

		LayoutInflater inflater = LayoutInflater.from(holder.context);
		for (String versionChange : change.value) {
			int tagIndex = versionChange.indexOf(":");
			View changeItem = inflater.inflate(R.layout.changelog_item, changes, false);

			TextView changeTag = changeItem.findViewById(R.id.changeTag);
			TextView changeDescription = changeItem.findViewById(R.id.changeDescription);
			int tagColor = TagColors.get(versionChange.substring(0, tagIndex));

			changeDescription.setText(versionChange.substring(tagIndex+1));
			changeTag.setText(versionChange.substring(0, tagIndex));
			changeTag.setBackgroundTintList(ColorStateList.valueOf(tagColor));
			changes.addView(changeItem);
		}
	}

	@Override
	public int getItemCount() {
		return data.size();
	}
}
