package org.pyload.android.client.fragments;

import java.util.ArrayList;
import java.util.List;

import org.pyload.android.client.R;
import org.pyload.android.client.pyLoadApp;
import org.pyload.android.client.components.ExpandableListFragment;
import org.pyload.android.client.components.TabHandler;
import org.pyload.android.client.dialogs.FileInfoDialog;
import org.pyload.android.client.module.GuiTask;
import org.pyload.thrift.Destination;
import org.pyload.thrift.DownloadStatus;
import org.pyload.thrift.FileData;
import org.pyload.thrift.PackageData;
import org.pyload.thrift.Pyload.Client;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public abstract class AbstractPackageFragment extends ExpandableListFragment
		implements TabHandler {

	/**
	 * Destination, queue = 0, collector = 1, same as in pyLoad Core
	 */
	final static int FILEINFO_DIALOG = 0;

	protected int dest;
	private List<PackageData> data;
	private pyLoadApp app;
	private Client client;
	// tab position
	private int pos = -1;

	private final Runnable mUpdateResults = new Runnable() {

		public void run() {
			onDataReceived();
		}
	};

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Log.d("pyLoad", dest + " onAttach " + app);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("pyLoad", dest + " OnCreate " + app);
		app = (pyLoadApp) getActivity().getApplicationContext();
		data = new ArrayList<PackageData>();

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.d("pyLoad", dest + " onCreateView " + app);
		View v = inflater.inflate(R.layout.package_list, null, false);

		return v;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {

		Log.d("pyLoad", dest + " onViewCreated");

		registerForContextMenu(view.findViewById(android.R.id.list));
		PackageListAdapter adp = new PackageListAdapter(app, data,
				R.layout.package_item, R.layout.package_child_item);
		setListAdapter(adp);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Log.d("pyLoad", dest + " onActivityCreated " + app);

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d("pyLoad", dest + "onDestroy");
	}

	@Override
	public void onSelected() {
		Log.d("pyLoad", dest + " selected " + app);
		app = (pyLoadApp) getActivity().getApplicationContext();
		refresh();

	}

	@Override
	public void onDeselected() {
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int group,
			int child, long id) {

		PackageData pack;
		FileData file;
		try {
			pack = data.get(group);
			file = pack.links.get(child);
		} catch (Exception e) {
			return true;
		}

		FileInfoDialog dialog = FileInfoDialog.newInstance(pack, file);
		dialog.show(getFragmentManager(), FileInfoDialog.class.getName());
		return true;
	}

	@Override
	public void setPosition(int pos) {
		this.pos = pos;
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
		Toast.makeText(getActivity(), app.getString(R.string.success),
				Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.package_context_menu, menu);
		menu.setHeaderTitle(R.string.choose_action);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		Log.d("pyLoad", dest + " onContextItemSelected " + item);

		// filter event und allow to proceed
		if (!app.isCurrentTab(pos))
			return false;

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
				Toast.makeText(getActivity(), R.string.cant_move_files,
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
							newDest = Destination.Collector;
						} else {
							newDest = Destination.Queue;
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

	private List<PackageData> data;
	private pyLoadApp app;
	private final int groupRes;
	private final int childRes;
	private final LayoutInflater layoutInflater;

	public PackageListAdapter(pyLoadApp app, List<PackageData> data,
			int groupRes, int childRes) {

		this.app = app;
		this.data = data;
		this.groupRes = groupRes;
		this.childRes = childRes;

		layoutInflater = (LayoutInflater) app
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
			holder.progress = (ProgressBar) convertView
					.findViewById(R.id.package_progress);
			holder.size = (TextView) convertView.findViewById(R.id.size_stats);
			holder.links = (TextView) convertView.findViewById(R.id.link_stats);
			convertView.setTag(holder);
		}

		GroupViewHolder holder = (GroupViewHolder) convertView.getTag();
		holder.name.setText(pack.name);

		if (pack.linkstotal == 0)
			pack.linkstotal = 1;

		holder.progress.setProgress((int) ((pack.linksdone * 100) / pack.links
				.size()));
		holder.size.setText(app.formatSize(pack.sizedone) + " / "
				+ app.formatSize(pack.sizetotal));
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
		} else if (file.status == DownloadStatus.Skipped) {
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
