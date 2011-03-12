package org.pyload.android.client;

import android.os.Bundle;

public final class QueueActivity extends PackageActivity {
	
	public void onCreate(Bundle savedInstanceState) {
		dest = 0;
		super.onCreate(savedInstanceState);
		app.setQueue(this);
	}
}
