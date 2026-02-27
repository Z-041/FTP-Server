package com.ftpserver.data;

import com.ftpserver.util.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class PassiveDataConnection extends DataConnection {
    private ServerSocket serverSocket;
    private final InetAddress serverAddress;
    private final int port;
    private static final Logger logger = Logger.getInstance();

    public PassiveDataConnection(InetAddress serverAddress, int port) throws DataConnectionException {
        try {
            this.serverAddress = serverAddress;
            this.port = port;
            this.serverSocket = new ServerSocket(port, 1, serverAddress);
            this.serverSocket.setSoTimeout(60000);
            logger.info("Passive data connection server socket created on " + serverAddress + ":" + getPort(), "PassiveDataConnection", null);
        } catch (IOException e) {
            logger.error("Failed to create passive data connection server socket: " + e.getMessage(), "PassiveDataConnection", null);
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
            logger.info("Waiting for client connection on " + serverAddress + ":" + getPort(), "PassiveDataConnection", null);
            socket = serverSocket.accept();
            socket.setSoTimeout(30000);
            logger.info("Passive data connection established with " + socket.getInetAddress(), "PassiveDataConnection", socket.getInetAddress().getHostAddress());
        } catch (IOException e) {
            logger.error("Failed to accept passive data connection: " + e.getMessage(), "PassiveDataConnection", null);
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
                    logger.info("Passive data connection server socket closed", "PassiveDataConnection", null);
                } catch (IOException e) {
                    logger.warn("Error closing server socket: " + e.getMessage(), "PassiveDataConnection", null);
                }
            }
        } catch (DataConnectionException e) {
            logger.error("Error closing passive data connection: " + e.getMessage(), "PassiveDataConnection", null);
            throw e;
        }
    }
}
