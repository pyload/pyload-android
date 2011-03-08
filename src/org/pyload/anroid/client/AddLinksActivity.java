package org.pyload.anroid.client;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

public class AddLinksActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.add_links);
		
	}
	
	public void addPackage(View button){
		
		Intent data = new Intent();
				
		EditText view = (EditText) findViewById(R.id.new_packname);
				
		data.putExtra("name", view.getText().toString());
		
		view = (EditText) findViewById(R.id.links);
		data.putExtra("links", view.getText().toString());
		
		view = (EditText) findViewById(R.id.password);
		data.putExtra("password", view.getText().toString());
		
		Spinner spin = (Spinner) findViewById(R.id.destination);
		
		data.putExtra("dest", spin.getSelectedItemPosition());
			
		setResult(RESULT_OK, data);
		finish();
	}
	
	public void onCancel(View button){
		setResult(RESULT_CANCELED);
		finish();
	}

}
