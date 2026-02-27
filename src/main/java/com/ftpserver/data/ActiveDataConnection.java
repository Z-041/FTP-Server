package com.ftpserver.data;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ActiveDataConnection extends DataConnection {
    private final InetAddress clientAddress;
    private final int clientPort;
    private static final Logger logger = Logger.getLogger(ActiveDataConnection.class.getName());

    public ActiveDataConnection(InetAddress clientAddress, int clientPort) {
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
    }

    @Override
    public void connect() throws DataConnectionException {
        try {
            logger.log(Level.INFO, "Connecting to client " + clientAddress + ":" + clientPort);
            socket = new Socket(clientAddress, clientPort);
            socket.setSoTimeout(30000);
            logger.log(Level.INFO, "Active data connection established");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to establish active data connection", e);
            throw new DataConnectionException("Failed to establish active data connection", 
                                           DataConnectionException.ErrorType.CONNECTION_ERROR, e);
        }
    }
}
