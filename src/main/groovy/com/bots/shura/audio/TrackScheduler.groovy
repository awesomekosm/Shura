package com.bots.shura.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TrackScheduler extends AudioEventAdapter {

    @Autowired
    AudioPlayer audioPlayer

    Queue<AudioTrack> trackQueue = new LinkedList<>()
    boolean playing = false

    @Override
    public void onPlayerPause(AudioPlayer player) {
        // Player was paused
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        // Player was resumed
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
        nextTrack(player)
        //        if (endReason.mayStartNext && !trackQueue.isEmpty()) {
        //            player.playTrack(trackQueue.peek())
        //            return
        //        }
        //
        //        switch (endReason){
        //            case AudioTrackEndReason.FINISHED:
        //                nextTrack(player)
        //                break
        //            case AudioTrackEndReason.LOAD_FAILED:
        //                nextTrack(player)
        //                break
        //            case AudioTrackEndReason.STOPPED:
        //                nextTrack(player)
        //                break
        //            case AudioTrackEndReason.REPLACED:
        //                break
        //            case AudioTrackEndReason.CLEANUP:
        //                break
        //        }

        // endReason == FINISHED: A track finished or died by an exception (mayStartNext = true).
        // endReason == LOAD_FAILED: Loading of a track failed (mayStartNext = true).
        // endReason == STOPPED: The player was stopped.
        // endReason == REPLACED: Another track started playing while this had not finished
        // endReason == CLEANUP: Player hasn't been queried for a while, if you want you can put a
        //                       clone of this back to your queue
    }

    public void nextTrack(AudioPlayer player){
        if(!trackQueue.isEmpty()){
            player.playTrack(trackQueue.peek())
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        // An already playing track threw an exception (track end event will still be received separately)
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        // Audio track has been unable to provide us any audio, might want to just start a new track
        nextTrack(player)
    }

    public void queue(AudioTrack track) {
        trackQueue.add(track)
        if (!playing) {
            audioPlayer.playTrack(track)
            playing = true
        }
    }
}
