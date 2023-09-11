package com.bots.shura.audio;

import com.bots.shura.db.entities.Media;
import com.bots.shura.db.repositories.MediaRepository;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.file.Path;
import java.time.LocalDateTime;

@Component
public class MediaAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaAction.class);

    @Value("${shura.shurapleer.url}")
    private String shurapleerUrl;

    final MediaRepository mediaRepository;

    public MediaAction(MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    public void skipTrackOnError(AudioPlayerManager audioPlayerManager, AudioLoader audioLoader, long guildId) {
        Media currentMedia = mediaRepository.getCurrentMedia(guildId);
        LOGGER.error("Couldn't load {}",  currentMedia);
        // Audio track has been unable to provide us any audio, might want to just start a new track
        mediaRepository.updateMediaFinishTime(currentMedia.getId(), LocalDateTime.now());
        nextTrack(audioPlayerManager, audioLoader, guildId);
    }

    public void nextTrack(AudioPlayerManager audioPlayerManager, AudioLoader audioLoader, long guildId) {
        Media media = mediaRepository.getNonFinishedByGuildId(guildId);
        if (media != null) {
            mediaRepository.setCurrentMedia(guildId, media.getId());
            try {
                loadMedia(audioPlayerManager, audioLoader, media);
            } catch (Exception e) {
                LOGGER.error("Failed loading of media id: " + media.getId() + " link: " + media.getLink() + " name: " + media.getName(), e);
                mediaRepository.updateMediaFinishTime(media.getId(), LocalDateTime.now());
                nextTrack(audioPlayerManager, audioLoader, guildId);
            }
        } else {
            mediaRepository.removeCurrentMedia(guildId);
        }
    }

    public void loadMedia(AudioPlayerManager audioPlayerManager, AudioLoader audioLoader, Media media) {
        boolean shurapleer = StringUtils.contains(media.getSource(), "shurapleer");
        if (shurapleer) {
            if (Path.of(media.getLink()).toFile().exists()) {
                audioPlayerManager.loadItem(media.getLink(), audioLoader);
            } else {
                final String mediaUrl = UriComponentsBuilder
                        .fromUriString(shurapleerUrl)
                        .pathSegment("api", "files", media.getGuid())
                        .build().toUriString();
                audioPlayerManager.loadItem(mediaUrl, audioLoader);
            }
        }
    }
}
