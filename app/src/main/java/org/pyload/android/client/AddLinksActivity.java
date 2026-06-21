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
        String name = intent.getStringExtra("name");
        if (name != null) {
            EditText nameView = (EditText) findViewById(R.id.new_packname);
            nameView.setText(name);
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
		String path = intent.getStringExtra("dlcpath");
		if (path != null){
			EditText view = (EditText) findViewById(R.id.filename);
			view.setText(path);
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
		
		view = (EditText) findViewById(R.id.filename);
		String path = view.getText().toString().trim();
		data.putExtra("filepath", path);
		data.putExtra("filename", filename);

		if (selectedUri != null && path.equals(selectedUri.toString())) {
			data.setData(selectedUri);
			data.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		}
		
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
			TextView view = (TextView) findViewById(R.id.filename);
            view.setText(filename);
            view.setTypeface(null, android.graphics.Typeface.BOLD);
        }
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
	}
}


