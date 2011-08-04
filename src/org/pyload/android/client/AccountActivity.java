package org.pyload.android.client;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.pyload.android.client.module.GuiTask;
import org.pyload.thrift.AccountInfo;
import org.pyload.thrift.Pyload.Client;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class AccountActivity extends ListActivity {

	private pyLoadApp app;
	private AccountAdapter adp;
	
	private List<AccountInfo> accounts;
	//private List<String> types;

	private Runnable mUpdateResults = new Runnable() {

		@Override
		public void run() {
			adp.setData(accounts);
			app.setProgress(false);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		app = (pyLoadApp) getApplicationContext();
		
		adp = new AccountAdapter(app);		
		setListAdapter(adp);

	}

	@Override
	protected void onResume() {
		super.onResume();
		
		if (!app.hasConnection())
			return;

		app.setProgress(true);

		GuiTask task = new GuiTask(new Runnable() {

			public void run() {
				Client client = app.getClient();
				accounts = client.getAccounts(false);
				//types = client.getAccountTypes();

			}
		}, mUpdateResults);

		app.addTask(task);
		
	}

}

class AccountAdapter extends BaseAdapter {
	
	static class ViewHolder {
		private TextView type;
		private TextView name;
		private TextView valid;
		private TextView validuntil;
		private TextView trafficleft;
	}

	private pyLoadApp app;
	private LayoutInflater layoutInflater;
	private List<AccountInfo> data;
	
	public AccountAdapter(pyLoadApp app) {
		this.app = app;
		layoutInflater = (LayoutInflater) app
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		data = new ArrayList<AccountInfo>();
	}

	@Override
	public int getCount() {
		return data.size();
	}

	public void setData(List<AccountInfo> accounts) {
		data = accounts;
		notifyDataSetChanged();		
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
	public View getView(int pos, View view, ViewGroup group) {
		
		ViewHolder holder;
		
		AccountInfo acc = data.get(pos);
		
		if (view == null) {
			view = layoutInflater.inflate(R.layout.account_item, null);
			
			holder = new ViewHolder();
			
			holder.type = (TextView) view.findViewById(R.id.type);
			holder.name = (TextView) view.findViewById(R.id.name);
			holder.valid = (TextView) view.findViewById(R.id.valid);
			holder.validuntil = (TextView) view.findViewById(R.id.validuntil);
			holder.trafficleft = (TextView) view.findViewById(R.id.trafficleft);
			
			view.setTag(holder);
		}
		
		holder = (ViewHolder) view.getTag();
		
		holder.type.setText(acc.type);
		holder.name.setText(acc.login);
		
		if (acc.valid)
			holder.valid.setText(R.string.valid);
		else
			holder.valid.setText(R.string.invalid);
		
		if (acc.trafficleft < 0)
			holder.trafficleft.setText(R.string.unlimited);
		else
			holder.trafficleft.setText(app.formatSize(acc.trafficleft));
		
		if (acc.validuntil < 0)
			holder.validuntil.setText(R.string.unlimited);
		else {
			Date date = new Date();
			date.setTime(acc.validuntil * 1000);
			SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");  			
			holder.validuntil.setText(formatter.format(date));
		}
			
		
		return view;
		
	}

}
