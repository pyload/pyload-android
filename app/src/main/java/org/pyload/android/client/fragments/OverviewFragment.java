package org.pyload.android.client.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.ListFragment;

import org.pyload.android.client.R;
import org.pyload.android.client.components.TabHandler;
import org.pyload.android.client.module.GuiTask;
import org.pyload.android.client.module.Utils;
import org.pyload.android.client.pyLoadApp;
import org.pyload.android.openapi.api.PyLoadRestApi;
import org.pyload.android.openapi.model.ApiStopDownloadsPostRequest;
import org.pyload.android.openapi.model.DownloadInfo;
import org.pyload.android.openapi.model.DownloadStatus;
import org.pyload.android.openapi.model.ServerStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OverviewFragment extends ListFragment implements TabHandler {

    private pyLoadApp app;
    private PyLoadRestApi client;
    private OverviewAdapter adp;

    private List<DownloadInfo> downloads;
    private List<DownloadInfo> allDownloads;
    private String filter = "";
    private ServerStatus serverStatus;
    private int interval = 5;
    private boolean update = false;
    // tab position
    private int pos = -1;

    /**
     * GUI Elements
     */
    private TextView statusServer;
    private TextView reconnect;
    private TextView speed;
    private TextView active;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mUpdateResults = new Runnable() {

        public void run() {
            onDataReceived();
        }
    };
    private final Runnable runUpdate = new Runnable() {

        public void run() {
            client = app.getClient();
            downloads = app.executeNetworkCall(client.apiStatusDownloadsGet());
            serverStatus = app.executeNetworkCall(client.apiStatusServerGet());
        }
    };

    private final Runnable cancelUpdate = new Runnable() {

        public void run() {
            stopUpdate();
        }
    };

    private final Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            refresh();
            if (update)
                mHandler.postDelayed(this, (long) interval * 1000);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app = (pyLoadApp) requireActivity().getApplicationContext();

        downloads = new ArrayList<>();
        adp = new OverviewAdapter(requireContext(), R.layout.overview_item, downloads);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.overview, container, false);

        statusServer = (TextView) v.findViewById(R.id.status_server);
        reconnect = (TextView) v.findViewById(R.id.reconnect);
        speed = (TextView) v.findViewById(R.id.speed);
        active = (TextView) v.findViewById(R.id.active);

        // toggle pause on click
        statusServer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                app.addTask(new GuiTask(new Runnable() {
                    public void run() {
                        PyLoadRestApi client = app.getClient();
                        app.executeNetworkCall(client.apiTogglePausePost());
                    }
                }, app.handleSuccess));
            }
        });

        // toggle reconnect on click
        reconnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                app.addTask(new GuiTask(new Runnable() {
                    public void run() {
                        PyLoadRestApi client = app.getClient();
                        app.executeNetworkCall(client.apiToggleReconnectPost());
                    }
                }, app.handleSuccess));
            }
        });

        if (serverStatus != null && downloads != null)
            onDataReceived();

        registerForContextMenu(v.findViewById(android.R.id.list));

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setListAdapter(adp);
        getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
                    Utils.hideKeyboard(view);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        onSelected();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.overview_context_menu, menu);
        menu.setHeaderTitle(R.string.choose_action);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        if (!app.isCurrentTab(pos))
            return false;

        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item
                .getMenuInfo();
        final int id = menuInfo.position;
        final DownloadInfo info = downloads.get(id);
        int itemId = item.getItemId();
        if (itemId == R.id.abort) {

            app.addTask(new GuiTask(new Runnable() {

                public void run() {
                    client = app.getClient();
                    ArrayList<Integer> fids = new ArrayList<>();
                    fids.add(info.getFid());
                    ApiStopDownloadsPostRequest request = new ApiStopDownloadsPostRequest().fileIds(fids);
                    app.executeNetworkCall(client.apiStopDownloadsPost(request));
                }
            }, new Runnable() {

                public void run() {
                    refresh();
                }
            }));
            return true;
        }

        return super.onContextItemSelected(item);

    }

    @Override
    public void onSelected() {
        this.filter = ""; // Reset filter on selection
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
        startUpdate();
    }

    @Override
    public void onDeselected() {
        stopUpdate();
    }

    @Override
    public void onSearch(String query) {
        this.filter = query;
        applyFilter();
    }

    private void applyFilter() {
        if (allDownloads == null) return;

        if (TextUtils.isEmpty(filter)) {
            downloads = new ArrayList<>(allDownloads);
        } else {
            downloads = new ArrayList<>();
            String lowerFilter = filter.toLowerCase();
            for (DownloadInfo info : allDownloads) {
                if (info.getName().toLowerCase().contains(lowerFilter)) {
                    downloads.add(info);
                }
            }
        }

        if (adp != null) {
            adp.setDownloads(downloads);
        }
    }

    private void startUpdate() {
        // already update running
        if (update)
            return;
        try {
            interval = Integer.parseInt(app.prefs
                    .getString("refresh_rate", "5"));
        } catch (NumberFormatException e) {
            // somehow contains illegal value
            interval = 5;
        }

        update = true;
        mHandler.post(mUpdateTimeTask);
    }

    private void stopUpdate() {
        update = false;
        mHandler.removeCallbacks(mUpdateTimeTask);
    }

    /**
     * Called when Status data received
     */
    protected void onDataReceived() {
        app.setProgress(false);
        if (serverStatus == null || downloads == null)
            return;

        allDownloads = new ArrayList<>(downloads);
        if (!TextUtils.isEmpty(filter)) {
            applyFilter();
        } else {
            OverviewAdapter adapter = (OverviewAdapter) getListAdapter();

            if (adapter != null)
                adapter.setDownloads(downloads);
        }

        statusServer.setText(app.verboseBool(serverStatus.getDownload()));
        reconnect.setText(app.verboseBool(serverStatus.getReconnect()));
        speed.setText(Utils.formatSize(serverStatus.getSpeed()) + "/s");
        active.setText(String.format(Locale.US, "%d / %d", serverStatus.getActive(), serverStatus.getTotal()));

    }

    public void refresh() {
        if (!app.hasConnection())
            return;

        app.setProgress(true);
        GuiTask task = new GuiTask(runUpdate, mUpdateResults);
        task.setCritical(cancelUpdate);

        app.addTask(task);
    }

    @Override
    public void setPosition(int pos) {
        this.pos = pos;
    }
}

