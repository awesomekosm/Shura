package com.bots.shura.audio;

import com.bots.shura.caching.Downloader;
import com.bots.shura.db.entities.Track;
import com.bots.shura.db.repositories.TrackRepository;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AudioLoader implements AudioLoadResultHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AudioLoader.class);
    private final TrackScheduler trackScheduler;
    private final TrackRepository trackRepository;
    private boolean reloadingTracks;
    private List<Downloader.TrackEntry> cachedEntriesToLoad;

    public AudioLoader(TrackScheduler trackScheduler, TrackRepository trackRepository) {
        this.trackScheduler = trackScheduler;
        this.trackRepository = trackRepository;
    }

    public void saveTrack(AudioTrack track, TrackOrigin trackOrigin, String title, String playlistName) {
        if (!reloadingTracks) {
            Track repositoryTrack = new Track();
            repositoryTrack.setGuildId(trackScheduler.getTrackPlayer().getGuildId());
            repositoryTrack.setName(title);
            repositoryTrack.setLink(track.getInfo().uri);
            repositoryTrack.setPlaylistName(playlistName);
            repositoryTrack.setOrigin(trackOrigin);
            repositoryTrack.setTime(0);

            trackRepository.save(repositoryTrack);
        }
    }

    private Downloader.TrackEntry findTrackEntity(String uri) {
        Downloader.TrackEntry trackEntry = null;
        if (cachedEntriesToLoad != null) {
            trackEntry = cachedEntriesToLoad.stream()
                    .filter(ple -> ple.uri.equals(uri))
                    .findFirst()
                    .orElse(null);
        }
        return trackEntry;
    }

    @Override
    public void trackLoaded(final AudioTrack track) {
        // LavaPlayer found an audio source for us to play
        // cached tracks are loaded one by one, sometimes they are playlists
        //  the loaded track can be part of a playlist
        if (cachedEntriesToLoad != null && !cachedEntriesToLoad.isEmpty()) {
            Downloader.TrackEntry trackEntry = findTrackEntity(track.getInfo().uri);
            String title = track.getInfo().title;
            String playlistName = "";
            if (trackEntry != null) {
                title = trackEntry.title;
                playlistName = trackEntry.playlistName;
            }
            if (StringUtils.isNotBlank(playlistName)) {
                saveTrack(track, TrackOrigin.PLAYLIST, title, playlistName);
                trackScheduler.queue(track, TrackOrigin.PLAYLIST, title, playlistName);
            } else {
                saveTrack(track, TrackOrigin.SINGLE, title, "");
                trackScheduler.queue(track, TrackOrigin.SINGLE, title, "");
            }
        } else {
            saveTrack(track, TrackOrigin.SINGLE, track.getInfo().title, "");
            trackScheduler.queue(track, TrackOrigin.SINGLE, track.getInfo().title,"");
        }
    }

    @Override
    public void playlistLoaded(final AudioPlaylist playlist) {
        // LavaPlayer found multiple AudioTracks from some playlist
        for (AudioTrack track : playlist.getTracks()) {
            trackScheduler.queue(track, TrackOrigin.PLAYLIST, track.getInfo().title, playlist.getName());
            saveTrack(track, TrackOrigin.PLAYLIST, track.getInfo().title, playlist.getName());
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

    public void setCachedEntriesToLoad(List<Downloader.TrackEntry> cachedEntriesToLoad) {
        this.cachedEntriesToLoad = cachedEntriesToLoad;
    }
}
