package org.pyload.android.client;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.pyload.android.client.module.GuiTask;
import org.pyload.thrift.CaptchaTask;
import org.pyload.thrift.DownloadInfo;
import org.pyload.thrift.DownloadStatus;
import org.pyload.thrift.Pyload.Client;
import org.pyload.thrift.ServerStatus;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class OverviewActivity extends ListActivity implements OnDismissListener {

	public final static int CAPTCHA_DIALOG = 0;

	private pyLoadApp app;
	private Client client;

	private List<DownloadInfo> downloads;
	private ServerStatus status;
	private CaptchaTask captcha;
	private int lastCaptcha = -1;
	private int interval = 5;
	private boolean update = false;
	private boolean dialogOpen = false;
	
	/**
	 * GUI Elements
	 */
	
	private TextView statusServer;
	private TextView reconnect;
	private TextView speed;
	private TextView active;

	private final Handler mHandler = new Handler();
	private final Runnable mUpdateResults = new Runnable() {
		
		public void run() {
			onDataReceived();
		}
	};
	private final Runnable runUpdate = new Runnable() {

		
		public void run() {
			client = app.getClient();
			downloads = client.statusDownloads();
			status = client.statusServer();
			if (client.isCaptchaWaiting()) {
				Log.d("pyLoad", "Captcha available");
				captcha = client.getCaptchaTask(false);
				Log.d("pyload", captcha.resultType);
			}
		}
	};

	private final Runnable cancelUpdate = new Runnable() {

		
		public void run() {
			stopUpdate();
		}
	};

	private final Runnable mUpdateTimeTask = new Runnable() {
		public void run() {
			refresh();
			if (update)
				mHandler.postDelayed(this, interval * 1000);
		}
	};

	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.overview);	

	    
		app = (pyLoadApp) getApplicationContext();
		app.setOverview(this);

		downloads = new ArrayList<DownloadInfo>();
		OverviewAdapter adapter = new OverviewAdapter(app,
				R.layout.overview_item, downloads);
		setListAdapter(adapter);
		registerForContextMenu(getListView());

		statusServer = (TextView) findViewById(R.id.status_server);
		reconnect = (TextView) findViewById(R.id.reconnect);
		speed = (TextView) findViewById(R.id.speed);
		active = (TextView) findViewById(R.id.active);
		
	}

	
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.overview_context_menu, menu);
		menu.setHeaderTitle(R.string.choose_action);
	}

	
	public boolean onContextItemSelected(MenuItem item) {

		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item
				.getMenuInfo();
		final int id = menuInfo.position;
		final DownloadInfo info = downloads.get(id);
		switch (item.getItemId()) {
		case R.id.abort:

			app.addTask(new GuiTask(new Runnable() {

				
				public void run() {
					client = app.getClient();
					ArrayList<Integer> fids = new ArrayList<Integer>();
					fids.add(info.fid);
					client.stopDownloads(fids);
				}
			}, new Runnable() {
				
				public void run() {
					refresh();
				}
			}));
			return true;

		default:
			return super.onContextItemSelected(item);
		}

	}

	private void startUpdate() {
		interval = Integer.parseInt(app.prefs.getString("refresh_rate", "5"));
		update = true;
		mHandler.post(mUpdateTimeTask);
	}

	private void stopUpdate() {
		update = false;
		mHandler.removeCallbacks(mUpdateTimeTask);
	}

	/**
	 * Called when Status data received
	 */
	protected void onDataReceived() {
		OverviewAdapter adapter = (OverviewAdapter) getListAdapter();
		
		adapter.setDownloads(downloads);

		statusServer.setText(app.verboseBool(status.download));
		reconnect.setText(app.verboseBool(status.reconnect));
		speed.setText(app.formatSize(status.speed) + "/s");
		active.setText(String.format("%d / %d", status.active, status.total));

		if (captcha != null && app.prefs.getBoolean("pull_captcha", true) &&
				captcha.resultType.equals("textual") && lastCaptcha != captcha.tid) {
			showDialog(CAPTCHA_DIALOG);
		}

	}

	public void refresh() {

		if (!app.hasConnection())
			return;

		GuiTask task = new GuiTask(runUpdate, mUpdateResults);
		task.setCritical(cancelUpdate);

		app.addTask(task);
	}

	
	protected void onResume() {
		super.onResume();

		startUpdate();
	}

	
	protected void onPause() {
		super.onPause();

		app.clearTasks();
		stopUpdate();
	}

	
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case CAPTCHA_DIALOG:

			if (dialogOpen || captcha == null)
				return null;

			final Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.captcha_dialog);
			dialog.setTitle(getString(R.string.captcha_dialog_titel));

			final TextView text = (TextView) dialog.findViewById(R.id.text);

			final int tid = captcha.tid;
			lastCaptcha = tid;

			Log.d("pyLoad", "Got Captcha Task");

			Button enter = (Button) dialog.findViewById(R.id.enter);

			enter.setOnClickListener(new OnClickListener() {

				
				public void onClick(View arg0) {
					app.addTask(new GuiTask(new Runnable() {
						
						public void run() {
							String result = text.getText().toString();
							Client client = app.getClient();
							Log.d("pyLoad", "Send Captcha result: " + tid + " "
									+ result);
							client.setCaptchaResult(tid, result);

						}
					}));
					dialog.dismiss();
				}
			});

			Button cancel = (Button) dialog.findViewById(R.id.cancel);

			cancel.setOnClickListener(new OnClickListener() {
				
				public void onClick(View arg0) {
					dialog.dismiss();
				}
			});

			dialog.setOnDismissListener(this);

			dialogOpen = true;
			return dialog;

		default:
			return null;
		}

	}

	
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case CAPTCHA_DIALOG:
			ImageView image = (ImageView) dialog.findViewById(R.id.image);

			byte[] decoded = Base64.decodeBase64(captcha.getData());

			Bitmap bm = BitmapFactory.decodeByteArray(decoded, 0,
					decoded.length);
			image.setImageBitmap(bm);
			break;

		default:
			super.onPrepareDialog(id, dialog);
		}
	}

	
	public void onDismiss(DialogInterface arg0) {
		captcha = null;
		dialogOpen = false;
	}
}

