package org.pyload.anroid.client;

import android.os.Bundle;

public class CollectorActivity extends PackageActivity {

	public void onCreate(Bundle savedInstanceState) {
		dest = 1;
		super.onCreate(savedInstanceState);		
		
		app.setCollector(this);
	}
}
