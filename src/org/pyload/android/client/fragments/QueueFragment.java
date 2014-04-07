package org.pyload.android.client.fragments;

import android.app.Activity;

public final class QueueFragment extends AbstractPackageFragment {

	@Override
	public void onAttach(Activity activity) {
		dest = 0;
		super.onAttach(activity);
	}
}
