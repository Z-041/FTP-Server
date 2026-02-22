package com.ftpserver.data;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class ActiveDataConnection extends DataConnection {
    private final InetAddress clientAddress;
    private final int clientPort;

    public ActiveDataConnection(InetAddress clientAddress, int clientPort) {
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
    }

    @Override
    public void connect() throws IOException {
        socket = new Socket(clientAddress, clientPort);
        socket.setSoTimeout(30000);
    }
}
