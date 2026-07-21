package org.pyload.android.client;

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
import android.widget.ProgressBar;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.pyload.android.client.module.GuiTask;
import org.pyload.android.client.module.Utils;
import org.pyload.android.openapi.api.PyLoadRestApi;
import org.pyload.android.openapi.model.CaptchaTask;

import java.net.URI;
import java.util.Locale;
import java.util.Map;

public class CaptchaActivity extends AppCompatActivity {

	private static boolean active = false;

	private pyLoadApp app;
	private CaptchaTask task;
	private EditText textView;
	private ImageView imageView;
	private WebView webView;
	private View captchaContent;
	private ProgressBar loading;

	private final Runnable runFetchTask = new Runnable() {
		public void run() {
			PyLoadRestApi client = app.getClient();
			if (app.executeNetworkCall(client.apiIsCaptchaWaitingGet())) {
				task = app.executeNetworkCall(client.apiGetCaptchaTaskGet(false));
			} else {
				task = null;
			}
		}
	};

	private final Runnable onTaskReceived = new Runnable() {
		public void run() {
			if (task != null) {
				initUI();
			} else {
				finish();
			}
		}
	};

	private final Runnable finishOnError = this::finish;

	public static boolean isActive() {
		return active;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		active = true;
		app = (pyLoadApp) getApplicationContext();
		setContentView(R.layout.activity_captcha);

		textView = findViewById(R.id.text);
		imageView = findViewById(R.id.image);
		webView = findViewById(R.id.web);
		captchaContent = findViewById(R.id.captcha_content);
		loading = findViewById(R.id.loading);

		Button enter = findViewById(R.id.enter);
		Button cancel = findViewById(R.id.cancel);

		enter.setOnClickListener(v -> {
			submitResult(textView.getText().toString());
		});

		cancel.setOnClickListener(v -> {
			finish();
		});

		fetchNextTask();
	}

	@Override
	protected void onNewIntent(@NonNull Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		fetchNextTask();
	}

	private void fetchNextTask() {
		captchaContent.setVisibility(View.GONE);
		loading.setVisibility(View.VISIBLE);
		GuiTask guiTask = new GuiTask(runFetchTask, onTaskReceived);
		guiTask.setCritical(finishOnError);
		app.addTask(guiTask);
	}

	private void initUI() {
		loading.setVisibility(View.GONE);
		captchaContent.setVisibility(View.VISIBLE);

		textView.setText("");
		
		String resultType = task.getResultType();
		if (resultType == null) resultType = "textual";

		if ("positional".equals(resultType)) {
			setTitle(R.string.captcha_positional_title);
		} else if ("interactive".equals(resultType)) {
			setTitle(R.string.captcha_interactive_title);
		} else {
			setTitle(R.string.captcha_textual_title);
		}

		if ("interactive".equals(resultType)) {
			imageView.setVisibility(View.GONE);
			textView.setVisibility(View.GONE);
			webView.setVisibility(View.VISIBLE);
			findViewById(R.id.enter).setVisibility(View.GONE);
			
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
			webView.setVisibility(View.GONE);
			imageView.setVisibility(View.VISIBLE);
			findViewById(R.id.enter).setVisibility(View.VISIBLE);
			
			if ("textual".equals(resultType)) {
				textView.setVisibility(View.VISIBLE);
				textView.setFocusableInTouchMode(true);
				textView.setHint(R.string.captcha_textual_title);
			} else {
				textView.setVisibility(View.VISIBLE);
				textView.setFocusable(false);
				textView.setHint(R.string.captcha_positional_title);
			}

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
							}
							return true;
						}
					});
				} else {
					imageView.setOnTouchListener(null);
				}
			} else {
				Log.e("pyLoad", "Captcha data is null or not a Map for " + resultType);
			}
		} else {
			Log.e("pyLoad", "Unsupported captcha type: " + resultType);
			textView.setVisibility(View.VISIBLE);
			textView.setHint("Unsupported captcha type: " + resultType);
			imageView.setVisibility(View.GONE);
			webView.setVisibility(View.GONE);
		}
	}

	private void submitResult(String result) {
		// Show loading state while waiting for confirmation
		captchaContent.setVisibility(View.GONE);
		loading.setVisibility(View.VISIBLE);

		GuiTask guiTask = new GuiTask(() -> {
			PyLoadRestApi client = app.getClient();
			app.executeNetworkCall(client.apiSetCaptchaResultPost(task.getTid(), result));
		}, () -> {
			app.onSuccess();
			// Fetch next captcha immediately
			fetchNextTask();
		});
		guiTask.setCritical(finishOnError);
		app.addTask(guiTask);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		active = false;
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
