package org.pyload.android.client;

import org.pyload.android.client.module.FileChooser;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

public class AddLinksActivity extends Activity {
	
	public static final int NEW_PACKAGE = 0;
	
	private String filename = "uploaded_from_android.dlc";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.add_links);
		
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		Intent intent = getIntent();
		String path = intent.getStringExtra("dlcurl");
		if (path != null){
			EditText view = (EditText) findViewById(R.id.links);
			view.setText(path);
		}
		path = intent.getStringExtra("dlcpath");
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


