package org.pyload.android.client.fragments;

import android.content.Context;

public final class QueueFragment extends AbstractPackageFragment {

	@Override
	public void onAttach(Context context) {
		dest = 0;
		super.onAttach(context);
	}
}
