package org.pyload.android.client.dialogs;

import org.pyload.android.client.R;
import org.pyload.android.client.module.Utils;
import org.pyload.android.openapi.model.FileData;
import org.pyload.android.openapi.model.PackageData;

import android.app.Dialog;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class FileInfoDialog extends DialogFragment {

	private PackageData pack = null;
	private FileData file = null;
	
	public static FileInfoDialog newInstance(PackageData pack, FileData file){
		
		FileInfoDialog dialog = new FileInfoDialog();
		Bundle args = new Bundle();

		args.putString("pack", Utils.encodeObject(pack));
		args.putString("file", Utils.encodeObject(file));
		
		dialog.setArguments(args);
		return dialog;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		pack = Utils.decodeObject(getArguments().getString("pack"), PackageData.class);
		file = Utils.decodeObject(getArguments().getString("file"), FileData.class);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View dialog = inflater.inflate(R.layout.fileinfo_dialog, container, false);
		
		TextView view = (TextView) dialog.findViewById(R.id.name);
		view.setText(file.getName());

		view = (TextView) dialog.findViewById(R.id.status);
		view.setText(file.getStatusmsg());

		view = (TextView) dialog.findViewById(R.id.plugin);
		view.setText(file.getPlugin());

		view = (TextView) dialog.findViewById(R.id.size);
		view.setText(file.getFormatSize());

		view = (TextView) dialog.findViewById(R.id.error);
		view.setText(file.getError());

		view = (TextView) dialog.findViewById(R.id.packageValue);
		view.setText(pack.getName());

		view = (TextView) dialog.findViewById(R.id.folder);
		view.setText(pack.getFolder());
		
		Button button = (Button) dialog.findViewById(R.id.close);
		button.setOnClickListener(new OnClickListener() {

			public void onClick(View arg0) {
				dismiss();
			}
		});
				
		return dialog;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);
		dialog.setTitle(R.string.fileinfo_title);
		return dialog;
	}
}
