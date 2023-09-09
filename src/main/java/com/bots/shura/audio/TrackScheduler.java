package com.bots.shura.audio;

import com.bots.shura.db.entities.Media;
import com.bots.shura.db.repositories.MediaRepository;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;


public class TrackScheduler extends AudioEventAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrackScheduler.class);

    private final TrackPlayer trackPlayer;
    private final MediaAction mediaAction;
    private final AudioLoader audioLoader;
    private final AudioPlayerManager audioPlayerManager;
    private final MediaRepository mediaRepository;

    public TrackScheduler(TrackPlayer trackPlayer,
                          MediaAction mediaAction,
                          AudioLoader audioLoader,
                          AudioPlayerManager audioPlayerManager,
                          MediaRepository mediaRepository) {
        this.trackPlayer = trackPlayer;
        this.mediaAction = mediaAction;
        this.audioLoader = audioLoader;
        this.audioPlayerManager = audioPlayerManager;
        this.mediaRepository = mediaRepository;
    }

    @Override
    public void onPlayerPause(AudioPlayer player) {
        Media currentMedia = mediaRepository.getCurrentMedia(trackPlayer.getGuildId());
        LOGGER.info("Player was paused on {}", currentMedia);
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        Media currentMedia = mediaRepository.getCurrentMedia(trackPlayer.getGuildId());
        LOGGER.info("Player was resumed on {}", currentMedia);
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        Media currentMedia = mediaRepository.getCurrentMedia(trackPlayer.getGuildId());
        LOGGER.info("Player was started on {}", currentMedia);
        mediaRepository.updateMediaStartTime(currentMedia.getId(), LocalDateTime.now());
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        Media currentMedia = mediaRepository.getCurrentMedia(trackPlayer.getGuildId());
        LOGGER.info("Player was ended on {}", currentMedia);
        mediaRepository.updateMediaFinishTime(currentMedia.getId(), LocalDateTime.now());
        mediaAction.nextTrack(audioPlayerManager, audioLoader, trackPlayer.getGuildId());
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        // An already playing track threw an exception (track end event will still be received separately)
        LOGGER.error("Playing track threw an exception", exception);
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        Media currentMedia = mediaRepository.getCurrentMedia(trackPlayer.getGuildId());
        LOGGER.error("Track is stuck {}",  currentMedia);
        // Audio track has been unable to provide us any audio, might want to just start a new track
        mediaRepository.updateMediaFinishTime(currentMedia.getId(), LocalDateTime.now());
        mediaAction.nextTrack(audioPlayerManager, audioLoader, trackPlayer.getGuildId());
    }

    public void skipPlaylist() {
//        if (currentTrack != null && currentTrack.getOrigin().equals(TrackOrigin.PLAYLIST)) {
//            LoadedTrack lt = trackQueue.peek();
//            // the very last track, just stop it
//            if (lt == null) {
//                trackPlayer.getAudioPlayer().stopTrack();
//                return;
//            }
//            if (lt.getOrigin().equals(TrackOrigin.PLAYLIST) && currentTrack.getPlaylistName().equals(lt.getPlaylistName())) {
//                while (!trackQueue.isEmpty() && lt != null && currentTrack.getPlaylistName().equals(lt.getPlaylistName())) {
//                    List<Track> dbSkipTracks = trackRepository.findAllNotSkippedOrFinishedByNameAndGuildId(lt.getTitle(), trackPlayer.getGuildId());
//                    if (dbSkipTracks.size() > 0 && dbSkipTracks.get(0).getPlaylistName().equals(lt.getPlaylistName())) {
//                        trackRepository.updateTrackStatus(dbSkipTracks.get(0), TrackPlayStatus.SKIPPED);
//                    }
//                    trackQueue.remove();
//                    lt = trackQueue.peek();
//                }
//                trackPlayer.getAudioPlayer().stopTrack();
//            }
//        }
    }

    public TrackPlayer getTrackPlayer() {
        return trackPlayer;
    }

}
