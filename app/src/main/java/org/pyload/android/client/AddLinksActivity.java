package org.pyload.android.client;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import org.pyload.android.client.module.FileChooser;

import java.util.regex.Matcher;

public class AddLinksActivity extends Activity {
	
	public static final int NEW_PACKAGE = 0;
	private static final int PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 1;

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
		boolean hasPermission = true;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			hasPermission = hasPermission();
			if (!hasPermission) {
				if (shouldShowRequestPermissionRationale()) {
					showPermissionRequestDialog(false);
				} else {
					requestPermission();
				}
			}
		}

		if (hasPermission) {
			browseForFile();
		}
	}

	private void browseForFile() {
		Intent intent = new Intent().setClass(this, FileChooser.class);
		startActivityForResult(intent, FileChooser.CHOOSE_FILE);
	}

	@RequiresApi(api = Build.VERSION_CODES.M)
	private boolean hasPermission() {
		return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
				== PackageManager.PERMISSION_GRANTED;
	}

	@RequiresApi(api = Build.VERSION_CODES.M)
	private boolean shouldShowRequestPermissionRationale() {
		return shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE);
	}

	@RequiresApi(api = Build.VERSION_CODES.M)
	private void requestPermission() {
			requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
					PERMISSIONS_REQUEST_EXTERNAL_STORAGE);
	}

	@RequiresApi(api = Build.VERSION_CODES.M)
	@Override
	public void onRequestPermissionsResult(int requestCode,
										   @NonNull String permissions[],
										   @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		switch (requestCode) {
			case PERMISSIONS_REQUEST_EXTERNAL_STORAGE: {
				// If request is cancelled, the result arrays are empty.
				boolean hasPermission = grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED;
				if (!hasPermission) {
					if (shouldShowRequestPermissionRationale()) {
						showPermissionRequestDialog(false);
					} else {
						showPermissionRequestDialog(true);
					}
				}
				else {
					browseForFile();
				}
			}
		}
	}

	private void showPermissionRequestDialog(final boolean gotoSettings) {
		new AlertDialog.Builder(this)
				.setTitle(R.string.permission_request)
				.setMessage(R.string.permission_explanation)
				.setNegativeButton(android.R.string.cancel, null)
				.setPositiveButton(gotoSettings ? R.string.go_to_settings : R.string.allow,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								if (gotoSettings) {
									startAppSettings();
								} else {
									requestPermission();
								}
							}
						})
				.show();
	}

	private void startAppSettings() {
		Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
		intent.setData(Uri.parse("package:" + getPackageName()));
		startActivity(intent);
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


