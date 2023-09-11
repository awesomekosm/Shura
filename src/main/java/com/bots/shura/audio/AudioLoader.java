package com.bots.shura.audio;

import com.bots.shura.caching.Downloader;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AudioLoader implements AudioLoadResultHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AudioLoader.class);

    private final AudioPlayer audioPlayer;

    private final MediaAction mediaAction;

    private final AudioPlayerManager audioPlayerManager;

    private final long guildId;

    public AudioLoader(AudioPlayer audioPlayer, MediaAction mediaAction, AudioPlayerManager audioPlayerManager, long guildId) {
        this.audioPlayer = audioPlayer;
        this.mediaAction = mediaAction;
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

    public void setCachedEntriesToLoad(List<Downloader.TrackEntry> cachedEntriesToLoad) {
        // todo remove
    }
}
