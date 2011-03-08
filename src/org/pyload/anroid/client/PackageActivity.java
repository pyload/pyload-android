package org.pyload.anroid.client;

import java.util.ArrayList;
import java.util.List;

import org.pyload.thrift.Destination;
import org.pyload.thrift.FileData;
import org.pyload.thrift.PackageData;
import org.pyload.thrift.Pyload.Client;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.os.Bundle;
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
import android.widget.TextView;
import android.widget.Toast;

public abstract class PackageActivity extends ExpandableListActivity {

	/**
	 * Destination, queue = 0, collector = 1, same as in pyLoad Core
	 */
	protected int dest;
	private List<PackageData> data;
	protected pyLoadApp app;
	private Client client;

	private final Runnable mUpdateResults = new Runnable() {

		@Override
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

	@Override
	protected void onPause() {
		super.onPause();
		app.clearTasks();
	}

	@Override
	protected void onResume() {
		super.onResume();
		refresh();
	}

	public void refresh() {

		if (!app.hasConnection())
			return;

		GuiTask task = new GuiTask(new Runnable() {

			@Override
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
		PackageListAdapter adapter = (PackageListAdapter) getExpandableListAdapter();
		adapter.setData(data);
	}

	protected void onTaskPerformed() {
		refresh();
		Toast.makeText(this, app.getString(R.string.success),
				Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.package_context_menu, menu);
	}

	@Override
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
					@Override
					public void run() {
						client = app.getClient();
						client.restartFile(file.fid);
					}
				}, app.handleSuccess));

				break;
			case R.id.delete:

				app.addTask(new GuiTask(new Runnable() {
					@Override
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
						Toast.LENGTH_SHORT);
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
					@Override
					public void run() {
						client = app.getClient();
						client.restartPackage(pack.pid);
					}
				}, app.handleSuccess));
				
				
				break;
			case R.id.delete:
				
				app.addTask(new GuiTask(new Runnable() {
					@Override
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
					@Override
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

	@Override
	public Object getChild(int group, int child) {
		return data.get(group).links.get(child);
	}

	@Override
	public long getChildId(int group, int child) {
		return child;
	}

	@Override
	public int getChildrenCount(int group) {
		return data.get(group).links.size();
	}

	@Override
	public Object getGroup(int group) {
		return data.get(group);
	}

	@Override
	public int getGroupCount() {
		return data.size();
	}

	@Override
	public long getGroupId(int group) {
		return group;
	}

	@Override
	public View getGroupView(int group, boolean isExpanded, View convertView,
			ViewGroup parent) {

		PackageData pack = data.get(group);
		final View view = layoutInflater.inflate(groupRes, null);

		TextView name = (TextView) view.findViewById(R.id.name);
		name.setText(pack.name);

		return view;
	}

	@Override
	public View getChildView(int group, int child, boolean isLastChild,
			View convertView, ViewGroup parent) {

		FileData file = data.get(group).links.get(child);
		final View view = layoutInflater.inflate(childRes, null);

		TextView text = (TextView) view.findViewById(R.id.name);
		text.setText(file.name);

		text = (TextView) view.findViewById(R.id.status);
		text.setText(file.statusmsg);

		text = (TextView) view.findViewById(R.id.size);
		text.setText(app.formatSize(file.size));

		text = (TextView) view.findViewById(R.id.plugin);
		text.setText(file.plugin);

		return view;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isChildSelectable(int group, int child) {
		return true;
	}
}
