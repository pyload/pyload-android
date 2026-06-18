package org.pyload.android.client.module;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.google.gson.Gson;

import java.util.Locale;

public final class Utils {

	private static Gson gson;

	public static String formatSize(long size) {
		double format = size;
		int steps = 0;
		String[] sizes = { "B", "KiB", "MiB", "GiB", "TiB" };
		while (format > 1000) {
			format /= 1024.0;
			steps++;
		}
		return String.format(Locale.US, "%.2f %s", format, sizes[steps]);
	}

	public static String getFileName(Context context, Uri uri) {
		String result = null;
		if (uri.getScheme().equals("content")) {
			try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
				if (cursor != null && cursor.moveToFirst()) {
					int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
					if (index >= 0) result = cursor.getString(index);
				}
			}
		}
		if (result == null) {
			result = uri.getLastPathSegment();
		}
		return result;
	}

	public static String encodeObject(Object o) {
		if (gson == null)
			gson = new Gson();

		return gson.toJson(o);
	}

	public static <T> T decodeObject(String json, Class<T> classOfT) {
		if (gson == null)
			gson = new Gson();

		return gson.fromJson(json, classOfT);
	}
}
