package org.pyload.android.client;

import java.util.ArrayList;
import java.util.List;

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
	private int interval = 5;
	private boolean update = false;
	private boolean dialogOpen = false;

	private final Handler mHandler = new Handler();
	private final Runnable mUpdateResults = new Runnable() {
		@Override
		public void run() {
			onDataReceived();
		}
	};
	private final Runnable runUpdate = new Runnable() {
		
		@Override
		public void run() {
			client = app.getClient();
			downloads = client.statusDownloads();
			status = client.statusServer();
			if (client.isCaptchaWaiting()) {
				captcha = client.getCaptchaTask(false);
			}
		}
	};
	
	private final Runnable cancelUpdate = new Runnable() {
		
		@Override
		public void run() {
			stopUpdate();			
		}
	};

	private final Runnable mUpdateTimeTask = new Runnable() {
		public void run() {
			refresh();
			if(update)
				mHandler.postDelayed(this, interval * 1000);
		}
	};

	@Override
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

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.overview_context_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		int id = item.getGroupId();
		final DownloadInfo info = downloads.get(id);
		switch (item.getItemId()) {
		case R.id.abort:

			app.addTask(new GuiTask(new Runnable() {

				@Override
				public void run() {
					client = app.getClient();
					ArrayList<Integer> fids = new ArrayList<Integer>();
					fids.add(info.fid);
					client.stopDownloads(fids);
				}
			}, new Runnable() {
				@Override
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

		TextView view = (TextView) findViewById(R.id.status_server);
		view.setText(app.verboseBool(status.download));

		view = (TextView) findViewById(R.id.reconnect);
		view.setText(app.verboseBool(status.reconnect));

		view = (TextView) findViewById(R.id.speed);
		view.setText(app.formatSize(status.speed) + "/s");

		view = (TextView) findViewById(R.id.active);
		view.setText(String.format("%d / %d", status.active, status.total));

		if(captcha != null && app.prefs.getBoolean("pull_captcha", true)){
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

	@Override
	protected void onResume() {
		super.onResume();

		startUpdate();
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		app.clearTasks();
		stopUpdate();
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case CAPTCHA_DIALOG:
			
			if(dialogOpen) return null;
			
			final Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.captcha_dialog);
			dialog.setTitle(getString(R.string.captcha_dialog_titel));

			final TextView text = (TextView) dialog.findViewById(R.id.text);
			final int tid = captcha.tid;
			
			ImageView image = (ImageView) dialog.findViewById(R.id.image);
			
			Bitmap bm = BitmapFactory.decodeByteArray(captcha.data.array(), 0, captcha.data.array().length);			
			image.setImageBitmap(bm);
			
			Log.d("pyLoad", "Got Captcha Task"+ captcha.tid + "content length: "+ captcha.getData().length);
			
			Button enter = (Button) dialog.findViewById(R.id.enter);
			
			enter.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View arg0) {
					app.addTask(new GuiTask(new Runnable() {						
						@Override
						public void run() {
							String result = text.getText().toString();
							Client client = app.getClient();
							Log.d("pyLoad", "Send Captcha result: "+tid+" "+result);
							client.setCaptchaResult(tid, result);
							
						}
					}));
					dialog.dismiss();
				}
			});
			
			Button cancel = (Button) dialog.findViewById(R.id.cancel);
			
			cancel.setOnClickListener(new OnClickListener() {
				@Override
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

	@Override
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

	@Override
	public int getCount() {
		return downloads.size();
	}

	@Override
	public Object getItem(int id) {
		return downloads.get(id);
	}

	@Override
	public long getItemId(int pos) {
		return pos;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		DownloadInfo info = downloads.get(position);
		final View view = layoutInflater.inflate(rowResID, null);

		TextView text = (TextView) view.findViewById(R.id.name);
		text.setText(info.name);

		ProgressBar progress = (ProgressBar) view.findViewById(R.id.progress);
		progress.setProgress(info.percent);

		if (info.status == DownloadStatus.Downloading) {
			text = (TextView) view.findViewById(R.id.size);
			text.setText(app.formatSize(info.size));

			text = (TextView) view.findViewById(R.id.speed);
			text.setText(app.formatSize(info.speed) + "/s");
			
			text = (TextView) view.findViewById(R.id.size_done);
			text.setText(app.formatSize(info.size - info.bleft));

			text = (TextView) view.findViewById(R.id.eta);
			text.setText(info.format_eta);
			
			text = (TextView) view.findViewById(R.id.percent);
			text.setText(info.percent + "%");
			
		} else if (info.status == DownloadStatus.Waiting) {
			text = (TextView) view.findViewById(R.id.speed);
			text.setText(info.format_wait);

			text = (TextView) view.findViewById(R.id.percent);
			text.setText(info.statusmsg);
		} else {
			text = (TextView) view.findViewById(R.id.speed);
			text.setText(info.statusmsg);
		}

		return view;

	}

}
