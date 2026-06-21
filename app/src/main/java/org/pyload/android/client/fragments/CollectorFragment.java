package org.pyload.android.client.fragments;


import android.content.Context;

public final class CollectorFragment extends AbstractPackageFragment {

	@Override
	public void onAttach(Context context) {
		dest = 1;
		super.onAttach(context);
	}
}
