package com.ftpserver.data;

import java.io.*;
import java.net.*;

public abstract class DataConnection implements AutoCloseable {
    protected Socket socket;
    protected InputStream inputStream;
    protected OutputStream outputStream;
    protected boolean asciiMode;

    public DataConnection() {
        this.asciiMode = false;
    }

    public void setAsciiMode(boolean asciiMode) {
        this.asciiMode = asciiMode;
    }

    public boolean isAsciiMode() {
        return asciiMode;
    }

    public abstract void connect() throws IOException;

    public InputStream getInputStream() throws IOException {
        if (inputStream == null) {
            inputStream = socket.getInputStream();
            if (asciiMode) {
                inputStream = new AsciiInputStream(inputStream);
            }
        }
        return inputStream;
    }

    public OutputStream getOutputStream() throws IOException {
        if (outputStream == null) {
            outputStream = socket.getOutputStream();
            if (asciiMode) {
                outputStream = new AsciiOutputStream(outputStream);
            }
        }
        return outputStream;
    }

    public void sendFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        }
    }

    public void receiveFile(File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             InputStream is = getInputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
    }

    public void sendListing(String listing) throws IOException {
        try (OutputStream os = getOutputStream()) {
            os.write(listing.getBytes("UTF-8"));
            os.flush();
        }
    }

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
            inputStream = null;
        }
        if (outputStream != null) {
            outputStream.close();
            outputStream = null;
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
            socket = null;
        }
    }

    private static class AsciiInputStream extends FilterInputStream {
        private static final byte CR = 13;
        private static final byte LF = 10;
        private boolean lastWasCR = false;

        protected AsciiInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (lastWasCR && b == LF) {
                lastWasCR = false;
                return super.read();
            }
            lastWasCR = (b == CR);
            return b;
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

    private static class AsciiOutputStream extends FilterOutputStream {
        private static final byte CR = 13;
        private static final byte LF = 10;

        protected AsciiOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            if (b == LF) {
                out.write(CR);
                out.write(LF);
            } else if (b == CR) {
                out.write(CR);
                out.write(LF);
            } else {
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
