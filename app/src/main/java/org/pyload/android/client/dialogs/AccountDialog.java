package org.pyload.android.client.dialogs;

import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.pyload.android.client.R;
import org.pyload.android.client.module.GuiTask;
import org.pyload.android.client.module.Utils;
import org.pyload.android.client.pyLoadApp;
import org.pyload.android.openapi.api.PyLoadRestApi;
import org.pyload.android.openapi.model.AccountInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AccountDialog extends DialogFragment {

    private List<AccountInfo> accountData;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AccountAdapter adapter = new AccountAdapter(getActivity());

        Runnable mSetData = new Runnable() {
            @Override
            public void run() {
                adapter.setData(accountData);
            }
        };

        final pyLoadApp app = (pyLoadApp) getActivity().getApplication();
        GuiTask task = new GuiTask(new Runnable() {
            public void run() {
                PyLoadRestApi client = app.getClient();
                accountData = app.executeNetworkCall(client.apiGetAccountsGet(false));
            }
        }, mSetData);
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
        private TextView premium;
    }

    private LayoutInflater layoutInflater;
    private List<AccountInfo> data;
    private boolean loading = true;

    public AccountAdapter(final Context context) {
        layoutInflater = LayoutInflater.from(context);

        data = new ArrayList<AccountInfo>();
    }

    public int getCount() {
        // this is a hack to show empty list item in getView()
        return (data.size() > 0 ? data.size() : 1);
    }

    public void setData(List<AccountInfo> accounts) {
        data = accounts;
        loading = false;
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
            view = layoutInflater.inflate(R.layout.account_empty_list, group, false);
            ProgressBar progress = (ProgressBar) view.findViewById(R.id.loading_progress);
            TextView text = (TextView) view.findViewById(R.id.no_accounts_text);

            if (loading) {
                progress.setVisibility(View.VISIBLE);
                text.setText(R.string.loading);
            } else {
                progress.setVisibility(View.GONE);
                text.setText(R.string.no_accounts);
            }

            return view;
        }

        view = layoutInflater.inflate(R.layout.account_item, group, false);

        holder = new ViewHolder();

        holder.type = (TextView) view.findViewById(R.id.type);
        holder.name = (TextView) view.findViewById(R.id.name);
        holder.valid = (TextView) view.findViewById(R.id.valid);
        holder.validuntil = (TextView) view.findViewById(R.id.validuntil);
        holder.trafficleft = (TextView) view.findViewById(R.id.trafficleft);
        holder.premium = (TextView) view.findViewById(R.id.premium);

        view.setTag(holder);

        holder = (ViewHolder) view.getTag();

        holder.type.setText(acc.getType());

        if (Boolean.TRUE.equals(acc.getPremium())) {
            holder.premium.setText(R.string.premium);
        } else {
            holder.premium.setText(R.string.free);
        }
        holder.name.setText(acc.getLogin());

        if (Boolean.TRUE.equals(acc.getValid()))
            holder.valid.setText(R.string.valid);
        else
            holder.valid.setText(R.string.invalid);

        Long trafficleft = acc.getTrafficleft();
        if (trafficleft == null)
            holder.trafficleft.setText(R.string.n_a);
        else if (trafficleft == 0)
            holder.trafficleft.setText(R.string.drained);
        else if (trafficleft < 0)
            holder.trafficleft.setText(R.string.unlimited);
        else
            holder.trafficleft.setText(Utils.formatSize(trafficleft));

        Float validuntil = acc.getValiduntil();
        if (validuntil == null)
            holder.validuntil.setText(R.string.n_a);
        else if (validuntil < 0)
            holder.validuntil.setText(R.string.unlimited);
        else {
            Date date = new Date();
            date.setTime(validuntil.longValue() * 1000);
            SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
            holder.validuntil.setText(formatter.format(date));
        }

        return view;
    }

}
