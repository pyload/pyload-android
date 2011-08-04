package org.pyload.android.client;

import java.util.ArrayList;
import java.util.HashMap;

import org.pyload.thrift.ConfigItem;
import org.pyload.thrift.ConfigSection;
import org.pyload.thrift.Pyload.Client;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ConfigSectionActivity extends Activity {

	private pyLoadApp app;
	private ConfigSection section;
	private String type;
	private HashMap<String, ConfigItemView> items = new HashMap<String, ConfigItemView>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		app = (pyLoadApp) getApplicationContext();

		Bundle extras = getIntent().getExtras();

		section = (ConfigSection) extras.getSerializable("section");
		type = extras.getString("type");

		View view = getLayoutInflater().inflate(R.layout.config_section, null);
		createLayout(view);

		TextView t = (TextView) view.findViewById(R.id.list_header_title);
		t.setText(section.description);

		setContentView(view);
	}

	private View createLayout(View view) {
		LinearLayout ll = (LinearLayout) view.findViewById(R.id.layout_root);
		ll.setOrientation(LinearLayout.VERTICAL);

		for (ConfigItem item : section.items) {
			ConfigItemView c = new ConfigItemView(this, item);
			items.put(item.name, c);
			ll.addView(c);
		}

		return ll;
	}

	public void onSubmit(View button) {

		Client client = app.getClient();
		if (client == null)
			return;

		try {

			for (ConfigItem item : section.items) {
				ConfigItemView view = items.get(item.name);
				String newValue = view.getValue();
				if (!item.value.equals(newValue)) {
					Log.d("pyLoad", String.format(
							"Set config value: %s, %s, %s", type, section.name,
							item.name));
					
					client.setConfigValue(section.name, item.name, newValue,
							type);
				}
			}

			finish();
		} catch (Exception e) {
			Toast.makeText(app, R.string.error, Toast.LENGTH_SHORT);
		}

	}

	public void onCancel(View view) {
		finish();
	}
}

class ConfigItemView extends LinearLayout {

	private ConfigItem item;
	private View v;
	private Spinner sp = null;
	private ArrayList<String> choices = null;

	public ConfigItemView(Context context, ConfigItem item) {
		super(context);
		this.item = item;

		setOrientation(LinearLayout.VERTICAL);

		if (!item.type.equals("bool")) {
			TextView tv = new TextView(context);
			tv.setText(item.description);
			tv.setTextColor(Color.WHITE);
			tv.setTextSize(16);
			tv.setPadding(2, 0, 0, 0);
			addView(tv);
		}

		if (item.type.equals("int")) {
			EditText et = new EditText(context);
			et.setInputType(InputType.TYPE_CLASS_NUMBER);
			et.setText(item.value);
			v = et;
		} else if (item.type.equals("password")) {
			EditText et = new EditText(context);
			et.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
			et.setTransformationMethod(PasswordTransformationMethod
					.getInstance());
			et.setText(item.value);
			v = et;
		} else if (item.type.equals("bool")) {
			CheckBox cb = new CheckBox(context);
			cb.setText(item.description);

			if (item.value.equals("True")) {
				cb.setChecked(true);
			}

			v = cb;
		} else if (item.type.contains(";")) {
			sp = new Spinner(context);

			choices = new ArrayList<String>();
			for (String s : item.type.split(";")) {
				choices.add(s);
			}

			ArrayAdapter<String> adp = new ArrayAdapter<String>(context,
					android.R.layout.simple_spinner_item, choices);
			adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

			sp.setAdapter(adp);
			sp.setSelection(choices.indexOf(item.value));

			v = sp;
		} else {
			v = new EditText(context);
			((EditText) v).setText(item.value);
		}

		addView(v);

	}

	/**
	 * Returns the string representation of the config item
	 * 
	 * @return
	 */
	public String getValue() {
		if (item.type.equals("bool")) {
			CheckBox cb = (CheckBox) v;
			if (cb.isChecked())
				return "True";
			else
				return "False";
		} else if (sp != null) {
			return choices.get(sp.getSelectedItemPosition());
		}
		return ((EditText) v).getText().toString();
	}
}
