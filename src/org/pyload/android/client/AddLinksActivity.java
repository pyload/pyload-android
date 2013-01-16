package org.pyload.android.client;

import android.util.Patterns;
import android.view.MenuItem;
import org.pyload.android.client.module.FileChooser;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.regex.Matcher;

public class AddLinksActivity extends Activity {
	
	public static final int NEW_PACKAGE = 0;
	
	private String filename = "uploaded_from_android.dlc";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.add_links);
		
        if (pyLoadApp.isActionBarAvailable()) {
            getActionBar().setHomeButtonEnabled(true);
            getActionBar().setDisplayHomeAsUpEnabled(true);
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
            Matcher m = Patterns.WEB_URL.matcher(url);
            if (m.find()) {
                url = m.group();
                EditText view = (EditText) findViewById(R.id.links);
                view.setText(url);
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
		data.putExtra("filepath", view.getText().toString().trim());
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
		Intent intent = new Intent().setClass(this, FileChooser.class);
		startActivityForResult(intent, FileChooser.CHOOSE_FILE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case FileChooser.CHOOSE_FILE:
			switch (resultCode) {
			case RESULT_OK:
				
				String path = data.getStringExtra("filepath");
				filename = data.getStringExtra("filename");
				EditText view = (EditText) findViewById(R.id.filename);
				view.setText(path);
				
				break;

			default:
				break;
			}
			break;
		default:
			super.onActivityResult(requestCode, resultCode, data);
		}
	}
}


