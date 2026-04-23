package com.bots.shura.audio;

import com.bots.shura.db.entities.Media;
import com.bots.shura.db.repositories.MediaRepository;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.api.JDA;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;


public class TrackScheduler extends AudioEventAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrackScheduler.class);

    private final TrackPlayer trackPlayer;
    private final MediaAction mediaAction;
    private final AudioLoader audioLoader;
    private final AudioPlayerManager audioPlayerManager;
    private final MediaRepository mediaRepository;
    private final DiscordStatusService discordStatusService;
    private final JDA jda;

    public TrackScheduler(TrackPlayer trackPlayer,
                          MediaAction mediaAction,
                          AudioLoader audioLoader,
                          AudioPlayerManager audioPlayerManager,
                          MediaRepository mediaRepository,
                          DiscordStatusService discordStatusService,
                          JDA jda) {
        this.trackPlayer = trackPlayer;
        this.mediaAction = mediaAction;
        this.audioLoader = audioLoader;
        this.audioPlayerManager = audioPlayerManager;
        this.mediaRepository = mediaRepository;
        this.discordStatusService = discordStatusService;
        this.jda = jda;
    }

    @Override
    public void onPlayerPause(AudioPlayer player) {
        Media currentMedia = mediaRepository.getCurrentMedia(trackPlayer.getGuildId());
        LOGGER.info("Player was paused on {}", currentMedia);
        if (currentMedia != null && StringUtils.isNotBlank(currentMedia.getName())) {
            discordStatusService.setPaused(jda, trackPlayer.getGuildId(), currentMedia.getName());
        }
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        Media currentMedia = mediaRepository.getCurrentMedia(trackPlayer.getGuildId());
        LOGGER.info("Player was resumed on {}", currentMedia);
        if (currentMedia != null && StringUtils.isNotBlank(currentMedia.getName())) {
            discordStatusService.setPlaying(jda, trackPlayer.getGuildId(), currentMedia.getName());
        }
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        Media currentMedia = mediaRepository.getCurrentMedia(trackPlayer.getGuildId());
        LOGGER.info("Player was started on {}", currentMedia);
        if (currentMedia != null) {
            mediaRepository.updateMediaStartTime(currentMedia.getId(), LocalDateTime.now());
        }
        final String trackName = currentMedia != null ? currentMedia.getName() : track.getInfo().title;
        if (StringUtils.isNotBlank(trackName)) {
            if (player.isPaused()) {
                discordStatusService.setPaused(jda, trackPlayer.getGuildId(), trackName);
            } else {
                discordStatusService.setPlaying(jda, trackPlayer.getGuildId(), trackName);
            }
        }
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        Media currentMedia = mediaRepository.getCurrentMedia(trackPlayer.getGuildId());
        LOGGER.info("Player was ended on {}", currentMedia);
        if (currentMedia != null) {
            mediaRepository.updateMediaFinishTime(currentMedia.getId(), LocalDateTime.now());
        }
        discordStatusService.setIdle(jda, trackPlayer.getGuildId());
        mediaAction.nextTrack(audioPlayerManager, audioLoader, trackPlayer.getGuildId());
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        // An already playing track threw an exception (track end event will still be received separately)
        LOGGER.error("Playing track threw an exception", exception);
        mediaAction.skipTrackOnError(audioPlayerManager, audioLoader, trackPlayer.getGuildId());
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        LOGGER.error("Track is stuck");
        mediaAction.skipTrackOnError(audioPlayerManager, audioLoader, trackPlayer.getGuildId());
    }

    public void skipPlaylist() {
        mediaRepository.skipCurrentPlaylist(trackPlayer.getGuildId());
        trackPlayer.getAudioPlayer().stopTrack();
    }

    public void skip(int skipNumber) {
        if (skipNumber > 0) {
            mediaRepository.skipMedia(trackPlayer.getGuildId(), skipNumber);
            trackPlayer.getAudioPlayer().stopTrack();
        }
    }

}
