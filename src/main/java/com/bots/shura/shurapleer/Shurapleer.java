package com.bots.shura.shurapleer;

import com.bots.shura.audio.AudioLoader;
import com.bots.shura.db.entities.Media;
import com.bots.shura.db.repositories.MediaRepository;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.bots.shura.shurapleer.ShurapleerClient.MediaLocation;

public class Shurapleer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Shurapleer.class);

    private final ShurapleerClient shurapleerClient;
    private final MediaRepository mediaRepository;
    private final long guildId;

    public Shurapleer(long guildId,
                      ShurapleerClient shurapleerClient,
                      AudioPlayerManager audioPlayerManager,
                      AudioLoader audioLoader,
                      MediaRepository mediaRepository) {
        this.guildId = guildId;
        this.shurapleerClient = shurapleerClient;
        this.mediaRepository = mediaRepository;
    }

    public void loadTracks(String url) {
        UUID requestGuid = UUID.randomUUID();
        boolean randomize = StringUtils.startsWith(url, "r");
        if (randomize) {
            url = StringUtils.substringAfter(url, "r");
        }
        boolean isPlaylist = StringUtils.contains(url, "playlist");
        boolean isAccount = StringUtils.contains(url, "account");

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(url);
        UriComponents uriComponents = uriBuilder.build();

        if (isPlaylist) {
            boolean mediaInPlaylist = StringUtils.contains(url, "media");
            String playlistId;
            String mediaId = null;
            if (mediaInPlaylist) {
                playlistId = uriComponents.getPathSegments().get(uriComponents.getPathSegments().size() - 3);
                mediaId = uriComponents.getPathSegments().get(uriComponents.getPathSegments().size() - 1);
            } else {
                playlistId = uriComponents.getPathSegments().get(uriComponents.getPathSegments().size() - 1);
            }

            List<MediaLocation> mediaLocations = shurapleerClient.getPlaylistMediaLocations(playlistId, mediaId);
            if (randomize) {
                Collections.shuffle(mediaLocations);
            }
            for (var ml : mediaLocations) {
                addToMediaRepository(url, requestGuid.toString(), ml);
            }
        } else if (isAccount) {
            String accountId = uriComponents.getPathSegments().get(uriComponents.getPathSegments().size() - 2);
            List<MediaLocation> mediaLocations = shurapleerClient.getAccountMediaLocations(accountId);
            if (randomize) {
                Collections.shuffle(mediaLocations);
            }
            for (var ml : mediaLocations) {
                addToMediaRepository(url, requestGuid.toString(), ml);
            }
        } else {
            String mediaId = uriComponents.getPathSegments().get(uriComponents.getPathSegments().size() - 1);
            MediaLocation mediaLocation = shurapleerClient.getMediaLocation(mediaId);
            if (mediaLocation != null) {
                addToMediaRepository(url, requestGuid.toString(), mediaLocation);
            } else {
                LOGGER.error("Unexpected, {} url returned no media", url);
            }
        }
    }

    public void addToMediaRepository(String source, String requestGuid, MediaLocation ml) {
        Media media = new Media();
        media.setGuildId(guildId);
        media.setName(ml.getName());
        media.setArtist(ml.getArtist());
        media.setAlbum(ml.getAlbum());
        media.setLink(ml.getLocalUri());
        media.setGuid(ml.getPublicId());
        media.setSource(source);
        media.setRequestGuid(requestGuid);
        media.setRequestTime(LocalDateTime.now());
        media.setStartTime(null);
        media.setFinishTime(null);

        mediaRepository.save(media);
    }
}
