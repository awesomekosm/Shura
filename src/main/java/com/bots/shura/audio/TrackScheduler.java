package com.bots.shura.audio;

import com.bots.shura.db.entities.Track;
import com.bots.shura.db.entities.TrackPlayStatus;
import com.bots.shura.db.repositories.TrackRepository;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


public class TrackScheduler extends AudioEventAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrackScheduler.class);

    private final TrackPlayer trackPlayer;
    private final TrackRepository trackRepository;
    private final Queue<LoadedTrack> trackQueue = new LinkedList<>();
    private LoadedTrack currentTrack;
    private boolean playing = false;

    public TrackScheduler(TrackPlayer trackPlayer, TrackRepository trackRepository) {
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

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getPlaylistName() {
            return playlistName;
        }

        public void setPlaylistName(String playlistName) {
            this.playlistName = playlistName;
        }

        private AudioTrack audio;
        private TrackOrigin origin;
        private String title;
        private String playlistName;
    }

    @Override
    public void onPlayerPause(AudioPlayer player) {
        var title = currentTrack.getTitle();
        LOGGER.info("Player was paused on {}", title);
        updateTrackByName(title, TrackPlayStatus.PAUSED);
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        var title = currentTrack.getTitle();
        LOGGER.info("Player was resumed on {}", title);
        updateTrackByName(title, TrackPlayStatus.PLAYING);
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        // A track started playing
        currentTrack = trackQueue.remove();

        var title = currentTrack.getTitle();
        LOGGER.info("Player was resumed on {}", title);
        updateTrackByName(title, TrackPlayStatus.PLAYING);

        playing = true;
    }

    private void updateTrackByName(String name, TrackPlayStatus trackPlayStatus) {
        List<Track> dbTrack = trackRepository.findAllNotSkippedOrFinishedByNameAndGuildId(name, trackPlayer.getGuildId());
        if (dbTrack != null && dbTrack.size() > 0) {
            trackRepository.updateTrackStatus(dbTrack.get(0), trackPlayStatus);
        }
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        playing = false;
        TrackPlayStatus trackPlayStatus = TrackPlayStatus.FINISHED;
        switch (endReason) {
            case FINISHED:
            case CLEANUP:
                break;
            case LOAD_FAILED:
                trackPlayStatus = TrackPlayStatus.LOAD_FAILED;
                break;
            case STOPPED:
            case REPLACED:
                trackPlayStatus = TrackPlayStatus.SKIPPED;
                break;
        }
        updateTrackByName(currentTrack.getTitle(), trackPlayStatus);
        while (trackPlayer.getSkipCount() > 0) {
            LoadedTrack loadedTrack = trackQueue.poll();
            if (loadedTrack != null) {
                updateTrackByName(loadedTrack.getTitle(), TrackPlayStatus.SKIPPED);
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
        LOGGER.error("Track is stuck {} {}", currentTrack.getTitle(), track.getInfo().uri);
        // Audio track has been unable to provide us any audio, might want to just start a new track
        nextTrack(player);
    }

    public void queue(AudioTrack track, TrackOrigin origin, String title, String playlistName) {
        var loadedTrack = new LoadedTrack();
        loadedTrack.setAudio(track);
        loadedTrack.setOrigin(origin);
        loadedTrack.setTitle(title);
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
            // the very last track, just stop it
            if (lt == null) {
                trackPlayer.getAudioPlayer().stopTrack();
                return;
            }
            if (lt.getOrigin().equals(TrackOrigin.PLAYLIST) && currentTrack.getPlaylistName().equals(lt.getPlaylistName())) {
                while (!trackQueue.isEmpty() && lt != null && currentTrack.getPlaylistName().equals(lt.getPlaylistName())) {
                    List<Track> dbSkipTracks = trackRepository.findAllNotSkippedOrFinishedByNameAndGuildId(lt.getTitle(), trackPlayer.getGuildId());
                    if (dbSkipTracks.size() > 0 && dbSkipTracks.get(0).getPlaylistName().equals(lt.getPlaylistName())) {
                        trackRepository.updateTrackStatus(dbSkipTracks.get(0), TrackPlayStatus.SKIPPED);
                    }
                    trackQueue.remove();
                    lt = trackQueue.peek();
                }
                trackPlayer.getAudioPlayer().stopTrack();
            }
        }
    }

    public TrackPlayer getTrackPlayer() {
        return trackPlayer;

    }
}
