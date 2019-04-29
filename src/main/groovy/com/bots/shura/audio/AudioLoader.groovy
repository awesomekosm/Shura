package com.bots.shura.audio

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AudioLoader implements AudioLoadResultHandler {

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
        // LavaPlayer did not find any audio to extract
    }

    @Override
    public void loadFailed(final FriendlyException exception) {
        // LavaPlayer could not parse an audio source for some reason
    }
}