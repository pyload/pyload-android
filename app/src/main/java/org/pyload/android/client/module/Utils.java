package org.pyload.android.client.module;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.google.gson.Gson;
import org.pyload.android.openapi.model.CaptchaTask;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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

	public static String getCaptchaHTML(CaptchaTask task) {
		Object data = task.getData();
		if (!(data instanceof Map)) return "";

		@SuppressWarnings("unchecked")
		Map<String, Object> captchaParams = (Map<String, Object>) data;

		String captchaType = String.valueOf(captchaParams.getOrDefault("captcha_plugin", ""));
		String siteKey = String.valueOf(captchaParams.getOrDefault("sitekey", ""));
		String captchaUrl = String.valueOf(captchaParams.getOrDefault("url", ""));
		URI uri = URI.create(captchaUrl);
		String baseUrl = uri.getScheme() + "://" + uri.getAuthority();

		String scriptSrc;
		String widgetHtml;

		switch (captchaType) {
			case "ReCaptcha":
				scriptSrc = "https://www.google.com/recaptcha/api.js?hl=en";
				widgetHtml = "<div class=\"g-recaptcha\" data-sitekey=\"" + siteKey + "\" data-callback=\"onSuccess\" data-size=\"compact\"></div>";
				break;
			case "Turnstile":
				scriptSrc = "https://challenges.cloudflare.com/turnstile/v0/api.js";
				widgetHtml = "<div class=\"cf-turnstile\" data-sitekey=\"" + siteKey + "\" data-callback=\"onSuccess\" data-language=\"en\"></div>";
				break;
			case "HCaptcha":
				scriptSrc = "https://js.hcaptcha.com/1/api.js?hl=en";
				widgetHtml = "<div class=\"h-captcha\" data-sitekey=\"" + siteKey + "\" data-callback=\"onSuccess\" data-size=\"compact\"></div>";
				break;
			default:
				scriptSrc = "";
				widgetHtml = "<p>Unsupported interactive captcha: " + captchaType + "</p>";
		}

		return "<!DOCTYPE html>\n" +
				"<html lang=\"en\">\n" +
				"<head>\n" +
				"    <meta charset=\"UTF-8\">\n" +
				"    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
				"    <base href=\"" + baseUrl + "\">\n" +
				"    <title>" + captchaType + "</title>\n\n" +
				"    <script src=\"" + scriptSrc + "\" async defer></script>\n" +
				"    <style>\n" +
				"        body { \n" +
				"            font-family: Arial, sans-serif; \n" +
				"            margin: 0; \n" +
				"            padding: 0; \n" +
				"            display: flex; \n" +
				"            flex-direction: column; \n" +
				"            align-items: center; \n" +
				"            justify-content: center; \n" +
				"            height: 100vh; \n" +
				"            width: 100vw;\n" +
				"        }\n" +
				"        .info {\n" +
				"            position: absolute;\n" +
				"            top: 60px;\n" +
				"            text-align: center;\n" +
				"            width: 100%;\n" +
				"            pointer-events: none;\n" +
				"            opacity: 0.3;\n" +
				"        }\n" +
				"    </style>\n" +
				"</head>\n" +
				"<body>\n" +
				"    " + widgetHtml + "\n" +
				"\n" +
				"    <script>\n" +
				"        (function() {\n" +
				"            const originalDomain = '" + baseUrl + "';\n" +
				"            try {\n" +
				"                Object.defineProperty(window, 'location', {\n" +
				"                    value: new URL(originalDomain),\n" +
				"                    writable: false\n" +
				"                });\n" +
				"                Object.defineProperty(document, 'referrer', { value: originalDomain });\n" +
				"            } catch (e) { console.error('Spoofing failed:', e); }\n" +
				"        })();\n" +
				"\n" +
				"        function onSuccess(token) {\n" +
				"            console.log('CAPTCHA_TOKEN:' + token);\n" +
				"            if (window.AndroidBridge && window.AndroidBridge.onTokenCaptured) {\n" +
				"                window.AndroidBridge.onTokenCaptured(token);\n" +
				"            }\n" +
				"        }\n" +
				"    </script>\n" +
				"</body>\n" +
				"</html>";
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
								String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8.name());
				String value = "";
				if (pair.length > 1) {
					value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8.name());
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
