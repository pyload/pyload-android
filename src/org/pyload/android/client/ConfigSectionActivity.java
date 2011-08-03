package org.pyload.android.client;

import java.util.ArrayList;

import org.pyload.thrift.ConfigItem;
import org.pyload.thrift.ConfigSection;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class ConfigSectionActivity extends Activity {

	private pyLoadApp app;
	private ConfigSection section;
	private String type;
	private ArrayList<ConfigItemView> items = new ArrayList<ConfigItemView>();

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
			items.add(c);
			ll.addView(c);
		}

		return ll;
	}

	public void onSubmit(View view) {

	}

	public void onCancel(View view) {
		finish();
	}
}

class ConfigItemView extends LinearLayout {

	private ConfigItem item;

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
		
		View v;

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
			
			if (item.value.equals("True")){
				cb.setChecked(true);
			}
			
			v = cb;
		} else if (item.type.contains(";")) {
			Spinner sp = new Spinner(context);
			
			ArrayList<String> choices = new ArrayList<String>();
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
	
	public ConfigItem getItem() {
		return item;
	}

}
