package org.pyload.android.client;

import java.util.List;
import java.util.Map;

import org.pyload.thrift.ConfigItem;
import org.pyload.thrift.ConfigSection;
import org.pyload.thrift.Pyload.Client;
import org.pyload.android.client.pyLoadApp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.CheckBox;

public class ServerSettings extends Activity {

	pyLoadApp app = (pyLoadApp) super.getApplication();
	Client client = app.getClient();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.server_settings);
		
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (client == null) return; //TODO Maybe better to warn user that there is no good login to display server settings

		Map<String, ConfigSection> cfgFull = client.getConfig();
		ConfigSection cfgDownload = cfgFull.get("download");
		List<ConfigItem> downloadItems = cfgDownload.getItems();
		for (int i=0;i <= downloadItems.size();i++) {   //Loop in download configuration section
			ConfigItem cfgItem = downloadItems.get(i); //Get single download configuration item
			if (cfgItem.name.equalsIgnoreCase("limit_speed")) {
				CheckBox cbLimitSpeed = (CheckBox) findViewById(R.id.checkBoxLimitSpeed);
				cbLimitSpeed.setEnabled(true);
				
				if(cfgItem.value.equalsIgnoreCase("True")) cbLimitSpeed.setChecked(true);
				else cbLimitSpeed.setChecked(false);
			}
			
		}
	
	}
	//Function called when checkbox to limit speed is clicked
	protected void onToggleLimitSpeed() {

		CheckBox cbLimitSpeed = (CheckBox) findViewById(R.id.checkBoxLimitSpeed);
		if(cbLimitSpeed.isChecked()) client.setConfigValue("download", "limit_speed", "True", "core");
		else client.setConfigValue("download", "limit_speed", "False", "core");
	}
	
	
	
	
	
}
