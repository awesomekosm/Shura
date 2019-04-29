package com.bots.shura.audio

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

    @Override
    public void trackLoaded(final AudioTrack track) {
        // LavaPlayer found an audio source for us to play
        trackScheduler.queue(track)
    }

    @Override
    public void playlistLoaded(final AudioPlaylist playlist) {
        // LavaPlayer found multiple AudioTracks from some playlist
        for (AudioTrack track : playlist.getTracks()) {
            trackScheduler.queue(track)
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