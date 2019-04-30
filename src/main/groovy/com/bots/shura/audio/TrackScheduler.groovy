package com.bots.shura.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TrackScheduler extends AudioEventAdapter {

    static Logger LOGGER = LoggerFactory.getLogger(TrackScheduler)

    @Autowired
    TrackPlayer trackPlayer

    Queue<AudioTrack> trackQueue = new LinkedList<>()
    boolean playing = false

    @Override
    public void onPlayerPause(AudioPlayer player) {
        LOGGER.info("Player was paused")
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        LOGGER.info("Player was resumed")
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        // A track started playing
        AudioTrack currentTrack = trackQueue.remove()
        playing = true
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        playing = false
        while (trackPlayer.skipCount > 0) {
            trackPlayer.skipCount--
            try {
                trackQueue.remove()
            } catch (NoSuchElementException ex) {}
        }

        nextTrack(player)
    }

    public void nextTrack(AudioPlayer player){
        if(!trackQueue.isEmpty()){
            player.playTrack(trackQueue.peek())
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        // An already playing track threw an exception (track end event will still be received separately)
        LOGGER.error("Playing track threw an exception", exception)
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        LOGGER.error("Track is stuck {} {}", track.getInfo().title, track.getInfo().uri)
        // Audio track has been unable to provide us any audio, might want to just start a new track
        nextTrack(player)
    }

    public void queue(AudioTrack track) {
        trackQueue.add(track)
        if (!playing) {
            trackPlayer.audioPlayer.playTrack(track)
            playing = true
        }
    }
}
