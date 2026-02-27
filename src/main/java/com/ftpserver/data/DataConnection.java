package com.ftpserver.data;

import java.io.*;
import java.net.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * 数据连接抽象类，处理FTP数据传输
 */
public abstract class DataConnection implements AutoCloseable {
    protected Socket socket;
    protected InputStream inputStream;
    protected OutputStream outputStream;
    protected boolean asciiMode;
    protected static final Logger logger = Logger.getLogger(DataConnection.class.getName());

    /**
     * 构造函数
     */
    public DataConnection() {
        this.asciiMode = false;
    }

    /**
     * 设置ASCII模式
     * @param asciiMode 是否使用ASCII模式
     */
    public void setAsciiMode(boolean asciiMode) {
        this.asciiMode = asciiMode;
    }

    /**
     * 获取当前是否为ASCII模式
     * @return 是否为ASCII模式
     */
    public boolean isAsciiMode() {
        return asciiMode;
    }

    /**
     * 连接到客户端
     * @throws DataConnectionException 数据连接异常
     */
    public abstract void connect() throws DataConnectionException;

    /**
     * 获取输入流
     * @return 输入流
     * @throws DataConnectionException 数据连接异常
     */
    public InputStream getInputStream() throws DataConnectionException {
        try {
            if (inputStream == null) {
                if (socket == null || socket.isClosed()) {
                    throw new DataConnectionException("Socket is not connected", DataConnectionException.ErrorType.CONNECTION_ERROR);
                }
                inputStream = socket.getInputStream();
                if (asciiMode) {
                    inputStream = new AsciiInputStream(inputStream);
                }
            }
            return inputStream;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to get input stream", e);
            throw new DataConnectionException("Failed to get input stream", DataConnectionException.ErrorType.RESOURCE_ERROR, e);
        }
    }

    /**
     * 获取输出流
     * @return 输出流
     * @throws DataConnectionException 数据连接异常
     */
    public OutputStream getOutputStream() throws DataConnectionException {
        try {
            if (outputStream == null) {
                if (socket == null || socket.isClosed()) {
                    throw new DataConnectionException("Socket is not connected", DataConnectionException.ErrorType.CONNECTION_ERROR);
                }
                outputStream = socket.getOutputStream();
                if (asciiMode) {
                    outputStream = new AsciiOutputStream(outputStream);
                }
            }
            return outputStream;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to get output stream", e);
            throw new DataConnectionException("Failed to get output stream", DataConnectionException.ErrorType.RESOURCE_ERROR, e);
        }
    }

    /**
     * 发送文件
     * @param file 要发送的文件
     * @throws DataConnectionException 数据连接异常
     */
    public void sendFile(File file) throws DataConnectionException {
        if (!file.exists() || !file.isFile()) {
            throw new DataConnectionException("File does not exist or is not a file", DataConnectionException.ErrorType.RESOURCE_ERROR);
        }
        
        if (!file.canRead()) {
            throw new DataConnectionException("Cannot read file", DataConnectionException.ErrorType.RESOURCE_ERROR);
        }
        
        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytesSent = 0;
            long fileLength = file.length();
            
            logger.log(Level.INFO, "Starting file transfer: " + file.getName() + " (" + fileLength + " bytes)");
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
                totalBytesSent += bytesRead;
            }
            os.flush();
            
            // 验证传输大小
            if (totalBytesSent != fileLength) {
                logger.log(Level.WARNING, "File transfer incomplete: expected " + fileLength + " bytes, sent " + totalBytesSent);
                throw new DataConnectionException("File transfer incomplete: expected " + fileLength + " bytes, sent " + totalBytesSent, 
                                                DataConnectionException.ErrorType.TRANSFER_ERROR);
            }
            
            logger.log(Level.INFO, "File transfer completed: " + file.getName() + " (" + totalBytesSent + " bytes)");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "File transfer failed", e);
            throw new DataConnectionException("File transfer failed", DataConnectionException.ErrorType.TRANSFER_ERROR, e);
        }
    }

    /**
     * 接收文件
     * @param file 要接收的文件
     * @throws DataConnectionException 数据连接异常
     */
    public void receiveFile(File file) throws DataConnectionException {
        try {
            // 确保父目录存在
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new DataConnectionException("Cannot create parent directory", DataConnectionException.ErrorType.RESOURCE_ERROR);
                }
            }
            
            try (FileOutputStream fos = new FileOutputStream(file);
                 InputStream is = getInputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesReceived = 0;
                
                logger.log(Level.INFO, "Starting file reception: " + file.getName());
                
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalBytesReceived += bytesRead;
                }
                
                logger.log(Level.INFO, "File reception completed: " + file.getName() + " (" + totalBytesReceived + " bytes)");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "File reception failed", e);
            throw new DataConnectionException("File reception failed", DataConnectionException.ErrorType.TRANSFER_ERROR, e);
        }
    }

    /**
     * 发送目录列表
     * @param listing 目录列表内容
     * @throws DataConnectionException 数据连接异常
     */
    public void sendListing(String listing) throws DataConnectionException {
        try (OutputStream os = getOutputStream()) {
            logger.log(Level.INFO, "Sending directory listing");
            os.write(listing.getBytes("UTF-8"));
            os.flush();
            logger.log(Level.INFO, "Directory listing sent successfully");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to send directory listing", e);
            throw new DataConnectionException("Failed to send directory listing", DataConnectionException.ErrorType.TRANSFER_ERROR, e);
        }
    }

    /**
     * 关闭数据连接
     * @throws DataConnectionException 数据连接异常
     */
    @Override
    public void close() throws DataConnectionException {
        try {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Error closing input stream", e);
                } finally {
                    inputStream = null;
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Error closing output stream", e);
                } finally {
                    outputStream = null;
                }
            }
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Error closing socket", e);
                } finally {
                    socket = null;
                }
            }
            logger.log(Level.INFO, "Data connection closed");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error closing data connection", e);
            throw new DataConnectionException("Error closing data connection", DataConnectionException.ErrorType.RESOURCE_ERROR, e);
        }
    }

    /**
     * ASCII输入流，处理行尾转换
     */
    private static class AsciiInputStream extends FilterInputStream {
        private static final byte CR = 13;
        private static final byte LF = 10;
        private boolean lastWasCR = false;

        /**
         * 构造函数
         * @param in 底层输入流
         */
        protected AsciiInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b == -1) {
                return -1;
            }
            if (lastWasCR) {
                lastWasCR = false;
                if (b == LF) {
                    // CRLF -> LF
                    return LF;
                } else {
                    // CR not followed by LF, return both
                    lastWasCR = (b == CR);
                    return CR;
                }
            } else {
                if (b == CR) {
                    lastWasCR = true;
                    return read(); // 递归读取下一个字符
                } else {
                    return b;
                }
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int count = 0;
            for (int i = off; i < off + len; i++) {
                int c = read();
                if (c == -1) break;
                b[i] = (byte) c;
                count++;
            }
            return count == 0 ? -1 : count;
        }
    }

    /**
     * ASCII输出流，处理行尾转换
     */
    private static class AsciiOutputStream extends FilterOutputStream {
        private static final byte CR = 13;
        private static final byte LF = 10;

        /**
         * 构造函数
         * @param out 底层输出流
         */
        protected AsciiOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            if (b == LF) {
                // LF -> CRLF
                out.write(CR);
                out.write(LF);
            } else if (b != CR) {
                // 只写入非CR字符，避免双重转换
                out.write(b);
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            for (int i = off; i < off + len; i++) {
                write(b[i]);
            }
        }
    }
}
