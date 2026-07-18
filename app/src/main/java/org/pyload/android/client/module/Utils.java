package org.pyload.android.client.module;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.google.gson.Gson;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class Utils {

	private static Gson gson;

    public static void hideKeyboard(View view) {
        if (view == null) return;
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

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
		if (uri.getScheme() != null && uri.getScheme().equals("content")) {
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

	public static Map<String, String> parseQueryParams(String query) {
		Map<String, String> params = new HashMap<>();
		if (query == null || query.isEmpty()) return params;
		try {
			for (String param : query.split("&")) {
				String[] pair = param.split("=");
				String key = URLDecoder.decode(pair[0], "UTF-8");
				String value = "";
				if (pair.length > 1) {
					value = URLDecoder.decode(pair[1], "UTF-8");
				}
				params.put(key, value);
			}
		} catch (Exception ignored) {}
		return params;
	}

	public static byte[] hexToBytes(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}
}
