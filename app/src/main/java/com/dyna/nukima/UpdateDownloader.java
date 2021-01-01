package com.dyna.nukima;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

class UpdateDownloader extends AsyncTask<String, Integer, Boolean> {
	public final Object permissionResponse = new Object();
	private AlertDialog downloadProgress;
	private WeakReference<Context> context;
	private File tempApkFile;

	private WeakReference<TextView> title;
	private WeakReference<ProgressBar> progress;

	public UpdateDownloader(Context context) throws IOException {
		this.context = new WeakReference<>(context);
		this.tempApkFile = new File(context.getCacheDir() + "/nukima.apk");
		if (tempApkFile.exists()) {
			tempApkFile.delete();
			tempApkFile.createNewFile();
		}
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		this.downloadProgress = new AlertDialog.Builder(context.get())
			.setCancelable(false)
			.setView(R.layout.progress_dialog)
			.create();

		this.downloadProgress.show();
		this.title = new WeakReference<>(downloadProgress.findViewById(R.id.title));
		this.progress = new WeakReference<>(downloadProgress.findViewById(R.id.progress));
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		title.get().setText("Downloading: " + values[0] + "%");
		progress.get().setProgress(values[0]);
	}

	@Override
	protected void onPostExecute(Boolean completed) {
		super.onPostExecute(completed);
		if (completed) {
			Intent install = new Intent(Intent.ACTION_VIEW);
			Uri fileUri = FileProvider.getUriForFile(context.get(), "com.dyna.nukima.provider", tempApkFile);

			install.setDataAndType(fileUri, "application/vnd.android.package-archive");
			install.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			context.get().startActivity(install);
			downloadProgress.dismiss();
		}
	}

	@Override
	protected Boolean doInBackground(String... strings) {
		checkExternalStoragePermissions();
		if (!isGranted(android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
			return doInBackground(strings);

		InputStream input = null;
		OutputStream output = null;
		HttpURLConnection connection = null;
		try {
			URL url = new URL(strings[0]);
			connection = (HttpURLConnection) url.openConnection();
			connection.connect();

			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return false;
			}

			int fileLength = connection.getContentLength();
			input = connection.getInputStream();
			output = new FileOutputStream(tempApkFile.getAbsolutePath());

			int count;
			long total = 0;
			byte[] data = new byte[4028];
			while ((count = input.read(data)) != -1) {
				if (isCancelled()) {
					input.close();
					return false;
				}
				total += count;
				if (fileLength > 0)
					publishProgress((int) (total * 100 / fileLength));
				output.write(data, 0, count);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (output != null)
					output.close();
				if (input != null)
					input.close();
			} catch (IOException ignored) {}
			if (connection != null)
				connection.disconnect();
		}
		return true;
	}

	private void checkExternalStoragePermissions() {
		try {
			if (!isGranted(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) || !isGranted(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
				String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
				ActivityCompat.requestPermissions((Activity) context.get(), permissions, 1);
				synchronized (permissionResponse) {
					permissionResponse.wait();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean isGranted(String permission) {
		return context.get().checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
	}
}
