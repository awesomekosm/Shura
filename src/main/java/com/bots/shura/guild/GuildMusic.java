package com.bots.shura.guild;

import com.bots.shura.audio.AudioLoader;
import com.bots.shura.audio.LavaPlayerAudioProvider;
import com.bots.shura.audio.TrackPlayer;
import com.bots.shura.audio.TrackScheduler;
import com.bots.shura.caching.Downloader;
import com.bots.shura.caching.YoutubeUrlCorrection;
import com.bots.shura.db.entities.Track;
import com.bots.shura.db.repositories.TrackRepository;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class GuildMusic {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuildMusic.class);

    private VoiceChannel voiceChannel;

    private final AudioLoader audioLoader;

    private final TrackPlayer trackPlayer;

    private final TrackScheduler trackScheduler;

    private final LavaPlayerAudioProvider audioProvider;

    private final AudioPlayerManager audioPlayerManager;

    private final TrackRepository trackRepository;

    private final Downloader downloader;

    private final YoutubeUrlCorrection youtubeUrlCorrection;

    public GuildMusic(VoiceChannel voiceChannel, TrackRepository trackRepository, Downloader downloader) {
        this.voiceChannel = voiceChannel;
        this.trackRepository = trackRepository;
        this.downloader = downloader;
        this.audioPlayerManager = playerManager();
        this.youtubeUrlCorrection = new YoutubeUrlCorrection();
        {
            // Create an AudioPlayer so Discord4J can receive audio data
            final AudioPlayer audioPlayer = audioPlayerManager.createPlayer();
            {
                this.trackPlayer = new TrackPlayer(this.voiceChannel.getGuild().getIdLong(), audioPlayer);
                this.trackScheduler = new TrackScheduler(trackPlayer, trackRepository);
                {
                    audioPlayer.addListener(trackScheduler);
                    audioPlayer.setVolume(20);
                }
            }

            this.audioLoader = new AudioLoader(trackScheduler, trackRepository);
            this.audioProvider = new LavaPlayerAudioProvider(audioPlayer);
        }

        connectToVoiceChannel(voiceChannel, audioProvider);
        recoverOnStartup();
    }

    private void connectToVoiceChannel(VoiceChannel channel, LavaPlayerAudioProvider audioProvider) {
        Guild guild = channel.getGuild();
        // Get an audio manager for this guild, this will be created upon first use for each guild
        AudioManager audioManager = guild.getAudioManager();
        // The order of the following instructions does not matter!
        // Set the sending handler to our echo system
        audioManager.setSendingHandler(audioProvider);
        // Connect to the voice channel
        audioManager.openAudioConnection(channel);
    }

    private AudioPlayerManager playerManager() {
        // Creates AudioPlayer instances and translates URLs to AudioTrack instances
        final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        playerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.HIGH);
        playerManager.enableGcMonitoring();
        playerManager.setFrameBufferDuration((int) TimeUnit.SECONDS.toMillis(20));
        // Give 10 seconds to connect before timing out
        playerManager.setHttpRequestConfigurator(requestConfig ->
                RequestConfig.copy(requestConfig).setConnectTimeout(10000).build());
        // Allow playerManager to parse remote sources like YouTube links
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
        return playerManager;
    }

    public void pause() {
        trackPlayer.getAudioPlayer().setPaused(true);
    }

    public void play(String command) {
        String correctedUrl = youtubeUrlCorrection.correctUrl(command);
        if (downloader != null) {
            checkCacheAndLoad(correctedUrl);
        } else {
            audioPlayerManager.loadItem(correctedUrl, audioLoader);
        }
    }

    public void leave() {
        voiceChannel.getGuild().getAudioManager().closeAudioConnection();
    }

    public void resume() {
        trackPlayer.getAudioPlayer().setPaused(false);
    }

    public void volume(int volume) {
        trackPlayer.getAudioPlayer().setVolume(volume);
    }

    public void skip(int skipNum) {
        if (skipNum > 1) {
            //skipping current - end event in scheduler will deduct the rest
            trackPlayer.setSkipCount(skipNum - 1);
        }
        trackPlayer.getAudioPlayer().stopTrack();
    }

    public void skipPlaylist() {
        trackScheduler.skipPlaylist();
    }

    public void recoverOnStartup() {
        // check if player didn't finish playing tracks from previous shutdown/crash
        List<Track> unPlayedTracks = trackRepository.findAllByGuildId(this.voiceChannel.getGuild().getIdLong());
        Optional.of(unPlayedTracks).ifPresent(tracks -> {
            if (tracks.size() > 0) {
                // todo substitute source with cache if exists and cache enabled
                // prevent saving duplicates to db upon restart - probably a better way to do this without
                // blocking on loadItem
                audioLoader.setReloadingTracks(true);
                tracks.forEach(track -> {
                    try {
                        audioPlayerManager.loadItem(track.getLink(), audioLoader).get();
                    } catch (InterruptedException | ExecutionException e) {
                        LOGGER.error("Startup recovery failed", e);
                    }
                });
                audioLoader.setReloadingTracks(false);
            }
        });
    }

    /**
     * Switch voice channel same guild without losing audio
     *
     * @param voiceChannel new voice channel
     */
    public void reconnectVoiceChannel(VoiceChannel voiceChannel) {
        connectToVoiceChannel(voiceChannel, audioProvider);
        this.voiceChannel = voiceChannel;
    }

    private void checkCacheAndLoad(String url) {
        // check for playlist in cache
        List<String> playlistSongPaths = downloader.getPlaylistSongs(url);
        if (!playlistSongPaths.isEmpty()) {
            // sync playlist, it may have new songs
            try {
                LOGGER.debug("Syncing playlist...");
                try {
                    downloader.playlist(url).get(30, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    LOGGER.error("Syncing playlist error", e);
                }
                playlistSongPaths = downloader.getPlaylistSongs(url);
            } catch (Downloader.YoutubeDLException e) {
                LOGGER.error("Could not sync playlist to cache", e);
            }
            // load songs
            LOGGER.debug("Loading playlist from cache {}", playlistSongPaths);
            playlistSongPaths.forEach(playlistSongPath -> {
                try {
                    audioPlayerManager.loadItem(playlistSongPath, audioLoader).get();
                } catch (InterruptedException | ExecutionException e) {
                    LOGGER.error("Blocking item loading failed", e);
                }
            });
            return;
        }

        // check for single in cache
        String singleCachedPath = downloader.getSingleSong(url);
        if (StringUtils.isNotBlank(singleCachedPath)) {
            LOGGER.debug("Loading single from cache {}", singleCachedPath);
            audioPlayerManager.loadItem(singleCachedPath, audioLoader);
            return;
        }

        // missing from cache, load from source
        LOGGER.debug("Cache miss {}", url);
        audioPlayerManager.loadItem(url, audioLoader);
        // cache for next time
        try {
            LOGGER.debug("Cache miss, syncing {}", url);
            downloader.playlistOrSingle(url);
        } catch (Downloader.YoutubeDLException e) {
            LOGGER.error("Could not cache " + url, e);
        }
    }
}
