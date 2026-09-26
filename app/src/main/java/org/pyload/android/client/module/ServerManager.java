package org.pyload.android.client.module;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.pyload.android.client.models.Server;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ServerManager {
    private static final String PREF_SERVERS_JSON = "servers_json";
    private static final String PREF_ACTIVE_SERVER_ID = "active_server_id";

    private static ServerManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final List<Server> servers = new ArrayList<>();
    private String activeServerId;

    private ServerManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(
                context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        gson = new Gson();
        loadServers();
    }

    public static synchronized ServerManager getInstance(Context context) {
        if (instance == null) {
            instance = new ServerManager(context);
        }
        return instance;
    }

    private void loadServers() {
        servers.clear();
        String json = prefs.getString(PREF_SERVERS_JSON, null);
        if (json != null && !json.trim().isEmpty()) {
            Type type = new TypeToken<ArrayList<Server>>() {}.getType();
            List<Server> loaded = gson.fromJson(json, type);
            if (loaded != null) {
                servers.addAll(loaded);
            }
        }

        if (servers.isEmpty()) {
            migrateLegacyServer();
        }

        activeServerId = prefs.getString(PREF_ACTIVE_SERVER_ID, null);

        boolean found = false;
        for (Server s : servers) {
            if (s.getId().equals(activeServerId)) {
                found = true;
                break;
            }
        }

        if (!found && !servers.isEmpty()) {
            activeServerId = servers.get(0).getId();
            prefs.edit().putString(PREF_ACTIVE_SERVER_ID, activeServerId).apply();
        }

        syncLegacyPrefs();
    }

    private void migrateLegacyServer() {
        String host = prefs.getString("host", "10.0.2.2");
        String port = prefs.getString("port", "8000");
        String pathPrefix = prefs.getString("path_prefix", "");
        boolean ssl = prefs.getBoolean("ssl", false);
        boolean sslValidate = prefs.getBoolean("ssl_validate", true);
        String apiKey = prefs.getString("api_key", "");

        Server defaultServer = new Server(
                UUID.randomUUID().toString(),
                "Default Server",
                host,
                port,
                pathPrefix,
                ssl,
                sslValidate,
                apiKey
        );

        servers.add(defaultServer);
        activeServerId = defaultServer.getId();

        saveServers();
        prefs.edit().putString(PREF_ACTIVE_SERVER_ID, activeServerId).apply();
    }

    private void saveServers() {
        String json = gson.toJson(servers);
        prefs.edit().putString(PREF_SERVERS_JSON, json).apply();
    }

    public synchronized List<Server> getServers() {
        return new ArrayList<>(servers);
    }

    public synchronized Server getServer(String id) {
        if (id == null) return null;
        for (Server s : servers) {
            if (s.getId().equals(id)) {
                return s;
            }
        }
        return null;
    }

    public synchronized Server getActiveServer() {
        Server server = getServer(activeServerId);
        if (server == null && !servers.isEmpty()) {
            server = servers.get(0);
            activeServerId = server.getId();
            prefs.edit().putString(PREF_ACTIVE_SERVER_ID, activeServerId).apply();
        }
        if (server == null) {
            server = new Server(UUID.randomUUID().toString(), "Default Server", "10.0.2.2", "8000", "", false, true, "");
            servers.add(server);
            activeServerId = server.getId();
            saveServers();
            prefs.edit().putString(PREF_ACTIVE_SERVER_ID, activeServerId).apply();
        }
        return server;
    }

    public synchronized void setActiveServerId(String id) {
        if (id == null) return;
        Server server = getServer(id);
        if (server != null) {
            activeServerId = id;
            prefs.edit().putString(PREF_ACTIVE_SERVER_ID, activeServerId).apply();
            syncLegacyPrefs();
        }
    }

    public synchronized void addServer(Server server) {
        if (server == null) return;
        servers.add(server);
        saveServers();
        if (servers.size() == 1) {
            setActiveServerId(server.getId());
        }
    }

    public synchronized void updateServer(Server updatedServer) {
        if (updatedServer == null) return;
        for (int i = 0; i < servers.size(); i++) {
            if (servers.get(i).getId().equals(updatedServer.getId())) {
                servers.set(i, updatedServer);
                saveServers();
                if (updatedServer.getId().equals(activeServerId)) {
                    syncLegacyPrefs();
                }
                return;
            }
        }
    }

    public synchronized boolean deleteServer(String id) {
        if (id == null) return false;
        Server target = getServer(id);
        if (target != null) {
            servers.remove(target);
            saveServers();
            if (id.equals(activeServerId)) {
                if (!servers.isEmpty()) {
                    setActiveServerId(servers.get(0).getId());
                } else {
                    activeServerId = null;
                    prefs.edit().remove(PREF_ACTIVE_SERVER_ID).apply();
                    migrateLegacyServer();
                }
            }
            return true;
        }
        return false;
    }

    public synchronized void syncLegacyPrefs() {
        Server active = getActiveServer();
        if (active != null) {
            prefs.edit()
                    .putString("host", active.getHost())
                    .putString("port", active.getPort())
                    .putString("path_prefix", active.getPathPrefix())
                    .putBoolean("ssl", active.isSsl())
                    .putBoolean("ssl_validate", active.isSslValidate())
                    .putString("api_key", active.getApiKey())
                    .apply();
        }
    }
}
