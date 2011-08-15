package org.pyload.android.client.fragments;


import android.app.Activity;

public final class CollectorFragment extends AbstractPackageFragment {

	@Override
	public void onAttach(Activity activity) {
		dest = 1;
		super.onAttach(activity);
	}
}
