package com.ftpserver.ui.model;

import javafx.beans.property.SimpleStringProperty;

public class ClientRow {
    public final SimpleStringProperty ip;
    public final SimpleStringProperty port;
    public final SimpleStringProperty connectTime;
    public final SimpleStringProperty status;

    public ClientRow(String ip, String port, String connectTime, String status) {
        this.ip = new SimpleStringProperty(ip);
        this.port = new SimpleStringProperty(port);
        this.connectTime = new SimpleStringProperty(connectTime);
        this.status = new SimpleStringProperty(status);
    }
}