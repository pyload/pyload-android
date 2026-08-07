package org.pyload.android.client;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.pyload.android.client.module.Utils;

import java.util.regex.Matcher;

public class AddLinksActivity extends AppCompatActivity {
	
	public static final int NEW_PACKAGE = 0;

	private String filename = "uploaded_from_android.dlc";
    private Uri selectedUri;

    private final ActivityResultLauncher<Intent> filePickLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    handleFilePickResult(result.getData());
                }
            }
    );

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        android.content.SharedPreferences prefs = newBase.getSharedPreferences(newBase.getPackageName() + "_preferences", android.content.Context.MODE_PRIVATE);
        String language = prefs.getString("language", "");
        if (!language.isEmpty()) {
            java.util.Locale locale = new java.util.Locale(language);
            android.content.res.Configuration config = new android.content.res.Configuration(newBase.getResources().getConfiguration());
            config.setLocale(locale);
            newBase = newBase.createConfigurationContext(config);
        }
        super.attachBaseContext(newBase);
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.add_links);
		
        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.add_links_container), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            setResult(RESULT_CANCELED);
            finish();
        }
        return true;
    }

	@Override
	protected void onStart() {
		super.onStart();
		
		Intent intent = getIntent();
        String packName = intent.getStringExtra("name");
        if (packName != null) {
            EditText nameView = (EditText) findViewById(R.id.new_packname);
            nameView.setText(packName);
        }
		String url = intent.getStringExtra("url");
		if (url != null){
			StringBuilder urls = new StringBuilder();
            Matcher m = Patterns.WEB_URL.matcher(url);
            while (m.find()) {
                urls.append(m.group() + "\n");
            }
            if(urls.length()>0) {
	            EditText view = (EditText) findViewById(R.id.links);
	            view.setText(urls.toString());
            }
		}
		String path = intent.getStringExtra("filepath");
		if (path != null){
			selectedUri = Uri.parse(path);
			filename = Utils.getFileName(this, selectedUri);
			TextView textView = (TextView) findViewById(R.id.filename);
			textView.setText(filename);
			textView.setTypeface(null, android.graphics.Typeface.BOLD);
		}
		
	}
	
	public void addPackage(View button){
		
		Intent data = new Intent();
				
		EditText view = (EditText) findViewById(R.id.new_packname);
				
		data.putExtra("name", view.getText().toString());
		
		view = (EditText) findViewById(R.id.links);
		data.putExtra("links", view.getText().toString());
		
		view = (EditText) findViewById(R.id.password);
		data.putExtra("password", view.getText().toString());
		
		TextView textView = (TextView) findViewById(R.id.filename);
		String path = textView.getText().toString().trim();

		if (selectedUri != null) {
			data.putExtra("filepath", selectedUri.toString());
			data.setData(selectedUri);
			data.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		} else {
			data.putExtra("filepath", path);
		}

		data.putExtra("filename", filename);
		
		Spinner spin = (Spinner) findViewById(R.id.destination);
		
		data.putExtra("dest", spin.getSelectedItemPosition());
			
		setResult(RESULT_OK, data);
		finish();
	}
	
	public void onCancel(View button){
		setResult(RESULT_CANCELED);
		finish();
	}

	public void pickFile(View button){
		browseForFile();
	}

	private void browseForFile() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("*/*"); // All file types

		filePickLauncher.launch(intent);
	}

    private void handleFilePickResult(Intent data) {
        Uri uri = data.getData();
        if (uri != null) {
            selectedUri = uri;
            filename = Utils.getFileName(this, uri);
			TextView textView = (TextView) findViewById(R.id.filename);
            textView.setText(filename);
            textView.setTypeface(null, android.graphics.Typeface.BOLD);
        }
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
	}
}


