package com.bots.shura.shurapleer;

import com.bots.shura.audio.AudioLoader;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Shurapleer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Shurapleer.class);

    private final ShurapleerClient shurapleerClient;
    private final AudioPlayerManager audioPlayerManager;
    private final AudioLoader audioLoader;

    public Shurapleer(ShurapleerClient shurapleerClient,
                      AudioPlayerManager audioPlayerManager,
                      AudioLoader audioLoader) {
        this.shurapleerClient = shurapleerClient;
        this.audioPlayerManager = audioPlayerManager;
        this.audioLoader = audioLoader;
    }

    public void loadTracks(String url) {
        boolean isPlaylist = StringUtils.contains(url, "playlist");

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(url);
        UriComponents uriComponents = uriBuilder.build();

        if (isPlaylist) {
            String playlistId = uriComponents.getPathSegments().get(uriComponents.getPathSegments().size() - 1);

            List<ShurapleerClient.MediaLocation> mediaLocations = shurapleerClient.getPlaylistMediaLocations(playlistId);

            for (var ml : mediaLocations) {
                try {
                    if (Path.of(ml.getLocalUri()).toFile().exists()) {
                        audioPlayerManager.loadItem(ml.getLocalUri(), audioLoader).get();
                    } else {
                        final String mediaUrl = UriComponentsBuilder
                                .fromUriString(shurapleerClient.getBasePath())
                                .pathSegment("api", "files", ml.getPublicId())
                                .build().toUriString();
                        audioPlayerManager.loadItem(mediaUrl, audioLoader).get();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    LOGGER.error("Blocking loading of track: " + ml.getLocalUri() + " name: " + ml.getName(), e);
                }
            }
        }
    }
}