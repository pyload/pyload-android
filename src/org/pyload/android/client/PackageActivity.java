package org.pyload.android.client;

import java.util.ArrayList;
import java.util.List;

import org.pyload.android.client.components.FixedExpandableListActivity;
import org.pyload.android.client.module.GuiTask;
import org.pyload.thrift.Destination;
import org.pyload.thrift.DownloadStatus;
import org.pyload.thrift.FileData;
import org.pyload.thrift.PackageData;
import org.pyload.thrift.Pyload.Client;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public abstract class PackageActivity extends FixedExpandableListActivity {

	/**
	 * Destination, queue = 0, collector = 1, same as in pyLoad Core
	 */
	final static int FILEINFO_DIALOG = 0;

	// saved for dialog
	private int group = 0;
	private int child = 0;

	protected int dest;
	private List<PackageData> data;
	protected pyLoadApp app;
	private Client client;

	private final Runnable mUpdateResults = new Runnable() {

		
		public void run() {
			onDataReceived();
		}
	};

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.package_list);
		app = (pyLoadApp) getApplicationContext();

		data = new ArrayList<PackageData>();

		PackageListAdapter adapter = new PackageListAdapter(this, data,
				R.layout.package_item, R.layout.package_child_item);

		setListAdapter(adapter);
		registerForContextMenu(getExpandableListView());
	}

	
	protected void onPause() {
		super.onPause();
		app.clearTasks();
	}

	
	protected void onResume() {
		super.onResume();
		refresh();
	}

	
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {

		group = groupPosition;
		child = childPosition;

		showDialog(FILEINFO_DIALOG);
		return true;
	}

	
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case FILEINFO_DIALOG:

			try {
				// just to be sure, check if clicked file exists
				data.get(group).links.get(child);
			} catch (IndexOutOfBoundsException e) {
				Log.d("pyLoad", "Error when creating dialog", e);
				return null;
			}

			final Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.fileinfo_dialog);
			dialog.setTitle(R.string.fileinfo_title);

			Button button = (Button) dialog.findViewById(R.id.close);
			button.setOnClickListener(new OnClickListener() {
				
				public void onClick(View arg0) {
					dialog.dismiss();
				}
			});

			return dialog;

		default:
			return super.onCreateDialog(id);
		}
	}

	
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case FILEINFO_DIALOG:

			FileData file;

			try {
				file = data.get(group).links.get(child);
			} catch (IndexOutOfBoundsException e) {
				Log.d("pyLoad", "Error when preparing dialog", e);
				return;
			}

			TextView view = (TextView) dialog.findViewById(R.id.name);
			view.setText(file.name);

			view = (TextView) dialog.findViewById(R.id.status);
			view.setText(file.statusmsg);

			view = (TextView) dialog.findViewById(R.id.plugin);
			view.setText(file.plugin);

			view = (TextView) dialog.findViewById(R.id.size);
			view.setText(file.format_size);

			view = (TextView) dialog.findViewById(R.id.error);
			view.setText(file.error);

			PackageData pack = null;
			for (PackageData comparePack : data)
				if (comparePack.pid == file.packageID)
					pack = comparePack;

			if (pack == null)
				return;

			view = (TextView) dialog.findViewById(R.id.packageValue);
			view.setText(pack.name);

			view = (TextView) dialog.findViewById(R.id.folder);
			view.setText(pack.folder);

			break;

		default:
			super.onPrepareDialog(id, dialog);
		}
	}

	public void refresh() {

		if (!app.hasConnection())
			return;
		
		app.setProgress(true);

		GuiTask task = new GuiTask(new Runnable() {

			
			public void run() {
				client = app.getClient();
				if (dest == 0)
					data = client.getQueueData();
				else
					data = client.getCollectorData();
			}
		}, mUpdateResults);

		app.addTask(task);
	}

	protected void onDataReceived() {
		app.setProgress(false);
		PackageListAdapter adapter = (PackageListAdapter) getExpandableListAdapter();
		adapter.setData(data);
	}

	protected void onTaskPerformed() {
		refresh();
		Toast.makeText(this, app.getString(R.string.success),
				Toast.LENGTH_SHORT).show();
	}

	
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.package_context_menu, menu);
		menu.setHeaderTitle(R.string.choose_action);
	}

	
	public boolean onContextItemSelected(MenuItem item) {

		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item
				.getMenuInfo();

		int type = ExpandableListView
				.getPackedPositionType(info.packedPosition);
		if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			int groupPos = ExpandableListView
					.getPackedPositionGroup(info.packedPosition);
			int childPos = ExpandableListView
					.getPackedPositionChild(info.packedPosition);

			final FileData file = data.get(groupPos).links.get(childPos);

			switch (item.getItemId()) {
			case R.id.restart:

				app.addTask(new GuiTask(new Runnable() {
					
					public void run() {
						client = app.getClient();
						client.restartFile(file.fid);
					}
				}, app.handleSuccess));

				break;
			case R.id.delete:

				app.addTask(new GuiTask(new Runnable() {
					
					public void run() {
						client = app.getClient();
						ArrayList<Integer> fids = new ArrayList<Integer>();
						fids.add(file.fid);

						client.deleteFiles(fids);
					}
				}, app.handleSuccess));

				break;

			case R.id.move:
				Toast.makeText(this, R.string.cant_move_files,
						Toast.LENGTH_SHORT).show();
				break;

			default:
				break;
			}

			return true;
		} else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			int groupPos = ExpandableListView
					.getPackedPositionGroup(info.packedPosition);

			final PackageData pack = data.get(groupPos);

			switch (item.getItemId()) {
			case R.id.restart:

				app.addTask(new GuiTask(new Runnable() {
					
					public void run() {
						client = app.getClient();
						client.restartPackage(pack.pid);
					}
				}, app.handleSuccess));

				break;
			case R.id.delete:

				app.addTask(new GuiTask(new Runnable() {
					
					public void run() {
						client = app.getClient();
						ArrayList<Integer> pids = new ArrayList<Integer>();
						pids.add(pack.pid);
						client.deletePackages(pids);
					}
				}, app.handleSuccess));

				break;

			case R.id.move:

				app.addTask(new GuiTask(new Runnable() {
					
					public void run() {
						client = app.getClient();
						Destination newDest;
						if (dest == 0) {
							newDest = Destination.Queue;
						} else {
							newDest = Destination.Collector;
						}

						client.movePackage(newDest, pack.pid);
					}
				}, app.handleSuccess));

				break;

			default:
				break;
			}

			return true;
		}

		return false;

	}
}