/**
 * Renders the single ListView items
 * 
 * @author RaNaN
 * 
 */
class OverviewAdapter extends BaseAdapter {

	static class ViewHolder {
		private TextView name;
		private ProgressBar progress;
		private TextView size;
		private TextView percent;
		private TextView size_done;
		private TextView speed;
		private TextView eta;
	}

	private final pyLoadApp app;
	private List<DownloadInfo> downloads;
	private final int rowResID;
	private final LayoutInflater layoutInflater;

	public OverviewAdapter(final pyLoadApp app, final int rowResID,
			List<DownloadInfo> downloads) {
		this.app = app;
		this.rowResID = rowResID;
		this.downloads = downloads;

		layoutInflater = (LayoutInflater) app
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public void setDownloads(List<DownloadInfo> downloads) {
		this.downloads = downloads;
		notifyDataSetChanged();
	}

	
	public int getCount() {
		return downloads.size();
	}

	
	public Object getItem(int id) {
		return downloads.get(id);
	}

	
	public long getItemId(int pos) {
		return pos;
	}

	
	public View getView(int position, View convertView, ViewGroup parent) {
		DownloadInfo info = downloads.get(position);
		if (convertView == null) {
			convertView = layoutInflater.inflate(rowResID, null);
			ViewHolder holder = new ViewHolder();
			holder.name = (TextView) convertView.findViewById(R.id.name);
			holder.progress = (ProgressBar) convertView
					.findViewById(R.id.progress);
			holder.size = (TextView) convertView.findViewById(R.id.size);
			holder.speed = (TextView) convertView.findViewById(R.id.speed);
			holder.size_done = (TextView) convertView
					.findViewById(R.id.size_done);
			holder.eta = (TextView) convertView.findViewById(R.id.eta);
			holder.percent = (TextView) convertView.findViewById(R.id.percent);
			convertView.setTag(holder);
		}

		ViewHolder holder = (ViewHolder) convertView.getTag();

		if (!info.name.equals(holder.name.getText())) {
			holder.name.setText(info.name);
		}

		holder.progress.setProgress(info.percent);

		if (info.status == DownloadStatus.Downloading) {
			holder.size.setText(app.formatSize(info.size));
			holder.percent.setText(info.percent + "%");
			holder.size_done.setText(app.formatSize(info.size - info.bleft));

			holder.speed.setText(app.formatSize(info.speed) + "/s");
			holder.eta.setText(info.format_eta);

		} else if (info.status == DownloadStatus.Waiting) {
			holder.size.setText(R.string.lambda);
			holder.percent.setText(R.string.lambda);
			holder.size_done.setText(R.string.lambda);

			holder.speed.setText(info.statusmsg);
			holder.eta.setText(info.format_wait);

		} else {
			holder.size.setText(R.string.lambda);
			holder.percent.setText(R.string.lambda);
			holder.size_done.setText(R.string.lambda);

			holder.speed.setText(info.statusmsg);
			holder.eta.setText(R.string.lambda);
		}

		return convertView;

	}

	
	public boolean hasStableIds() {
		return false;
	}

}
