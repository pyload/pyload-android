package org.pyload.android.client.fragments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;

import org.pyload.android.client.R;
import org.pyload.android.client.module.Utils;
import org.pyload.android.client.pyLoadApp;
import org.pyload.android.client.module.GuiTask;
import org.pyload.android.client.module.SeparatedListAdapter;
import org.pyload.android.openapi.api.PyLoadRestApi;
import org.pyload.android.openapi.model.ConfigSection;

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.fragment.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class SettingsFragment extends ListFragment implements ConfigSectionFragment.OnSettingsSavedListener {

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

    @Override
    public void onSettingsSaved() {
        update();
    }

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.settings_list, container, false);
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		app = (pyLoadApp) getActivity().getApplicationContext();

		adp = new SeparatedListAdapter(getActivity());

		general = new SettingsAdapter(getActivity());
		plugins = new SettingsAdapter(getActivity());

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
				PyLoadRestApi client = app.getClient();
				generalData = app.executeNetworkCall(client.apiGetConfigGet());
				pluginData = app.executeNetworkCall(client.apiGetPluginConfigGet());
			}
		}, mUpdateResults);

		app.addTask(task);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		Object itemObj = adp.getItem(position);
		if (!(itemObj instanceof Entry)) {
			return;
		}

		Entry<String, ConfigSection> item = (Entry<String, ConfigSection>) itemObj;

		Bundle args = new Bundle();
		// Calculate type correctly based on section headers
		int generalCount = general.getCount();
		if (position > generalCount + 1)
			args.putString("type", "plugin");
		else
			args.putString("type", "core");

		args.putString("section", Utils.encodeObject(item.getValue()));

		Fragment f = new ConfigSectionFragment();
		f.setArguments(args);

		new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            if (!isAdded()) return;
            
			FragmentTransaction ft = requireActivity().getSupportFragmentManager().beginTransaction();
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
			ft.addToBackStack(null);
			ft.replace(R.id.serverSettings, f);
			ft.commitAllowingStateLoss();
		});
	}

}

class SettingsAdapter extends BaseAdapter {

	static class ViewHolder {
		private TextView name;
		private TextView desc;
	}

	private LayoutInflater layoutInflater;
	private ArrayList<Entry<String, ConfigSection>> data;

	public SettingsAdapter(Context context) {
		layoutInflater = LayoutInflater.from(context);

		data = new ArrayList<Entry<String, ConfigSection>>();
	}

  class SettingsComparator
    implements Comparator<Entry<String,ConfigSection>> {

    @Override
    public int compare(Entry<String, ConfigSection> lhs, Entry<String, ConfigSection> rhs) {
      return lhs.getKey().compareTo(rhs.getKey());
    }
  }

	public void setData(Map<String, ConfigSection> map) {
		this.data = new ArrayList<Entry<String, ConfigSection>>(map.entrySet());
		Collections.sort(data, new SettingsComparator());
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
		android.util.Log.d("pyLoad", "SettingsAdapter.getView(" + row + ")");

		if (convertView == null) {

			convertView = layoutInflater.inflate(R.layout.settings_item, viewGroup, false);

			holder = new ViewHolder();

			holder.name = (TextView) convertView.findViewById(R.id.section);
			holder.desc = (TextView) convertView
					.findViewById(R.id.section_desc);

			convertView.setTag(holder);

		}

		ConfigSection section = data.get(row).getValue();
		holder = (ViewHolder) convertView.getTag();

		holder.name.setText(section.getDescription());

		if (section.getOutline() != null) {
			holder.desc.setText(section.getOutline());
			holder.desc.setMaxHeight(100);
		} else {
			holder.desc.setMaxHeight(0);
		}

		return convertView;
	}

}
