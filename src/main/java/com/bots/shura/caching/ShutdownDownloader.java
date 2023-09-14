package com.bots.shura.caching;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShutdownDownloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShutdownDownloader.class);

    private final Downloader downloader;

    public ShutdownDownloader(Downloader downloader) {
        this.downloader = downloader;
    }

    @PreDestroy
    public void onDestroy() {
        LOGGER.info("Shutdown downloader initiated...");
        downloader.shutdown();
        LOGGER.info("Shutdown downloader completed.");
    }
}
