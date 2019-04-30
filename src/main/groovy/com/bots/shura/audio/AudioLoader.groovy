package com.bots.shura.audio

import com.bots.shura.db.entities.Track
import com.bots.shura.db.repositories.TrackRepository
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AudioLoader implements AudioLoadResultHandler {

    static Logger LOGGER = LoggerFactory.getLogger(AudioLoader)

    @Autowired
    TrackScheduler trackScheduler

    @Autowired
    TrackRepository trackRepository

    boolean reloadingTracks

    void saveTrack(AudioTrack track, TrackOrigin trackOrigin){
        if(!reloadingTracks){
            trackRepository.save(new Track(
                    name: track?.info?.title,
                    link: track?.info?.uri,
                    time: 0,
                    origin: trackOrigin
            ))
        }
    }

    @Override
    public void trackLoaded(final AudioTrack track) {
        // LavaPlayer found an audio source for us to play
        trackScheduler.queue(track)
        saveTrack(track, TrackOrigin.SINGLE)
    }

    @Override
    public void playlistLoaded(final AudioPlaylist playlist) {
        // LavaPlayer found multiple AudioTracks from some playlist
        for (AudioTrack track : playlist.getTracks()) {
            trackScheduler.queue(track)
            saveTrack(track, TrackOrigin.PLAYLIST)
        }
    }

    @Override
    public void noMatches() {
        LOGGER.error('LavaPlayer did not find any audio to extract')
    }

    @Override
    public void loadFailed(final FriendlyException exception) {
        LOGGER.error('LavaPlayer could not parse an audio source for some reason', exception)
    }
}