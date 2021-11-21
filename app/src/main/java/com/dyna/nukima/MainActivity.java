package com.dyna.nukima;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.Task;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
	private UpdateDownloader updateDownloader;
	public RecyclerView animeSearch;
	private SearchView searchView;
	public static JSONObject userData;
	public static boolean ignoreNews = false;
	public static WeakReference<WebView> mainWebView;
	public static Map<String, String> cookies;
	public static String cookiesRaw;
	public final Object permissionResponse = new Object();
	public SwipeRefreshLayout pullToRefresh;

	public MainActivity() {
	}

	public static class CustomArrayList<E> extends ArrayList<E> {

		@NonNull
		@Override
		@RequiresApi(api = Build.VERSION_CODES.N)
		public String toString() {
			String result = "";
			for (E value : this) {
				String escapedValue = StringEscapeUtils.escapeJson(value.toString());
				result = result.equals("") ? ('"' + escapedValue + '"') : (result + ",\"" + escapedValue + '"');
			}
			return "[" + result  + "]";
		}
	}

	private static Object getArrayData(JSONObject jsonObject, String key) {
		try {
			if (!jsonObject.has(key))
				jsonObject.put(key, new ArrayList<>());
			Object tempList = jsonObject.get(key);
			return tempList.getClass() == JSONArray.class ? tempList.toString() :
				tempList.getClass() == String.class ? (String) tempList : (ArrayList) tempList;
		} catch (Exception e) {
			e.printStackTrace();
			return "[]";
		}
	}

	public static ArrayList<Integer> getIntegerArrayList(JSONObject jsonObject, String key) throws JSONException {
		String string;
		Object tempData = getArrayData(jsonObject, key);
		if (tempData.getClass() == ArrayList.class) {
			ArrayList<Integer> data = (ArrayList<Integer>) tempData;
			jsonObject.put(key, data);
			return data;
		}
		else { string = (String) tempData; }

		ArrayList<Integer> data = new ArrayList<>();
		jsonObject.put(key, data);
		if (string.length() == 2) {
			return data;
		}

		String[] integers = string.substring(1, string.length()-1).split(",");
		for (String integer : integers) {
			data.add(Integer.parseInt(integer.trim()));
		}
		return data;
	}

	public static CustomArrayList<String> getStringArrayList(JSONObject jsonObject, String key) throws JSONException {
		String string;
		Object tempData = getArrayData(jsonObject, key);
		if (tempData.getClass() == CustomArrayList.class) {
			CustomArrayList<String> data = (CustomArrayList<String>) tempData;
			jsonObject.put(key, data);
			return data;
		}
		else { string = (String) tempData; }

		CustomArrayList<String> data = new CustomArrayList<>();
		jsonObject.put(key, data);
		if (string.length() == 2) {
			return data;
		}

		JSONArray strings;
		try {
			strings = new JSONArray(string);
		} catch (Exception ignore) {
			// Failed parsing JSON, probably due to an old version stringify issue, keep it empty for refreshing
			strings = new JSONArray();
			ignoreNews = true;
		}

		for (int position = 0; position < strings.length(); position++) {
			data.add(strings.getString(position));
		}
		return data;
	}

	public void scheduleJob() {
		ComponentName component = new ComponentName(this, NotificationsService.class);
		JobInfo info = new JobInfo.Builder(25, component)
			.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
			.setPersisted(true)
			.build();

		ignoreNews = true;
		JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
		jobScheduler.schedule(info);
	}

	public Map<String, String> map(String cookies) {
		Map<String, String> cookiesList = new HashMap<>();
		for (String cookie : cookies.split(";")) {
			String[] cookieData = cookie.trim().split("=");
			cookiesList.put(cookieData[0], cookieData[1]);
		}
		return cookiesList;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setSupportActionBar(findViewById(R.id.toolbar));
		checkUserData(this);

		animeSearch = findViewById(R.id.animeSearch);
		animeSearch.setHasFixedSize(true);

		LinearLayoutManager layoutManager = new LinearLayoutManager(this);
		animeSearch.setLayoutManager(layoutManager);

		SearchAdapter mAdapter = new SearchAdapter();
		animeSearch.setAdapter(mAdapter);

		checkForUpdates();
		loadCategories();
		showMainPage();
		scheduleJob();

		pullToRefresh = findViewById(R.id.pullToRefresh);
		pullToRefresh.setOnRefreshListener(() -> {
			showMainPage();
		});

		// -- Cloudflare Bypass -- //
		/*mainWebView = new WeakReference<>(findViewById(R.id.mainWebView));
		mainWebView.get().getSettings().setJavaScriptEnabled(true);
		mainWebView.get().setWebViewClient(new WebViewClient(){
			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				if (url.startsWith("https://www.animefenix.com")) {
					urlSecret = url;
					cookiesRaw = CookieManager.getInstance().getCookie(url);
					cookies = map(cookiesRaw);
					System.out.println(cookiesRaw);

					mainWebView.get().setVisibility(View.GONE);
					showMainPage();
				}
			}
		});
		mainWebView.get().setWebChromeClient(new WebChromeClient());
		mainWebView.get().loadUrl("https://www.animefenix.com");*/
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		MenuItem search = menu.findItem(R.id.menu_search);
		MenuItem fav = menu.findItem(R.id.menu_fav);
		MenuItem history = menu.findItem(R.id.menu_history);
		MenuItem importExport = menu.findItem(R.id.menu_import);

		fav.setOnMenuItemClickListener(item -> {
			Intent intent = new Intent(this, AnimeActivity.class);
			intent.putExtra("animeName", "Favorites");
			intent.putExtra("favorites", true);
			startActivity(intent);
			return false;
		});

		history.setOnMenuItemClickListener(item -> {
			Intent intent = new Intent(this, HistoryActivity.class);
			startActivity(intent);
			return false;
		});

		searchView = (SearchView) search.getActionView();
		searchView.setMaxWidth(Integer.MAX_VALUE);
		search.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {
				setItemsVisibility(menu, item, false);
				animeSearch.setVisibility(View.VISIBLE);
				animeSearch.bringToFront();
				return true;
			}

			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				animeSearch.setVisibility(View.INVISIBLE);
				setItemsVisibility(menu, item, true);
				return true;
			}
		});

		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) { // Hide keyboard
				Activity activity = MainActivity.this;
				InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
				View view = activity.getCurrentFocus();

				if (view == null) { view = new View(activity); }
				imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
				view.clearFocus();

				return false;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				if (newText.length() > 0) {
					new Thread(() -> {
						try {
							ArrayList<String[]> animes = new ArrayList<>();
							Response page_data = HttpService.HttpGet("https://www.animefenix.com/animes?q=" + newText);

							if (!newText.equals(searchView.getQuery().toString())) { return; }
							Document doc = Jsoup.parse(page_data.body().string());

							for (Element anime : doc.getElementsByClass("list-series").get(0).children()) {
								Element animeInfo = anime.getElementsByClass("image").get(0).child(0);
								String[] animeData = {
									animeInfo.attr("title"),
									animeInfo.child(0).attr("src"),
									animeInfo.attr("href"),
									anime.getElementsByClass("image").get(0).childrenSize() == 4 ? "Airing" : "Finished"
								};
								animes.add(animeData);
							}

							runOnUiThread(()->((SearchAdapter) animeSearch.getAdapter()).setData(animes));
						} catch (Exception e) { e.printStackTrace(); }
					}).start();
				} else {
					((SearchAdapter) animeSearch.getAdapter()).setData(new ArrayList<>());
				}
				return false;
			}
		});

		String[] options = {"Import Data", "Export Data"};
		importExport.setOnMenuItemClickListener(item -> {
			new Thread(() -> {
				checkExternalStoragePermissions();
				if (!isGranted(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
					runOnUiThread(()->Toast.makeText(this, "Error. Permissions not granted.", Toast.LENGTH_LONG).show());
					return;
				}

				runOnUiThread(() -> new AlertDialog.Builder(this)
					.setTitle("Data Options")
					.setItems(options, (self, option) -> {
						if (option == 0) { // Import Data
							Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
							intent.addCategory(Intent.CATEGORY_OPENABLE);
							intent.setType("*/*");
							startActivityForResult(Intent.createChooser(intent, "Choose file to import"), 9161);
						} else { // Export Data
							Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
							intent.addCategory(Intent.CATEGORY_OPENABLE);
							intent.setType("application/json");
							intent.putExtra(Intent.EXTRA_TITLE, "nukima_data.json");
							startActivityForResult(Intent.createChooser(intent, "Choose directory to export to"), 5522);
						}
					}).show());
			}).start();
			return false;
		});
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onBackPressed() {
		if (!searchView.isIconified()) {
			searchView.setIconified(true);
			searchView.onActionViewCollapsed();
		} else {
			super.onBackPressed();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (updateDownloader != null) {
			updateDownloader.cancel(true);
		}
	}

	private void checkForUpdates() {
		try {
			Activity main = this;
			StorageReference storage = FirebaseStorage.getInstance().getReference();
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			File versionCodeFile = new File(getFilesDir().getAbsolutePath() + File.separator + "version_name");
			if (!versionCodeFile.exists()) versionCodeFile.createNewFile();
			updateDownloader = new UpdateDownloader(this);

			String localVersionCode = readFile(versionCodeFile);
			Task<Uri> apkUrl = storage.child("nukima.apk").getDownloadUrl();
			Task<Uri> clogUrl = storage.child("changelog").getDownloadUrl();
			storage.child("version_code").getDownloadUrl().addOnSuccessListener(url -> new Thread(() -> {
				try {
					Response versionInfo = HttpService.HttpGet(url.toString());
					int serverVersionCode = Integer.parseInt(versionInfo.body().string());
					int versionCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ?
						(int) pInfo.getLongVersionCode() : pInfo.versionCode;

					if (versionCode != serverVersionCode) {
						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(main)
							.setTitle("New version available!")
							.setMessage("Would you like to install the latest version of NUKIMA?");

						dialogBuilder.setNegativeButton("NO, THANKS", null);
						dialogBuilder.setPositiveButton("YEAH", (dialogInterface, i) -> updateDownloader.execute(apkUrl.getResult().toString()));

						dialogBuilder.setCancelable(false);
						runOnUiThread(() -> dialogBuilder.create().show());
					} else if (localVersionCode.isEmpty() || versionCode != Integer.parseInt(localVersionCode)) {
						if (!localVersionCode.isEmpty()) {
							String changelogUrl = clogUrl.getResult().toString();
							JSONObject changelog = new JSONObject(HttpService.HttpGet(changelogUrl).body().string());
							showChangeLog(changelog);
						}

						FileOutputStream versionCodeStream = new FileOutputStream(versionCodeFile);
						versionCodeStream.write(Integer.toString(versionCode).getBytes());
						versionCodeStream.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}).start());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void showChangeLog(JSONObject changelog) {
		try {
			ArrayList<Tuple<String, ArrayList<String>>> data = new ArrayList<>();
			for (Iterator<String> it = changelog.keys(); it.hasNext(); ) {
				String version = it.next();
				JSONArray logsArray = changelog.getJSONArray(version);

				ArrayList<String> logs = new ArrayList<>();
				for (int i = 0; i < logsArray.length(); i++) {
					logs.add(logsArray.getString(i));
				}
				data.add(new Tuple<>(version, logs));
			}

			runOnUiThread(() -> {
				View layout = LayoutInflater.from(this).inflate(R.layout.recycler_view, null);
				new AlertDialog.Builder(this)
					.setPositiveButton("Close", null) .setView(layout)
					.create() .show();

				RecyclerView recycler = layout.findViewById(R.id.recycler);
				recycler.setHasFixedSize(true);

				LinearLayoutManager layoutManager = new LinearLayoutManager(this);
				recycler.setLayoutManager(layoutManager);

				ChangelogAdapter mAdapter = new ChangelogAdapter(data);
				recycler.setAdapter(mAdapter);
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void handleException(Exception e, Context context) {
		if (context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
			File report = new File(context.getExternalFilesDir(null).getAbsolutePath() + "errorReport.txt");
			try {
				if (!report.exists()) report.createNewFile();
				FileOutputStream stream = new FileOutputStream(report);
				Toast.makeText(context, Arrays.toString(e.getStackTrace()), Toast.LENGTH_LONG).show();
				stream.write(Arrays.toString(e.getStackTrace()).getBytes());
				stream.close();
			} catch (Exception x) {
				Toast.makeText(context, Arrays.toString(x.getStackTrace()), Toast.LENGTH_LONG).show();
			}
		}
	}

	public static void checkUserData(Context context) {
		try {
			if (userData != null) return;
			File userDataFile = new File(context.getFilesDir().getAbsolutePath() + "/userdata.json");
			File backupFile = new File(context.getExternalFilesDir(null) + File.separator + "backup.json");
			String userDataString;
			if (userDataFile.exists() && readFile(userDataFile).length() > 118) { // 118 -> default userdata size
				userDataString = readFile(userDataFile);
				exportFile(userDataFile, context.getExternalFilesDir(null), "backup.json");
			} else if (backupFile.exists()) {
				userDataString = readFile(backupFile);
			} else {
				userDataFile.createNewFile();
				FileOutputStream userDataStream = new FileOutputStream(userDataFile);

				userDataString = CharStreams.toString(new InputStreamReader(context.getAssets().open("default.json"), Charsets.UTF_8));
				userDataStream.write(userDataString.getBytes());
				userDataStream.getFD().sync();
				userDataStream.close();
			}
			System.out.println(userDataString);
			userData = new JSONObject(userDataString);
		}
		catch (IOException | JSONException e) {
			e.printStackTrace();
			handleException(e, context);
		}
	}

	public static void updateUserData(Context context) {
		checkUserData(context);
		try {
			File userDataFile = new File(context.getFilesDir().getAbsolutePath() + "/userdata.json");
			String userDataString = userData.toString();

			if (userDataString.isEmpty()) {
				Toast.makeText(context, "User data wasn't saved, attempt to save an empty string.", Toast.LENGTH_LONG).show();
			} else {
				FileOutputStream stream = new FileOutputStream(userDataFile);
				stream.write(userDataString.getBytes());
				stream.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
			handleException(e, context);
		}
	}

	public static String[] getAnimeInfo(String episodeName, String episodeUrl) {
		try {
			int index = episodeUrl.lastIndexOf("-");
			String animeUrl = episodeUrl.substring(0, index).replace("/ver", "");

			Document soup = Jsoup.connect(animeUrl).get();
			ArrayList<String> info = new ArrayList<>();

			info.add(animeUrl);
			info.add(soup.getElementsByClass("image").get(0).child(0).attr("src"));
			info.add(episodeName.substring(0, episodeName.lastIndexOf(" ")));
			info.add(soup.getElementsByClass("is-success").size() > 0 ? "Airing" : "Finished");
			info.add(episodeName.substring(episodeName.lastIndexOf(" ")+1));

			return info.toArray(new String[]{});
		} catch (Exception e) {
			e.printStackTrace();
			return new String[]{};
		}
	}

	public static String readFile(File file) {
		StringBuilder data = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;

			while ((line = br.readLine()) != null) {
				if (data.length() > 0)
					data.append('\n');
				data.append(line);
			}
			br.close();
		}
		catch (IOException e) { e.printStackTrace(); }
		return data.toString();
	}

	private static void exportFile(File src, @Nullable File dst, String fileName) throws IOException {
		if (dst == null)
			return;
		if (!dst.exists()) {
			dst.mkdir();
		}

		File expFile = new File(dst.getPath() + File.separator + fileName);
		if (expFile.exists()) { expFile.delete(); }
		FileChannel inChannel = null;
		FileChannel outChannel = null;

		try {
			inChannel = new FileInputStream(src).getChannel();
			outChannel = new FileOutputStream(expFile).getChannel();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		try {
			inChannel.transferTo(0, inChannel.size(), outChannel);
		} finally {
			if (inChannel != null)
				inChannel.close();
			if (outChannel != null)
				outChannel.close();
		}
	}

	private void setItemsVisibility(Menu menu, MenuItem exception, boolean visible) {
		for (int i=0; i < menu.size(); ++i) {
			MenuItem item = menu.getItem(i);
			if (item != exception) item.setVisible(visible);
		}
	}

	public interface AnimeFilter {
		boolean filter(String a) throws JSONException;
	}
	public void addAnimes(String listClassName, String infoClassName, Category category, AnimeFilter filter, Document doc) {
		new Thread(() -> {
			try {
				ArrayList<String[]> animes = new ArrayList<>();
				for (Element anime : doc.getElementsByClass(listClassName).get(0).children()) {
					Element animeInfo = anime.getElementsByClass(infoClassName).get(0).child(0);
					if (filter.filter(animeInfo.attr("title"))) {
						animes.add(new String[]{
							animeInfo.attr("title"),
							animeInfo.child(0).attr("src"),
							animeInfo.attr("href"),
							anime.getElementsByClass("airing").size() > 0 ? "Airing" : "Finished"
						});
					}
				}
				runOnUiThread(() -> {
					category.layout.setVisibility(animes.size() == 0 ? View.GONE : View.VISIBLE);
					((AnimeAdapter) ((RecyclerView) category.layout.findViewById(R.id.category)).getAdapter()).setData(animes);
					if (category == favoritesCategory)
						pullToRefresh.setRefreshing(false);
				});
			} catch (Exception e) {
				e.printStackTrace();
				handleException(e, this);
			}
		}).start();
	}

	Category favoritesCategory = null;
	Category newCategory = null;
	Category recentCategory = null;
	Category popularCategory = null;

	public void loadCategories() {
		favoritesCategory = new Category("New Favorites", null, this);
		newCategory = new Category("New Episodes", null, this);
		recentCategory = new Category("Recently Added", null, this);
		popularCategory = new Category("Popular", null, this);

		View itemHolder = findViewById(R.id.itemHolder);

		favoritesCategory.inflateView(itemHolder);
		newCategory.inflateView(itemHolder);
		recentCategory.inflateView(itemHolder);
		popularCategory.inflateView(itemHolder);

		// Don't show it in case there's no favorite animes
		favoritesCategory.layout.setVisibility(View.GONE);
	}

	public void showMainPage() {
		new Thread(()->{
			try {
				JSONObject favorites = userData.getJSONObject("favorites");
				Document doc = Jsoup.connect("https://animefenix.com/").get();
				AtomicInteger count = new AtomicInteger();

				addAnimes("capitulos-grid", "overarchingdiv", newCategory, a -> true, doc);
				addAnimes("list-series", "image", recentCategory, a -> true, doc);
				addAnimes("home-slider", "image", popularCategory, a -> true, doc);
				addAnimes("capitulos-grid", "overarchingdiv", favoritesCategory, a -> {
					for (Iterator<String> it = favorites.keys(); it.hasNext();) {
						if (a.contains(it.next())) {
							count.getAndIncrement();
							return true;
						}
					}
					return false;
				}, doc);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}).start();

		/*mainWebView.get().evaluateJavascript("document.body.parentElement.innerHTML", html -> {
			Document doc = Jsoup.parse("<html>" + StringEscapeUtils.unescapeJava(html) + "</html>");
			addAnimes("capitulos-grid", "overarchingdiv", newCategory, doc);
			addAnimes("list-series", "image", recentCategory, doc);
			addAnimes("owl-stage", "image", popularCategory, doc);
			runOnUiThread(()->mainWebView.get().setVisibility(View.INVISIBLE));
		});*/
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK && data != null) {
			try {
				switch (requestCode) {
					case 9161: // Import Data
						InputStream importStream = getContentResolver().openInputStream(data.getData());
						userData = new JSONObject(CharStreams.toString(new InputStreamReader(importStream, Charsets.UTF_8)));
						updateUserData(this);
						importStream.close();

						ignoreNews = true;
						Toast.makeText(this, "Imported data successfully.", Toast.LENGTH_LONG).show();
						break;
					case 5522: // Export Data
						OutputStream exportStream = getContentResolver().openOutputStream(data.getData());
						exportStream.write(userData.toString().getBytes());
						exportStream.close();

						Toast.makeText(this, "Exported data successfully.", Toast.LENGTH_LONG).show();
						break;
				}
			} catch (Exception e) {
				e.printStackTrace();
				Toast.makeText(this, "Error. Couldn't get path.", Toast.LENGTH_LONG).show();
			}
		}
	}

	private void checkExternalStoragePermissions() {
		try {
			if (!isGranted(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) || !isGranted(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
				String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
				ActivityCompat.requestPermissions(this, permissions, 1);
				synchronized (permissionResponse) {
					permissionResponse.wait();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean isGranted(String permission) {
		return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		synchronized (permissionResponse) {
			permissionResponse.notify();
		}
	}
}