class PackageListAdapter extends BaseExpandableListAdapter {

	static class GroupViewHolder {
		private TextView name;
		private ProgressBar progress;
		private TextView size;
		private TextView links;
	}

	static class ChildViewHolder {
		private TextView name;
		private TextView status;
		private TextView size;
		private TextView plugin;
		private ImageView status_icon;
	}

	private final Context context;
	private List<PackageData> data;
	private pyLoadApp app;
	private final int groupRes;
	private final int childRes;
	private final LayoutInflater layoutInflater;

	public PackageListAdapter(Context contex, List<PackageData> data,
			int groupRes, int childRes) {

		this.context = contex;
		this.data = data;
		this.groupRes = groupRes;
		this.childRes = childRes;

		app = (pyLoadApp) contex.getApplicationContext();
		layoutInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public void setData(List<PackageData> data) {
		this.data = data;
		notifyDataSetChanged();
	}

	
	public Object getChild(int group, int child) {
		return data.get(group).links.get(child);
	}

	
	public long getChildId(int group, int child) {
		return child;
	}

	
	public int getChildrenCount(int group) {
		return data.get(group).links.size();
	}

	
	public Object getGroup(int group) {
		return data.get(group);
	}

	
	public int getGroupCount() {
		return data.size();
	}

	
	public long getGroupId(int group) {
		return group;
	}

	
	public View getGroupView(int group, boolean isExpanded, View convertView,
			ViewGroup parent) {

		PackageData pack = data.get(group);
		if (convertView == null) {
			convertView = layoutInflater.inflate(groupRes, null);
			GroupViewHolder holder = new GroupViewHolder();
			holder.name = (TextView) convertView.findViewById(R.id.name);
			holder.progress = (ProgressBar) convertView.findViewById(R.id.package_progress);
			holder.size = (TextView) convertView.findViewById(R.id.size_stats);
			holder.links = (TextView) convertView.findViewById(R.id.link_stats);
			convertView.setTag(holder);
		}

		GroupViewHolder holder = (GroupViewHolder) convertView.getTag();
		holder.name.setText(pack.name);
		
		if (pack.linkstotal == 0)
			pack.linkstotal = 1;
		
		holder.progress.setProgress((int) ((pack.linksdone * 100) / pack.links.size()));
		holder.size.setText(app.formatSize(pack.sizedone) + " / " + app.formatSize(pack.sizetotal));
		holder.links.setText(pack.linksdone + " / " + pack.links.size());

		return convertView;
	}

	
	public View getChildView(int group, int child, boolean isLastChild,
			View convertView, ViewGroup parent) {

		FileData file = data.get(group).links.get(child);

		if (file == null)
			return null;

		if (convertView == null) {
			convertView = layoutInflater.inflate(childRes, null);
			ChildViewHolder holder = new ChildViewHolder();
			holder.name = (TextView) convertView.findViewById(R.id.name);
			holder.status = (TextView) convertView.findViewById(R.id.status);
			holder.size = (TextView) convertView.findViewById(R.id.size);
			holder.plugin = (TextView) convertView.findViewById(R.id.plugin);
			holder.status_icon = (ImageView) convertView
					.findViewById(R.id.status_icon);
			convertView.setTag(holder);
		}

		ChildViewHolder holder = (ChildViewHolder) convertView.getTag();

		if (!file.name.equals(holder.name.getText()))
			holder.name.setText(file.name);

		holder.status.setText(file.statusmsg);
		holder.size.setText(app.formatSize(file.size));
		holder.plugin.setText(file.plugin);

		if (file.status == DownloadStatus.Failed
				|| file.status == DownloadStatus.Aborted
				|| file.status == DownloadStatus.Offline) {
			holder.status_icon.setImageResource(R.drawable.stop);
		} else if (file.status == DownloadStatus.Finished) {
			holder.status_icon.setImageResource(R.drawable.tick);
		} else if (file.status == DownloadStatus.Waiting) {
			holder.status_icon.setImageResource(R.drawable.menu_clock);
		} else if(file.status == DownloadStatus.Skipped){
			holder.status_icon.setImageResource(R.drawable.tag);
		} else {
			holder.status_icon.setImageResource(0);
		}

		return convertView;
	}

	
	public boolean hasStableIds() {
		return false;
	}

	
	public boolean isChildSelectable(int group, int child) {
		return true;
	}
}
