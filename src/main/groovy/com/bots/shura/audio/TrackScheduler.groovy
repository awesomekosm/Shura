package com.bots.shura.audio

import com.bots.shura.db.entities.Track
import com.bots.shura.db.repositories.TrackRepository
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Component
class TrackScheduler extends AudioEventAdapter {

    static Logger LOGGER = LoggerFactory.getLogger(TrackScheduler)

    @Autowired
    @Lazy
    TrackPlayer trackPlayer

    @Autowired
    TrackRepository  trackRepository

    class LoadedTrack {
        AudioTrack audio
        TrackOrigin origin
        String playlistName
    }

    Queue<LoadedTrack> trackQueue = new LinkedList<>()
    LoadedTrack currentTrack
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
        currentTrack = trackQueue.remove()
        playing = true
    }

    public void deleteFirstTrackByName(String name){
        List<Track> dbTrack = trackRepository.findAllByName(name)
        if(dbTrack != null && dbTrack.size() > 0)
            trackRepository.delete(dbTrack[0])
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        playing = false
        String trackName = track?.info?.title

        deleteFirstTrackByName(trackName)
        while (trackPlayer.skipCount > 0) {
            trackPlayer.skipCount--
            try {
                trackName = trackQueue.remove().audio.info.title
                deleteFirstTrackByName(trackName)
            } catch (NoSuchElementException ex) {}
        }

        nextTrack(player)
    }

    public void nextTrack(AudioPlayer player){
        if(!trackQueue.isEmpty()){
            player.playTrack(trackQueue.peek().audio)
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

    public void queue(AudioTrack track, TrackOrigin origin, String playlistName) {
        trackQueue.add(new LoadedTrack(
                audio: track,
                origin: origin,
                playlistName: playlistName
        ))
        if (!playing) {
            trackPlayer.audioPlayer.playTrack(track)
            playing = true
        }
    }

    public void skipPlaylist() {
        if (currentTrack != null && currentTrack.origin == TrackOrigin.PLAYLIST) {
            LoadedTrack lt = trackQueue.peek()
            if (lt != null && lt.origin == TrackOrigin.PLAYLIST && currentTrack.playlistName == lt.playlistName) {
                while(!trackQueue.isEmpty() && lt != null && currentTrack.playlistName == lt.playlistName){
                    def dbSkipTracks = trackRepository.findAllByName(lt.audio?.info?.title)
                    if(dbSkipTracks.size() > 0 && dbSkipTracks[0].playlistName == lt.playlistName){
                        trackRepository.delete(dbSkipTracks[0])
                    }

                    trackQueue.remove()
                    lt = trackQueue.peek()
                }
                trackPlayer.audioPlayer.stopTrack()
            }
        }
    }
}
