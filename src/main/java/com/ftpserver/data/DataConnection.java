package com.ftpserver.data;

import com.ftpserver.util.Logger;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 数据连接抽象类，处理FTP数据传输
 */
public abstract class DataConnection implements AutoCloseable {
    protected Socket socket;
    protected InputStream inputStream;
    protected OutputStream outputStream;
    protected boolean asciiMode;
    protected long restartOffset = 0;
    protected static final Logger logger = Logger.getInstance();

    /**
     * 构造函数
     */
    public DataConnection() {
        this.asciiMode = false;
        this.restartOffset = 0;
    }

    /**
     * 设置 ASCII 模式
     * @param asciiMode 是否使用 ASCII 模式
     */
    public void setAsciiMode(boolean asciiMode) {
        this.asciiMode = asciiMode;
    }

    /**
     * 获取当前是否为 ASCII 模式
     * @return 是否为 ASCII 模式
     */
    public boolean isAsciiMode() {
        return asciiMode;
    }

    /**
     * 设置断点续传位置
     * @param offset 续传位置
     */
    public void setRestartOffset(long offset) {
        this.restartOffset = offset;
    }

    /**
     * 获取断点续传位置
     * @return 续传位置
     */
    public long getRestartOffset() {
        return restartOffset;
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
            String ip = socket != null && socket.getInetAddress() != null ? socket.getInetAddress().getHostAddress() : null;
            logger.error("Failed to get input stream: " + e.getMessage(), "DataConnection", ip);
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
            String ip = socket != null && socket.getInetAddress() != null ? socket.getInetAddress().getHostAddress() : null;
            logger.error("Failed to get output stream: " + e.getMessage(), "DataConnection", ip);
            throw new DataConnectionException("Failed to get output stream", DataConnectionException.ErrorType.RESOURCE_ERROR, e);
        }
    }

    private static final int BUFFER_SIZE = 65536;
    private static final int LARGE_FILE_THRESHOLD = 10 * 1024 * 1024; // 10MB
    
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
        
        String ip = socket != null && socket.getInetAddress() != null ? socket.getInetAddress().getHostAddress() : null;
        long fileLength = file.length();
        long totalBytesSent = 0;
        long startTime = System.currentTimeMillis();
        
        try {
            if (restartOffset > 0 && restartOffset < fileLength) {
                totalBytesSent = restartOffset;
                logger.info("Resuming file transfer: " + file.getName() + " at offset " + restartOffset, "DataConnection", ip);
            }
            
            logger.info("Starting file transfer: " + file.getName() + " (" + fileLength + " bytes)", "DataConnection", ip);

            if (asciiMode) {
                // ASCII模式：使用BufferedInputStream提高性能
                try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE);
                     BufferedOutputStream bos = new BufferedOutputStream(getOutputStream(), BUFFER_SIZE)) {
                    if (restartOffset > 0 && restartOffset < fileLength) {
                        bis.skip(restartOffset);
                    }
                    
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    long lastLogTime = startTime;
                    
                    while ((bytesRead = bis.read(buffer)) != -1) {
                        // 处理ASCII模式下的换行符转换
                        byte[] processed = processAsciiBuffer(buffer, bytesRead);
                        bos.write(processed);
                        totalBytesSent += bytesRead;
                        
                        // 大文件每5秒记录一次进度
                        long currentTime = System.currentTimeMillis();
                        if (fileLength > LARGE_FILE_THRESHOLD && currentTime - lastLogTime > 5000) {
                            logger.info("File transfer progress: " + file.getName() + " " + 
                                       (totalBytesSent * 100 / fileLength) + "%", "DataConnection", ip);
                            lastLogTime = currentTime;
                        }
                    }
                    bos.flush();
                }
            } else {
                // 二进制模式：使用FileChannel + BufferedOutputStream，避免不必要的数组拷贝
                try (FileChannel fileChannel = FileChannel.open(file.toPath(), java.nio.file.StandardOpenOption.READ);
                     BufferedOutputStream bos = new BufferedOutputStream(getOutputStream(), BUFFER_SIZE)) {
                    
                    if (restartOffset > 0 && restartOffset < fileLength) {
                        fileChannel.position(restartOffset);
                    }
                    
                    ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
                    long lastLogTime = startTime;
                    
                    while (fileChannel.read(buffer) != -1) {
                        buffer.flip();
                        // 直接使用ByteBuffer的array()或get()方法
                        if (buffer.hasArray()) {
                            bos.write(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
                        } else {
                            // 对于直接缓冲区，使用get方法
                            byte[] byteArray = new byte[buffer.remaining()];
                            buffer.get(byteArray);
                            bos.write(byteArray);
                        }
                        totalBytesSent += buffer.limit();
                        buffer.clear();
                        
                        // 大文件每5秒记录一次进度
                        long currentTime = System.currentTimeMillis();
                        if (fileLength > LARGE_FILE_THRESHOLD && currentTime - lastLogTime > 5000) {
                            logger.info("File transfer progress: " + file.getName() + " " + 
                                       (totalBytesSent * 100 / fileLength) + "%", "DataConnection", ip);
                            lastLogTime = currentTime;
                        }
                    }
                    bos.flush();
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            double speed = duration > 0 ? (totalBytesSent / 1024.0 / 1024.0) / (duration / 1000.0) : 0;
            
            if (restartOffset == 0 && totalBytesSent != fileLength) {
                logger.warn("File transfer incomplete: expected " + fileLength + " bytes, sent " + totalBytesSent, "DataConnection", ip);
                throw new DataConnectionException("File transfer incomplete: expected " + fileLength + " bytes, sent " + totalBytesSent,
                                                DataConnectionException.ErrorType.TRANSFER_ERROR);
            }

            logger.info(String.format("File transfer completed: %s (%d bytes, %.2f MB/s)", 
                                     file.getName(), totalBytesSent, speed), "DataConnection", ip);
        } catch (IOException e) {
            logger.error("File transfer failed: " + e.getMessage(), "DataConnection", ip);
            throw new DataConnectionException("File transfer failed", DataConnectionException.ErrorType.TRANSFER_ERROR, e);
        }
    }
    
    /**
     * 处理ASCII模式下的缓冲区，转换换行符
     */
    private byte[] processAsciiBuffer(byte[] buffer, int length) {
        // 检查是否需要转换
        boolean needsConversion = false;
        for (int i = 0; i < length; i++) {
            if (buffer[i] == '\n' || buffer[i] == '\r') {
                needsConversion = true;
                break;
            }
        }
        
        if (!needsConversion) {
            return buffer;
        }
        
        // 转换换行符为CRLF
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(length + 100);
        for (int i = 0; i < length; i++) {
            if (buffer[i] == '\n') {
                // 检查前一个字符是否是CR
                if (i == 0 || buffer[i - 1] != '\r') {
                    baos.write('\r');
                }
                baos.write('\n');
            } else if (buffer[i] == '\r') {
                baos.write('\r');
                // 检查下一个字符是否是LF
                if (i + 1 < length && buffer[i + 1] == '\n') {
                    baos.write('\n');
                    i++; // 跳过下一个LF
                } else {
                    baos.write('\n');
                }
            } else {
                baos.write(buffer[i]);
            }
        }
        return baos.toByteArray();
    }

    /**
     * 接收文件
     * @param file 要接收的文件
     * @throws DataConnectionException 数据连接异常
     */
    public void receiveFile(File file) throws DataConnectionException {
        String ip = socket != null && socket.getInetAddress() != null ? socket.getInetAddress().getHostAddress() : null;
        
        try {
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new DataConnectionException("Cannot create parent directory", DataConnectionException.ErrorType.RESOURCE_ERROR);
                }
            }
            
            boolean append = restartOffset > 0 && file.exists() && file.length() >= restartOffset;
            long totalBytesReceived = append ? restartOffset : 0;
            
            if (append) {
                logger.info("Resuming file reception: " + file.getName() + " at offset " + restartOffset, "DataConnection", ip);
            } else {
                logger.info("Starting file reception: " + file.getName(), "DataConnection", ip);
            }

            if (asciiMode) {
                try (FileOutputStream fos = new FileOutputStream(file, append);
                     InputStream is = getInputStream()) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                        totalBytesReceived += bytesRead;
                    }
                }
            } else {
                try (FileChannel fileChannel = FileChannel.open(file.toPath(), 
                         java.nio.file.StandardOpenOption.CREATE,
                         java.nio.file.StandardOpenOption.WRITE);
                     InputStream is = getInputStream()) {
                    
                    if (append && restartOffset > 0) {
                        fileChannel.position(restartOffset);
                    }
                    
                    ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
                    byte[] byteArray = new byte[BUFFER_SIZE];
                    int bytesRead;
                    
                    while ((bytesRead = is.read(byteArray)) != -1) {
                        buffer.clear();
                        buffer.put(byteArray, 0, bytesRead);
                        buffer.flip();
                        
                        while (buffer.hasRemaining()) {
                            fileChannel.write(buffer);
                        }
                        totalBytesReceived += bytesRead;
                    }
                }
            }

            logger.info("File reception completed: " + file.getName() + " (" + totalBytesReceived + " bytes)", "DataConnection", ip);
        } catch (IOException e) {
            logger.error("File reception failed: " + e.getMessage(), "DataConnection", ip);
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
            String ip = socket != null && socket.getInetAddress() != null ? socket.getInetAddress().getHostAddress() : null;
            logger.info("Sending directory listing", "DataConnection", ip);
            os.write(listing.getBytes("UTF-8"));
            os.flush();
            logger.info("Directory listing sent successfully", "DataConnection", ip);
        } catch (IOException e) {
            String ip = socket != null && socket.getInetAddress() != null ? socket.getInetAddress().getHostAddress() : null;
            logger.error("Failed to send directory listing: " + e.getMessage(), "DataConnection", ip);
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
            String ip = socket != null && socket.getInetAddress() != null ? socket.getInetAddress().getHostAddress() : null;
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.warn("Error closing input stream: " + e.getMessage(), "DataConnection", ip);
                } finally {
                    inputStream = null;
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    logger.warn("Error closing output stream: " + e.getMessage(), "DataConnection", ip);
                } finally {
                    outputStream = null;
                }
            }
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    logger.warn("Error closing socket: " + e.getMessage(), "DataConnection", ip);
                } finally {
                    socket = null;
                }
            }
            logger.info("Data connection closed", "DataConnection", ip);
        } catch (Exception e) {
            String ip = socket != null && socket.getInetAddress() != null ? socket.getInetAddress().getHostAddress() : null;
            logger.error("Error closing data connection: " + e.getMessage(), "DataConnection", ip);
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
