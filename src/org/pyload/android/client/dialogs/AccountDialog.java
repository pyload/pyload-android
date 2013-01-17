package org.pyload.android.client.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import org.pyload.android.client.R;
import org.pyload.android.client.module.GuiTask;
import org.pyload.android.client.module.Utils;
import org.pyload.android.client.pyLoadApp;
import org.pyload.thrift.AccountInfo;
import org.pyload.thrift.Pyload;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AccountDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AccountAdapter adapter = new AccountAdapter(getActivity());

        final pyLoadApp app = (pyLoadApp) getActivity().getApplication();
        GuiTask task = new GuiTask(new Runnable() {
            public void run() {
                Pyload.Client client = app.getClient();
                adapter.setData(client.getAccounts(false));
            }
        });
        app.addTask(task);

        ListView lv = new ListView(getActivity());
        lv.setAdapter(adapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(true)
               .setView(lv)
               .setTitle(R.string.accounts)
               .setPositiveButton(R.string.close, null);
        return builder.create();
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

    private LayoutInflater layoutInflater;
    private List<AccountInfo> data;

    public AccountAdapter(final Context context) {
        layoutInflater = (LayoutInflater)  context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        data = new ArrayList<AccountInfo>();
    }

    public int getCount() {
        // this is a hack to show empty list item in getView()
        return (data.size() > 0 ? data.size() : 1);
    }

    public void setData(List<AccountInfo> accounts) {
        data = accounts;
        notifyDataSetChanged();

    }

    public Object getItem(int arg0) {
        return data.get(arg0);
    }

    public long getItemId(int arg0) {
        return arg0;
    }

    public View getView(int pos, View view, ViewGroup group) {
        ViewHolder holder;

        AccountInfo acc = (data.size() == 0 ? null : data.get(pos));
        // here comes the empty list view
        if (acc == null) {
            return layoutInflater.inflate(R.layout.account_empty_list, null);
        }

        view = layoutInflater.inflate(R.layout.account_item, null);

        holder = new ViewHolder();

        holder.type = (TextView) view.findViewById(R.id.type);
        holder.name = (TextView) view.findViewById(R.id.name);
        holder.valid = (TextView) view.findViewById(R.id.valid);
        holder.validuntil = (TextView) view.findViewById(R.id.validuntil);
        holder.trafficleft = (TextView) view.findViewById(R.id.trafficleft);

        view.setTag(holder);

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
            holder.trafficleft.setText(Utils.formatSize(acc.trafficleft));

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
