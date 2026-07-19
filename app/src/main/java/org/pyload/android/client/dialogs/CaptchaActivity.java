package org.pyload.android.client.dialogs;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.Keep;
import androidx.appcompat.app.AppCompatActivity;

import org.pyload.android.client.R;
import org.pyload.android.client.module.Utils;
import org.pyload.android.client.pyLoad;
import org.pyload.android.openapi.model.CaptchaTask;

import java.net.URI;
import java.util.Locale;
import java.util.Map;

public class CaptchaActivity extends AppCompatActivity {

	private CaptchaTask task;
	private EditText textView;
	private ImageView imageView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.captcha_dialog);

		String taskJson = getIntent().getStringExtra("task");
		if (taskJson == null) {
			finish();
			return;
		}
		task = Utils.decodeObject(taskJson, CaptchaTask.class);

		textView = findViewById(R.id.text);
		imageView = findViewById(R.id.image);
		WebView webView = findViewById(R.id.web);

		String resultType = task.getResultType();
		if (resultType == null) resultType = "textual";

		if ("positional".equals(resultType)) {
			setTitle(R.string.captcha_positional_title);
		} else {
			setTitle(R.string.captcha_textual_title);
		}

		Button enter = findViewById(R.id.enter);
		Button cancel = findViewById(R.id.cancel);

		if ("interactive".equals(resultType)) {
			imageView.setVisibility(View.GONE);
			textView.setVisibility(View.GONE);
			webView.setVisibility(View.VISIBLE);
			enter.setVisibility(View.GONE);
			WebSettings webSettings = webView.getSettings();
			webSettings.setJavaScriptEnabled(true);
			webSettings.setDomStorageEnabled(true);
			webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
			webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36");

			// Use JavaScript Interface for direct communication
			webView.addJavascriptInterface(new CaptchaInterface(), "AndroidBridge");

			webView.setWebChromeClient(new WebChromeClient());

			Object data = task.getData();
			if (data instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> captchaParams = (Map<String, Object>) data;
				String captchaUrl = String.valueOf(captchaParams.getOrDefault("url", ""));
				if (captchaUrl.startsWith("http")) {
					URI uri = URI.create(captchaUrl);
					String baseUrl = uri.getScheme() + "://" + uri.getAuthority();
					webView.loadDataWithBaseURL(baseUrl, Utils.getCaptchaHTML(task), "text/html", "UTF-8", null);
				}
			}
		} else if ("textual".equals(resultType) || "positional".equals(resultType)) {
			Object data = task.getData();
			if (data instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> captchaParams = (Map<String, Object>) data;
				String src = String.valueOf(captchaParams.getOrDefault("src", ""));
				if (src.contains("base64,")) {
					src = src.split("base64,")[1];
				}

				byte[] decoded = Base64.decode(src, Base64.DEFAULT);
				Bitmap bm = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
				if (bm != null) {
					imageView.setImageBitmap(bm);
				} else {
					Log.e("pyLoad", "Failed to decode captcha bitmap");
				}

				if ("positional".equals(resultType)) {
					textView.setVisibility(View.GONE);
					imageView.setOnTouchListener(new View.OnTouchListener() {
						@Override
						public boolean onTouch(View v, MotionEvent event) {
							if (event.getAction() == MotionEvent.ACTION_UP) {
								float[] coords = new float[]{event.getX(), event.getY()};
								Matrix matrix = new Matrix();
								imageView.getImageMatrix().invert(matrix);
								matrix.mapPoints(coords);
								int x = (int) coords[0];
								int y = (int) coords[1];
								textView.setText(String.format(Locale.US, "%d,%d", x, y));
								Log.d("pyLoad", "Positional captcha: " + x + "," + y);
							}
							return true;
						}
					});
				}
			} else {
				Log.e("pyLoad", "Captcha data is null or not a Map for " + resultType);
			}
		} else {
			Log.e("pyLoad", "Unsupported captcha type: " + resultType);
			textView.setHint("Unsupported captcha type: " + resultType);
			imageView.setVisibility(View.GONE);
			enter.setVisibility(View.GONE);
		}

		enter.setOnClickListener(v -> {
			submitResult(textView.getText().toString());
		});

		cancel.setOnClickListener(v -> {
			finish();
		});
	}

	private void submitResult(String result) {
		Intent intent = new Intent(this, pyLoad.class);
		intent.setAction("SET_CAPTCHA_RESULT");
		intent.putExtra("tid", task.getTid());
		intent.putExtra("result", result);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		startActivity(intent);
		finish();
	}

	// Native interface for JavaScript to call
	private class CaptchaInterface {
		@Keep
		@JavascriptInterface
		@SuppressWarnings("unused")
		public void onTokenCaptured(String token) {
			runOnUiThread(() -> submitResult(token));
		}
	}
}
