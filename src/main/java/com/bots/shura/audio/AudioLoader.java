package com.bots.shura.audio;

import com.bots.shura.db.entities.Media;
import com.bots.shura.db.repositories.MediaRepository;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

public class AudioLoader implements AudioLoadResultHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AudioLoader.class);

    private final AudioPlayer audioPlayer;

    private final MediaAction mediaAction;

    private final AudioPlayerManager audioPlayerManager;

    private final MediaRepository mediaRepository;

    private final long guildId;

    public AudioLoader(AudioPlayer audioPlayer,
                       MediaAction mediaAction,
                       MediaRepository mediaRepository,
                       AudioPlayerManager audioPlayerManager,
                       long guildId) {
        this.audioPlayer = audioPlayer;
        this.mediaAction = mediaAction;
        this.mediaRepository = mediaRepository;
        this.audioPlayerManager = audioPlayerManager;
        this.guildId = guildId;
    }

    @Override
    public void trackLoaded(final AudioTrack track) {
        LOGGER.info("Loaded track {}", track.getIdentifier());
        audioPlayer.playTrack(track);
    }

    @Override
    public void playlistLoaded(final AudioPlaylist playlist) {
        LOGGER.info("Loaded playlist {}", playlist);

        Media currentMedia = mediaRepository.getCurrentMedia(guildId);
        // the only way to land in playlist loaded is when url is a playlist
        // that media is marked as finished (finished processing it and resulted in AudioPlaylist)
        // and individual tracks are now saved into media
        // then next track is called.
        // the audio player will play on individual media in the `trackLoaded` method
        mediaRepository.updateMediaFinishTime(currentMedia.getId(), LocalDateTime.now());
        for (var at : playlist.getTracks()) {
            Media media = new Media();
            media.setGuildId(guildId);
            media.setName(at.getInfo().title);
            media.setArtist(null);
            media.setAlbum(null);
            media.setLink(at.getInfo().uri);
            media.setGuid(at.getInfo().identifier);
            media.setSource(currentMedia.getSource());
            media.setRequestGuid(currentMedia.getRequestGuid());
            media.setRequestTime(LocalDateTime.now());
            media.setStartTime(null);
            media.setFinishTime(null);

            mediaRepository.save(media);
        }
        mediaAction.nextTrack(audioPlayerManager, this, guildId);
    }

    @Override
    public void noMatches() {
        LOGGER.error("LavaPlayer did not find any audio to extract");
        mediaAction.skipTrackOnError(audioPlayerManager, this, guildId);
    }

    @Override
    public void loadFailed(final FriendlyException exception) {
        LOGGER.error("LavaPlayer could not parse an audio source for some reason", exception);
        mediaAction.skipTrackOnError(audioPlayerManager, this, guildId);
    }
}
