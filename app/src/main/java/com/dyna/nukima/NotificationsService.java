package com.dyna.nukima;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationCompat;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;

public class NotificationsService extends JobService {
	public static String CHANNEL_ID = "anime_updates";
	public static String GROUP_ID = "anime_updates_group";
	private boolean jobCancelled = false;

	@Override
	public boolean onStartJob(JobParameters params) {
		// sendNotification(getApplicationContext(), "Checking Animes", "OWO");
		doBackgroundWork(params, getApplicationContext());
		return true;
	}

	@Override
	public boolean onStopJob(JobParameters params) {
		jobCancelled = true;
		return true;
	}

	private void doBackgroundWork(JobParameters params, Context context) {
		new Thread(() -> {
			try {
				MainActivity.checkUserData(this);
				if (jobCancelled) return;

				Document soup = Jsoup.connect("https://www.animefenix.com").get();
				MainActivity.CustomArrayList<String> recentEpisodes = MainActivity.getStringArrayList(MainActivity.userData, "recentEpisodes");
				MainActivity.CustomArrayList<String> recentAnimes = MainActivity.getStringArrayList(MainActivity.userData, "recentAnimes");
				MainActivity.CustomArrayList<String> actualRecentEpisodes = new MainActivity.CustomArrayList<>();
				MainActivity.CustomArrayList<String> actualRecentAnimes = new MainActivity.CustomArrayList<>();
				JSONObject favorites = MainActivity.userData.getJSONObject("favorites");

				for (Element episode : soup.getElementsByClass("capitulos-grid").get(0).children()) {
					if (jobCancelled) return;
					Element animeInfo = episode.getElementsByClass("overarchingdiv").get(0).child(0);
					String animeName = animeInfo.attr("title");
					actualRecentEpisodes.add(animeName);

					if (!recentEpisodes.contains(animeName)) {
						String animeUrl = animeInfo.attr("href");
						// recentEpisodes.add(escapedEpisodeName);

						if (!MainActivity.ignoreNews) {
							String[] episodeAnimeInfo = MainActivity.getAnimeInfo(animeName, animeUrl);
							Intent animeIntent = new Intent(context, AnimeActivity.class);
							animeIntent.putExtra("animeUrl", episodeAnimeInfo[0]);
							animeIntent.putExtra("animeImg", episodeAnimeInfo[1]);
							animeIntent.putExtra("animeName", episodeAnimeInfo[2]);
							animeIntent.putExtra("airing", episodeAnimeInfo[3]);

							int icon = favorites.has(episodeAnimeInfo[2]) ? R.drawable.ic_favorite : R.drawable.ic_addbox;
							sendNotification(context, episodeAnimeInfo[2], "Episode " + episodeAnimeInfo[4], icon, animeIntent);
						}
					}
				}
				for (Element anime : soup.getElementsByClass("list-series").get(0).children()) {
					if (jobCancelled) return;
					Element animeInfo = anime.getElementsByClass("image").get(0).child(0);
					String animeName = animeInfo.attr("title");
					actualRecentAnimes.add(animeName);

					if (!recentAnimes.contains(animeName)) {
						String animeImg = animeInfo.child(0).attr("src");
						String animeUrl = animeInfo.attr("href");
						String airing = anime.getElementsByClass("airing").size() > 0 ? "Airing" : "Finished";
						// recentAnimes.add(animeName);

						if (!MainActivity.ignoreNews) {
							Intent animeIntent = new Intent(context, AnimeActivity.class);
							animeIntent.putExtra("animeUrl", animeUrl);
							animeIntent.putExtra("animeImg", animeImg);
							animeIntent.putExtra("animeName", animeName);
							animeIntent.putExtra("airing", airing);

							sendNotification(context, animeName, "New anime!", R.drawable.ic_addcircle, animeIntent);
						}
					}
				}

				if (actualRecentEpisodes.size() == 0 || actualRecentAnimes.size() == 0)
					return; // We don't want to store empty lists in case it fails to fetch data from the server

				MainActivity.userData.put("recentEpisodes", actualRecentEpisodes);
				MainActivity.userData.put("recentAnimes", actualRecentAnimes);

				MainActivity.ignoreNews = false;
				MainActivity.updateUserData(context);
				scheduleRefresh(); jobFinished(params, false);
				//sendNotification(context, "Job finished!", "UWU");
			} catch (Exception e) {
				e.printStackTrace();
				//sendNotification(context, "Something went wrong", "UNU: " + e.getMessage());
			}
		}).start();
	}

	private void scheduleRefresh() {
		ComponentName component = new ComponentName(getPackageName(), NotificationsService.class.getName());
		JobInfo info = new JobInfo.Builder(25, component)
			.setMinimumLatency(5 * 60 * 1000)
			.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
			.setPersisted(true)
			.build();

		JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
		jobScheduler.schedule(info);
	}

	private int getNonUsedId(NotificationManager nManager) {
		ArrayList<Integer> ids = new ArrayList<>();
		for (StatusBarNotification notification : nManager.getActiveNotifications()) {
			ids.add(notification.getId());
		}

		int nonUsedId = 0;
		while (nonUsedId++ >= 0) {
			if (!ids.contains(nonUsedId))
				break;
		}
		return nonUsedId;
	}

	private void sendNotification(Context context, String title, String description, int icon, Intent pendingIntent) throws InterruptedException {
		NotificationManager nManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		int REQUEST_ID = getNonUsedId(nManager);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
			.setSmallIcon(icon)
			.setColor(context.getResources().getColor(R.color.colorAccent, null))
			.setContentTitle(title)
			.setContentText(description)
			.setGroup(GROUP_ID)
			.setAutoCancel(true)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setContentIntent(PendingIntent.getActivity(context, REQUEST_ID, pendingIntent, PendingIntent.FLAG_UPDATE_CURRENT));

		NotificationCompat.Builder summary = null;
		if (nManager.getActiveNotifications().length > 0) {
			summary = new NotificationCompat.Builder(context, CHANNEL_ID)
				.setSmallIcon(R.drawable.ic_inbox)
				.setGroup(GROUP_ID)
				.setGroupSummary(true)
				.setStyle(new NotificationCompat.InboxStyle())
				.setPriority(NotificationCompat.PRIORITY_DEFAULT)
				.setColor(context.getResources().getColor(R.color.colorAccent, null));
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nManager.getNotificationChannel(CHANNEL_ID) == null) {
			NotificationChannel nChannel = new NotificationChannel(CHANNEL_ID, "Anime Updates", NotificationManager.IMPORTANCE_HIGH);
			nManager.createNotificationChannel(nChannel);
		}
		nManager.notify(REQUEST_ID, builder.build());
		if (summary != null) {
			Thread.sleep(200);
			nManager.notify(999, summary.build());
		}
	}
}
