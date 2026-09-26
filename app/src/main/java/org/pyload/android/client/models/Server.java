package org.pyload.android.client.models;

import java.util.UUID;

public class Server {
    private String id;
    private String name;
    private String host;
    private String port;
    private String pathPrefix;
    private boolean ssl;
    private boolean sslValidate;
    private String apiKey;

    public Server() {
        this.id = UUID.randomUUID().toString();
        this.name = "";
        this.host = "";
        this.port = "8000";
        this.pathPrefix = "";
        this.ssl = false;
        this.sslValidate = true;
        this.apiKey = "";
    }

    public Server(String id, String name, String host, String port, String pathPrefix, boolean ssl, boolean sslValidate, String apiKey) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = name;
        this.host = host;
        this.port = port != null ? port : "8000";
        this.pathPrefix = pathPrefix != null ? pathPrefix : "";
        this.ssl = ssl;
        this.sslValidate = sslValidate;
        this.apiKey = apiKey != null ? apiKey : "";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        if (host != null && !host.trim().isEmpty()) {
            return host;
        }
        return "Server";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host != null ? host : "";
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port != null ? port : "8000";
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getPathPrefix() {
        return pathPrefix != null ? pathPrefix : "";
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public boolean isSslValidate() {
        return sslValidate;
    }

    public void setSslValidate(boolean sslValidate) {
        this.sslValidate = sslValidate;
    }

    public String getApiKey() {
        return apiKey != null ? apiKey : "";
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getFormattedUrl() {
        String protocol = ssl ? "https://" : "http://";
        String cleanHost = getHost().replaceFirst("^[a-zA-Z]+://", "");
        String prefix = getPathPrefix();
        if (!prefix.isEmpty() && !prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return protocol + cleanHost + ":" + getPort() + prefix;
    }
}