/**
 * Renders the single ListView items
 *
 * @author RaNaN
 *
 */
class OverviewAdapter extends BaseAdapter {

    static class ViewHolder {
        private TextView name;
        private ProgressBar progress;
        private TextView size;
        private TextView percent;
        private TextView size_done;
        private TextView speed;
        private TextView eta;
    }

    private final pyLoadApp app;
    private List<DownloadInfo> downloads;
    private final int rowResID;
    private final LayoutInflater layoutInflater;

    public OverviewAdapter(final Context context, final int rowResID,
                           List<DownloadInfo> downloads) {
        this.app = (pyLoadApp) context.getApplicationContext();
        this.rowResID = rowResID;
        this.downloads = downloads;

        layoutInflater = LayoutInflater.from(context);
    }

    public void setDownloads(List<DownloadInfo> downloads) {
        this.downloads = downloads;
        notifyDataSetChanged();
    }

    public int getCount() {
        return downloads.size();
    }

    public Object getItem(int id) {
        return downloads.get(id);
    }

    public long getItemId(int pos) {
        return pos;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        DownloadInfo info = downloads.get(position);
        if (convertView == null) {
            convertView = layoutInflater.inflate(rowResID, null);
            ViewHolder holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.progress = (ProgressBar) convertView
                    .findViewById(R.id.progress);
            holder.size = (TextView) convertView.findViewById(R.id.size);
            holder.speed = (TextView) convertView.findViewById(R.id.speed);
            holder.size_done = (TextView) convertView
                    .findViewById(R.id.size_done);
            holder.eta = (TextView) convertView.findViewById(R.id.eta);
            holder.percent = (TextView) convertView.findViewById(R.id.percent);
            convertView.setTag(holder);
        }

        ViewHolder holder = (ViewHolder) convertView.getTag();


        // name is null sometimes somehow
        if (info.getName() != null && !info.getName().equals(holder.name.getText())) {
            holder.name.setText(info.getName());
        }

        boolean marquee = app.prefs.getBoolean("package_marquee", true);
        if (marquee) {
            holder.name.setSingleLine(true);
            holder.name.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            holder.name.setHorizontallyScrolling(true);
        } else {
            holder.name.setSingleLine(false);
            holder.name.setEllipsize(null);
            holder.name.setHorizontallyScrolling(false);
        }

        holder.progress.setProgress(info.getPercent());

        if (info.getStatus() == DownloadStatus.DOWNLOADING) {
            holder.size.setText(Utils.formatSize(info.getSize()));
            holder.percent.setText(info.getPercent() + "%");
            holder.size_done.setText(Utils.formatSize(info.getSize() - info.getBleft()));

            holder.speed.setText(Utils.formatSize(info.getSpeed()) + "/s");
            holder.eta.setText(info.getFormatEta());

        } else if (info.getStatus() == DownloadStatus.WAITING) {
            holder.size.setText(R.string.lambda);
            holder.percent.setText(R.string.lambda);
            holder.size_done.setText(R.string.lambda);

            holder.speed.setText(info.getStatusmsg());
            holder.eta.setText(info.getFormatWait());

        } else {
            holder.size.setText(R.string.lambda);
            holder.percent.setText(R.string.lambda);
            holder.size_done.setText(R.string.lambda);

            holder.speed.setText(info.getStatusmsg());
            holder.eta.setText(R.string.lambda);
        }

        return convertView;

    }

    public boolean hasStableIds() {
        return false;
    }

}
