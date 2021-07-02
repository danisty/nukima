package com.dyna.nukima;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

class UpdateDownloader extends AsyncTask<String, Integer, Boolean> {
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
}
