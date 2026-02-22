package com.ftpserver.data;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class PassiveDataConnection extends DataConnection {
    private ServerSocket serverSocket;
    private final InetAddress serverAddress;
    private final int port;

    public PassiveDataConnection(InetAddress serverAddress, int port) throws IOException {
        this.serverAddress = serverAddress;
        this.port = port;
        this.serverSocket = new ServerSocket(port, 1, serverAddress);
        this.serverSocket.setSoTimeout(60000);
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    public InetAddress getServerAddress() {
        return serverAddress;
    }

    @Override
    public void connect() throws IOException {
        socket = serverSocket.accept();
        socket.setSoTimeout(30000);
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }
}
