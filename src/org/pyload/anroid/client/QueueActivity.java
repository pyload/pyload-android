package org.pyload.anroid.client;

import android.os.Bundle;

public class QueueActivity extends PackageActivity {
	
	public void onCreate(Bundle savedInstanceState) {
		dest = 0;
		super.onCreate(savedInstanceState);
		
		app.setQueue(this);
	}
}
