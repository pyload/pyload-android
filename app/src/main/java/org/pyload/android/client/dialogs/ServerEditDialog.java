package org.pyload.android.client.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import org.pyload.android.client.R;
import org.pyload.android.client.models.Server;
import org.pyload.android.client.module.ServerManager;

public class ServerEditDialog extends DialogFragment {

    private static final String ARG_SERVER_ID = "server_id";

    public interface OnServerSavedListener {
        void onServerSaved(Server server);
    }

    private OnServerSavedListener listener;
    private Server existingServer;

    public static ServerEditDialog newInstance(@Nullable String serverId) {
        ServerEditDialog dialog = new ServerEditDialog();
        Bundle args = new Bundle();
        if (serverId != null) {
            args.putString(ARG_SERVER_ID, serverId);
        }
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnServerSavedListener(OnServerSavedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_server_edit, null);

        TextInputEditText editName = view.findViewById(R.id.edit_server_name);
        TextInputEditText editHost = view.findViewById(R.id.edit_server_host);
        TextInputEditText editPort = view.findViewById(R.id.edit_server_port);
        TextInputEditText editPathPrefix = view.findViewById(R.id.edit_server_path_prefix);
        CheckBox checkSsl = view.findViewById(R.id.check_server_ssl);
        CheckBox checkSslValidate = view.findViewById(R.id.check_server_ssl_validate);
        TextInputEditText editApiKey = view.findViewById(R.id.edit_server_api_key);

        String serverId = getArguments() != null ? getArguments().getString(ARG_SERVER_ID) : null;
        if (serverId != null) {
            existingServer = ServerManager.getInstance(requireContext()).getServer(serverId);
        }

        if (existingServer != null) {
            editName.setText(existingServer.getName());
            editHost.setText(existingServer.getHost());
            editPort.setText(existingServer.getPort());
            editPathPrefix.setText(existingServer.getPathPrefix());
            checkSsl.setChecked(existingServer.isSsl());
            checkSslValidate.setChecked(existingServer.isSslValidate());
            editApiKey.setText(existingServer.getApiKey());
        }

        int titleRes = existingServer != null ? R.string.edit_server : R.string.add_server;

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(titleRes)
                .setView(view)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String name = editName.getText() != null ? editName.getText().toString().trim() : "";
                    String host = editHost.getText() != null ? editHost.getText().toString().trim() : "";
                    String port = editPort.getText() != null ? editPort.getText().toString().trim() : "8000";
                    if (port.isEmpty()) port = "8000";
                    String pathPrefix = editPathPrefix.getText() != null ? editPathPrefix.getText().toString().trim() : "";
                    boolean ssl = checkSsl.isChecked();
                    boolean sslValidate = checkSslValidate.isChecked();
                    String apiKey = editApiKey.getText() != null ? editApiKey.getText().toString().trim() : "";

                    Server server;
                    if (existingServer != null) {
                        server = existingServer;
                        server.setName(name);
                        server.setHost(host);
                        server.setPort(port);
                        server.setPathPrefix(pathPrefix);
                        server.setSsl(ssl);
                        server.setSslValidate(sslValidate);
                        server.setApiKey(apiKey);
                        ServerManager.getInstance(requireContext()).updateServer(server);
                    } else {
                        server = new Server(null, name, host, port, pathPrefix, ssl, sslValidate, apiKey);
                        ServerManager.getInstance(requireContext()).addServer(server);
                    }

                    if (listener != null) {
                        listener.onServerSaved(server);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
    }
}
