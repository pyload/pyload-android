package org.pyload.android.client.dialogs;

import org.apache.commons.codec.binary.Base64;
import org.pyload.android.client.R;
import org.pyload.android.client.pyLoad;
import org.pyload.thrift.CaptchaTask;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class CaptchaDialog extends DialogFragment {

	private OnDismissListener listener;
	private CaptchaTask task;
	private TextView text;

	public static CaptchaDialog newInstance(CaptchaTask task) {
		CaptchaDialog dialog = new CaptchaDialog();
		Bundle args = new Bundle();
		args.putSerializable("task", task);
		dialog.setArguments(args);
		return dialog;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		task = (CaptchaTask) getArguments().getSerializable("task");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View dialog = inflater.inflate(R.layout.captcha_dialog, container,
				false);
		text = (TextView) dialog.findViewById(R.id.text);

		ImageView image = (ImageView) dialog.findViewById(R.id.image);

		byte[] decoded = Base64.decodeBase64(task.getData());

		Bitmap bm = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
		image.setImageBitmap(bm);

		Button enter = (Button) dialog.findViewById(R.id.enter);

		enter.setOnClickListener(new OnClickListener() {

			public void onClick(View arg0) {
				CaptchaDialog.this.onClick();
				dismiss();
			}
		});

		Button cancel = (Button) dialog.findViewById(R.id.cancel);

		cancel.setOnClickListener(new OnClickListener() {

			public void onClick(View arg0) {
				dismiss();
			}
		});

		return dialog;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);
		dialog.setTitle(getString(R.string.captcha_dialog_titel));
		return dialog;
	}

	public void onClick(){
		((pyLoad) getActivity()).setCaptchaResult(task.tid, text.getText().toString());
	}

	public void setOnDismissListener(OnDismissListener listener) {
		this.listener = listener;
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		if (listener != null) {
			listener.onDismiss(dialog);
			// clear reference
			listener = null;
		}
	}
}
