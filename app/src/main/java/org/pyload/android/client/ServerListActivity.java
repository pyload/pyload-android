package org.pyload.android.client;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.pyload.android.client.models.Server;
import org.pyload.android.client.module.LanguageUtils;
import org.pyload.android.client.module.ServerManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ServerListActivity extends AppCompatActivity {

    private ServerAdapter adapter;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageUtils.attachBaseContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_list);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.servers);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        RecyclerView recyclerView = findViewById(R.id.recycler_servers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ServerAdapter();
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_add_server);
        fab.setOnClickListener(v -> showEditDialog(null));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.server_list_container), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        loadServers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadServers();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadServers() {
        adapter.setServers(ServerManager.getInstance(this).getServers());
    }

    private void showEditDialog(String serverId) {
        Intent intent = new Intent(this, ServerEditActivity.class);
        if (serverId != null) {
            intent.putExtra(ServerEditActivity.EXTRA_SERVER_ID, serverId);
        }
        startActivity(intent);
    }

    private class ServerAdapter extends RecyclerView.Adapter<ServerAdapter.ViewHolder> {

        private final List<Server> servers = new ArrayList<>();

        public void setServers(List<Server> newServers) {
            servers.clear();
            if (newServers != null) {
                servers.addAll(newServers);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.server_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Server server = servers.get(position);
            Server activeServer = ServerManager.getInstance(ServerListActivity.this).getActiveServer();
            boolean isActive = activeServer != null && activeServer.getId().equals(server.getId());

            holder.textName.setText(server.getName());
            holder.textUrl.setText(server.getFormattedUrl());
            holder.radioActive.setChecked(isActive);

            holder.itemView.setOnClickListener(v -> {
                if (!isActive) {
                    pyLoadApp app = (pyLoadApp) getApplicationContext();
                    app.switchServer(server.getId());
                    loadServers();
                }
            });

            holder.btnEdit.setOnClickListener(v -> showEditDialog(server.getId()));

            holder.btnDelete.setOnClickListener(v -> new MaterialAlertDialogBuilder(ServerListActivity.this)
                    .setTitle(R.string.delete_server)
                    .setMessage(R.string.delete_server_confirm)
                    .setPositiveButton(R.string.delete_server, (dialog, which) -> {
                        ServerManager.getInstance(ServerListActivity.this).deleteServer(server.getId());
                        pyLoadApp app = (pyLoadApp) getApplicationContext();
                        app.resetClient();
                        loadServers();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show());
        }

        @Override
        public int getItemCount() {
            return servers.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            RadioButton radioActive;
            TextView textName;
            TextView textUrl;
            ImageButton btnEdit;
            ImageButton btnDelete;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                radioActive = itemView.findViewById(R.id.radio_active);
                textName = itemView.findViewById(R.id.text_server_name);
                textUrl = itemView.findViewById(R.id.text_server_url);
                btnEdit = itemView.findViewById(R.id.btn_edit_server);
                btnDelete = itemView.findViewById(R.id.btn_delete_server);
            }
        }
    }
}
