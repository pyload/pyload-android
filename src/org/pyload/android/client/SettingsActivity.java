package org.pyload.android.client;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.pyload.android.client.module.GuiTask;
import org.pyload.android.client.module.SeparatedListAdapter;
import org.pyload.thrift.ConfigSection;
import org.pyload.thrift.Pyload.Client;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class SettingsActivity extends ListActivity {

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

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		app = (pyLoadApp) getApplicationContext();

		adp = new SeparatedListAdapter(app);

		general = new SettingsAdapter(app);
		plugins = new SettingsAdapter(app);

		adp.addSection(getString(R.string.general_config), general);
		adp.addSection(getString(R.string.plugin_config), plugins);

	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		setListAdapter(adp);

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
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
			
		Entry<String, ConfigSection> item = (Entry<String, ConfigSection>) adp.getItem(position);
		
		Intent intent = new Intent(app, ConfigSectionActivity.class);
		intent.putExtra("type", "general");
		intent.putExtra("section", item.getValue());
		startActivity(intent);
		
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
			holder.desc = (TextView) convertView.findViewById(R.id.section_desc);
		
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
