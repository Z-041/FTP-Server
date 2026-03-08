package com.ftpserver.config;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class ConfigWatcher {
    private final String configPath;
    private WatchService watchService;
    private WatchKey watchKey;
    private Thread watchThread;
    private volatile boolean running;
    private final List<ConfigChangeListener> listeners;

    public ConfigWatcher(String configPath) {
        this.configPath = configPath;
        this.listeners = new ArrayList<>();
    }

    public void start() throws IOException {
        if (running) {
            return;
        }

        Path path = Paths.get(configPath);
        Path parentDir = path.getParent();
        if (parentDir == null) {
            throw new IOException("Invalid config path: " + configPath);
        }

        watchService = FileSystems.getDefault().newWatchService();
        parentDir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

        running = true;
        watchThread = new Thread(this::watchDirectory, "ConfigWatcher");
        watchThread.setDaemon(true);
        watchThread.start();
    }

    public void stop() {
        if (!running) {
            return;
        }

        running = false;
        if (watchThread != null) {
            watchThread.interrupt();
        }
        if (watchKey != null) {
            watchKey.cancel();
        }
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (IOException e) {
        }
    }

    private void watchDirectory() {
        Path configFileName = Paths.get(configPath).getFileName();
        
        while (running) {
            try {
                WatchKey key = watchService.take();
                
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();
                    
                    if (filename.equals(configFileName)) {
                        notifyConfigChanged();
                    }
                }
                
                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            } catch (InterruptedException e) {
                if (running) {
                }
                break;
            }
        }
    }

    private void notifyConfigChanged() {
        for (ConfigChangeListener listener : listeners) {
            listener.onConfigChanged(configPath);
        }
    }

    public void addListener(ConfigChangeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ConfigChangeListener listener) {
        listeners.remove(listener);
    }

    public interface ConfigChangeListener {
        void onConfigChanged(String configPath);
    }
}
