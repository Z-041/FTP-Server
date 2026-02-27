package com.ftpserver.ui.model;

public class ClientRow {
    public final String ip;
    public final String port;
    public final String connectTime;
    public final String status;

    public ClientRow(String ip, String port, String connectTime, String status) {
        this.ip = ip;
        this.port = port;
        this.connectTime = connectTime;
        this.status = status;
    }

    public String get() {
        return ip;
    }
}
