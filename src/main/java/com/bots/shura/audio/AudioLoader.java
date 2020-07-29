package com.bots.shura.audio;

import com.bots.shura.db.entities.Track;
import com.bots.shura.db.repositories.TrackRepository;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AudioLoader implements AudioLoadResultHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AudioLoader.class);
    private final TrackScheduler trackScheduler;
    private final TrackRepository trackRepository;
    private boolean reloadingTracks;

    public AudioLoader(TrackScheduler trackScheduler, TrackRepository trackRepository) {
        this.trackScheduler = trackScheduler;
        this.trackRepository = trackRepository;
    }

    public void saveTrack(AudioTrack track, TrackOrigin trackOrigin, String playlistName) {
        if (!reloadingTracks) {
            Track repositoryTrack = new Track();
            repositoryTrack.setName(track.getInfo().title);
            repositoryTrack.setLink(track.getInfo().uri);
            repositoryTrack.setPlaylistName(playlistName);
            repositoryTrack.setOrigin(trackOrigin);
            repositoryTrack.setTime(0);

            trackRepository.save(repositoryTrack);
        }
    }

    @Override
    public void trackLoaded(final AudioTrack track) {
        // LavaPlayer found an audio source for us to play
        trackScheduler.queue(track, TrackOrigin.SINGLE, "");
        saveTrack(track, TrackOrigin.SINGLE, "");
    }

    @Override
    public void playlistLoaded(final AudioPlaylist playlist) {
        // LavaPlayer found multiple AudioTracks from some playlist
        for (AudioTrack track : playlist.getTracks()) {
            trackScheduler.queue(track, TrackOrigin.PLAYLIST, playlist.getName());
            saveTrack(track, TrackOrigin.PLAYLIST, playlist.getName());
        }

    }

    @Override
    public void noMatches() {
        LOGGER.error("LavaPlayer did not find any audio to extract");
    }

    @Override
    public void loadFailed(final FriendlyException exception) {
        LOGGER.error("LavaPlayer could not parse an audio source for some reason", exception);
    }

    public void setReloadingTracks(boolean reloadingTracks) {
        this.reloadingTracks = reloadingTracks;
    }
}
