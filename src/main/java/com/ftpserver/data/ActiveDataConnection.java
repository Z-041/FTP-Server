package com.ftpserver.data;

import com.ftpserver.util.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class ActiveDataConnection extends DataConnection {
    private final InetAddress clientAddress;
    private final int clientPort;
    private static final Logger logger = Logger.getInstance();

    public ActiveDataConnection(InetAddress clientAddress, int clientPort) {
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
    }

    @Override
    public void connect() throws DataConnectionException {
        try {
            logger.info("Connecting to client " + clientAddress + ":" + clientPort, "ActiveDataConnection", clientAddress.getHostAddress());
            socket = new Socket(clientAddress, clientPort);
            socket.setSoTimeout(30000);
            logger.info("Active data connection established", "ActiveDataConnection", clientAddress.getHostAddress());
        } catch (IOException e) {
            logger.error("Failed to establish active data connection: " + e.getMessage(), "ActiveDataConnection", clientAddress.getHostAddress());
            throw new DataConnectionException("Failed to establish active data connection",
                                           DataConnectionException.ErrorType.CONNECTION_ERROR, e);
        }
    }
}
