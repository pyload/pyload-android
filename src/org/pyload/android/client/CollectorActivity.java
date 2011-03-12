package org.pyload.android.client;

import android.os.Bundle;

public final class CollectorActivity extends PackageActivity {

	public void onCreate(Bundle savedInstanceState) {
		dest = 1;
		super.onCreate(savedInstanceState);		
		
		app.setCollector(this);
	}
}
