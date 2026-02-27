package com.ftpserver.data;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;
import java.util.logging.Level;

public class PassiveDataConnection extends DataConnection {
    private ServerSocket serverSocket;
    private final InetAddress serverAddress;
    private final int port;
    private static final Logger logger = Logger.getLogger(PassiveDataConnection.class.getName());

    public PassiveDataConnection(InetAddress serverAddress, int port) throws DataConnectionException {
        try {
            this.serverAddress = serverAddress;
            this.port = port;
            this.serverSocket = new ServerSocket(port, 1, serverAddress);
            this.serverSocket.setSoTimeout(60000);
            logger.log(Level.INFO, "Passive data connection server socket created on " + serverAddress + ":" + getPort());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create passive data connection server socket", e);
            throw new DataConnectionException("Failed to create passive data connection server socket", 
                                           DataConnectionException.ErrorType.CONNECTION_ERROR, e);
        }
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    public InetAddress getServerAddress() {
        return serverAddress;
    }

    @Override
    public void connect() throws DataConnectionException {
        try {
            logger.log(Level.INFO, "Waiting for client connection on " + serverAddress + ":" + getPort());
            socket = serverSocket.accept();
            socket.setSoTimeout(30000);
            logger.log(Level.INFO, "Passive data connection established with " + socket.getInetAddress());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to accept passive data connection", e);
            throw new DataConnectionException("Failed to accept passive data connection", 
                                           DataConnectionException.ErrorType.CONNECTION_ERROR, e);
        }
    }

    @Override
    public void close() throws DataConnectionException {
        try {
            super.close();
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                    logger.log(Level.INFO, "Passive data connection server socket closed");
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Error closing server socket", e);
                }
            }
        } catch (DataConnectionException e) {
            logger.log(Level.SEVERE, "Error closing passive data connection", e);
            throw e;
        }
    }
}
