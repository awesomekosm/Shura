package com.bots.shura.audio;

import com.bots.shura.db.entities.Track;
import com.bots.shura.db.repositories.TrackRepository;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@Component
public class TrackScheduler extends AudioEventAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrackScheduler.class);

    private final TrackPlayer trackPlayer;
    private final TrackRepository trackRepository;
    private final Queue<LoadedTrack> trackQueue = new LinkedList<>();
    private LoadedTrack currentTrack;
    private boolean playing = false;

    public TrackScheduler(@Lazy TrackPlayer trackPlayer, TrackRepository trackRepository) {
        this.trackPlayer = trackPlayer;
        this.trackRepository = trackRepository;
    }

    public static class LoadedTrack {
        public AudioTrack getAudio() {
            return audio;
        }

        public void setAudio(AudioTrack audio) {
            this.audio = audio;
        }

        public TrackOrigin getOrigin() {
            return origin;
        }

        public void setOrigin(TrackOrigin origin) {
            this.origin = origin;
        }

        public String getPlaylistName() {
            return playlistName;
        }

        public void setPlaylistName(String playlistName) {
            this.playlistName = playlistName;
        }

        private AudioTrack audio;
        private TrackOrigin origin;
        private String playlistName;
    }

    @Override
    public void onPlayerPause(AudioPlayer player) {
        LOGGER.info("Player was paused");
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        LOGGER.info("Player was resumed");
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        // A track started playing
        currentTrack = trackQueue.remove();
        playing = true;
    }

    public void deleteFirstTrackByName(String name) {
        List<Track> dbTrack = trackRepository.findAllByName(name);
        if (dbTrack != null && dbTrack.size() > 0) {
            trackRepository.delete(dbTrack.get(0));
        }
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        playing = false;
        deleteFirstTrackByName(track.getInfo().title);
        while (trackPlayer.getSkipCount() > 0) {
            LoadedTrack loadedTrack = trackQueue.poll();
            if (loadedTrack != null) {
                deleteFirstTrackByName(loadedTrack.getAudio().getInfo().title);
            }
            trackPlayer.setSkipCount(trackPlayer.getSkipCount() - 1);
        }

        nextTrack(player);
    }

    public void nextTrack(AudioPlayer player) {
        if (!trackQueue.isEmpty()) {
            player.playTrack(trackQueue.peek().getAudio());
        }

    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        // An already playing track threw an exception (track end event will still be received separately)
        LOGGER.error("Playing track threw an exception", exception);
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        LOGGER.error("Track is stuck {} {}", track.getInfo().title, track.getInfo().uri);
        // Audio track has been unable to provide us any audio, might want to just start a new track
        nextTrack(player);
    }

    public void queue(AudioTrack track, TrackOrigin origin, String playlistName) {
        var loadedTrack = new LoadedTrack();
        loadedTrack.setAudio(track);
        loadedTrack.setOrigin(origin);
        loadedTrack.setPlaylistName(playlistName);

        trackQueue.add(loadedTrack);
        if (!playing) {
            trackPlayer.getAudioPlayer().playTrack(track);
            playing = true;
        }
    }

    public void skipPlaylist() {
        if (currentTrack != null && currentTrack.getOrigin().equals(TrackOrigin.PLAYLIST)) {
            LoadedTrack lt = trackQueue.peek();
            if (lt != null && lt.getOrigin().equals(TrackOrigin.PLAYLIST) && currentTrack.getPlaylistName().equals(lt.getPlaylistName())) {
                while (!trackQueue.isEmpty() && lt != null && currentTrack.getPlaylistName().equals(lt.getPlaylistName())) {
                    final AudioTrack audio = lt.getAudio();
                    final AudioTrackInfo info = (audio == null ? null : audio.getInfo());
                    List<Track> dbSkipTracks = trackRepository.findAllByName((info == null ? null : info.title));
                    if (dbSkipTracks.size() > 0 && dbSkipTracks.get(0).getPlaylistName().equals(lt.getPlaylistName())) {
                        trackRepository.delete(dbSkipTracks.get(0));
                    }

                    trackQueue.remove();
                    lt = trackQueue.peek();
                }

                trackPlayer.getAudioPlayer().stopTrack();
            }
        }
    }
}
