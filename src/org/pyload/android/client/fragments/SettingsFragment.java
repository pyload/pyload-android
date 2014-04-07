package org.pyload.android.client.fragments;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.pyload.android.client.R;
import org.pyload.android.client.pyLoadApp;
import org.pyload.android.client.module.GuiTask;
import org.pyload.android.client.module.SeparatedListAdapter;
import org.pyload.thrift.ConfigSection;
import org.pyload.thrift.Pyload.Client;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class SettingsFragment extends ListFragment {

	private pyLoadApp app;
	private SeparatedListAdapter adp;
	private SettingsAdapter general;
	private Map<String, ConfigSection> generalData;
	private SettingsAdapter plugins;
	private Map<String, ConfigSection> pluginData;

	private Runnable mUpdateResults = new Runnable() {

		@Override
		public void run() {
			general.setData(generalData);
			plugins.setData(pluginData);
			adp.notifyDataSetChanged();

			app.setProgress(false);
		}
	};

    private Runnable mRefresh = new Runnable() {
        @Override
        public void run() {
            SettingsFragment.this.update();
        }
    };

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.settings_list, null, false);
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		app = (pyLoadApp) getActivity().getApplicationContext();

		adp = new SeparatedListAdapter(app);

		general = new SettingsAdapter(app);
		plugins = new SettingsAdapter(app);

		adp.addSection(getString(R.string.general_config), general);
		adp.addSection(getString(R.string.plugin_config), plugins);

	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setListAdapter(adp);
	}

    public void onStart() {
        super.onStart();
        update();
    }

	private void update() {
		if (!app.hasConnection())
			return;

		app.setProgress(true);

		GuiTask task = new GuiTask(new Runnable() {

			public void run() {
				Client client = app.getClient();
				generalData = client.getConfig();
				pluginData = client.getPluginConfig();

			}
		}, mUpdateResults);

		app.addTask(task);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		Entry<String, ConfigSection> item = (Entry<String, ConfigSection>) adp
				.getItem(position);

		FragmentTransaction ft = getFragmentManager().beginTransaction();

		Bundle args = new Bundle();
		if (position > generalData.size())
			args.putString("type", "plugin");
		else
			args.putString("type", "core");
		args.putSerializable("section", item.getValue());

		Fragment f = new ConfigSectionFragment(mRefresh);
		f.setArguments(args);

		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

		ft.addToBackStack(null);

		ft.replace(R.id.layout_root, f);
		ft.commit();
	}

}

class SettingsAdapter extends BaseAdapter {

	static class ViewHolder {
		private TextView name;
		private TextView desc;
	}

	private LayoutInflater layoutInflater;
	private ArrayList<Entry<String, ConfigSection>> data;

	public SettingsAdapter(pyLoadApp app) {
		layoutInflater = (LayoutInflater) app
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		data = new ArrayList<Entry<String, ConfigSection>>();
	}

	public void setData(Map<String, ConfigSection> map) {
		this.data = new ArrayList<Entry<String, ConfigSection>>(map.entrySet());
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public Object getItem(int arg0) {
		return data.get(arg0);
	}

	@Override
	public long getItemId(int arg0) {
		return arg0;
	}

	@Override
	public View getView(int row, View convertView, ViewGroup viewGroup) {

		ViewHolder holder;

		if (convertView == null) {

			convertView = layoutInflater.inflate(R.layout.settings_item, null);

			holder = new ViewHolder();

			holder.name = (TextView) convertView.findViewById(R.id.section);
			holder.desc = (TextView) convertView
					.findViewById(R.id.section_desc);

			convertView.setTag(holder);

		}

		ConfigSection section = data.get(row).getValue();
		holder = (ViewHolder) convertView.getTag();

		holder.name.setText(section.description);

		if (section.outline != null) {
			holder.desc.setText(section.outline);
			holder.desc.setMaxHeight(100);
		} else {
			holder.desc.setMaxHeight(0);
		}

		return convertView;
	}

}
