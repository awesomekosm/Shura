package com.bots.shura.guild;

import com.bots.shura.audio.*;
import com.bots.shura.caching.Downloader;
import com.bots.shura.caching.YoutubeUrlCorrection;
import com.bots.shura.db.entities.Media;
import com.bots.shura.db.repositories.MediaRepository;
import com.bots.shura.db.repositories.TrackRepository;
import com.bots.shura.shurapleer.Shurapleer;
import com.bots.shura.shurapleer.ShurapleerClient;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class GuildMusic {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuildMusic.class);

    private VoiceChannel voiceChannel;

    private final AudioLoader audioLoader;

    private final TrackPlayer trackPlayer;

    private final TrackScheduler trackScheduler;

    private final LavaPlayerAudioProvider audioProvider;

    private final AudioPlayerManager audioPlayerManager;

    private final TrackRepository trackRepository;

    private final MediaRepository mediaRepository;

    private final Downloader downloader;

    private final MediaAction mediaAction;

    private final YoutubeUrlCorrection youtubeUrlCorrection;

    private Shurapleer shurapleer;

    public GuildMusic(VoiceChannel voiceChannel, TrackRepository trackRepository, MediaRepository mediaRepository, Downloader downloader, ShurapleerClient shurapleerClient, MediaAction mediaAction) {
        this.voiceChannel = voiceChannel;
        this.trackRepository = trackRepository;
        this.mediaRepository = mediaRepository;
        this.downloader = downloader;
        this.mediaAction = mediaAction;
        this.audioPlayerManager = playerManager();
        this.youtubeUrlCorrection = new YoutubeUrlCorrection();
        {
            // Create an AudioPlayer so Discord4J can receive audio data
            final AudioPlayer audioPlayer = audioPlayerManager.createPlayer();
            this.audioLoader = new AudioLoader(audioPlayer);
            {
                this.trackPlayer = new TrackPlayer(this.voiceChannel.getGuild().getIdLong(), audioPlayer);
                this.trackScheduler = new TrackScheduler(trackPlayer, mediaAction, audioLoader, audioPlayerManager, mediaRepository);
                {
                    audioPlayer.addListener(trackScheduler);
                    audioPlayer.setVolume(20);
                }
            }

            this.audioProvider = new LavaPlayerAudioProvider(audioPlayer);

            if (shurapleerClient != null) {
                this.shurapleer = new Shurapleer(this.voiceChannel.getGuild().getIdLong(), shurapleerClient, audioPlayerManager, audioLoader, mediaRepository);
            }
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
        if (shurapleer != null && StringUtils.contains(command, "shurapleer")) {
            shurapleer.loadTracks(command);
        } else {
            String correctedUrl = youtubeUrlCorrection.correctUrl(command);
            if (downloader != null) {
                checkCacheAndLoad(correctedUrl);
            } else {
                audioPlayerManager.loadItem(correctedUrl, audioLoader);
            }
        }
        Media currentMedia = mediaRepository.getCurrentMedia(trackPlayer.getGuildId());
        if (currentMedia != null) {
            return;
        }
        // first process, then load item but do so from database
        mediaAction.nextTrack(audioPlayerManager, audioLoader, trackPlayer.getGuildId());
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
        trackScheduler.skip(skipNum);
    }

    public void skipPlaylist() {
        trackScheduler.skipPlaylist();
    }

    public void recoverOnStartup() {
        // check if player didn't finish playing tracks from previous shutdown/crash
        mediaAction.nextTrack(audioPlayerManager, audioLoader, trackPlayer.getGuildId());
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
        List<Downloader.TrackEntry> playlistEntries = List.of();
        try {
            playlistEntries = downloader.getPlayListSongsAll(url);
        } catch (InterruptedException | ExecutionException | Downloader.YoutubeDLException e) {
            LOGGER.error("Error compiling a playlist from cache", e);
        }
        if (!playlistEntries.isEmpty()) {
            // sync playlist, it may have new songs
            LOGGER.debug("Syncing playlist...");
            downloader.playlist(url);

            // load songs
            LOGGER.debug("Loading playlist from cache {}", playlistEntries);
            audioLoader.setCachedEntriesToLoad(playlistEntries);
            playlistEntries.forEach(playlistSongPath -> {
                try {
                    audioPlayerManager.loadItem(playlistSongPath.uri, audioLoader).get();
                } catch (InterruptedException | ExecutionException e) {
                    LOGGER.error("Blocking item loading failed", e);
                }
            });
            audioLoader.setCachedEntriesToLoad(null);
            LOGGER.debug("Loading playlist from cache finished");
            return;
        }

        // check for single in cache
        Downloader.TrackEntry singleEntry = downloader.getSingleSong(url);
        if (singleEntry != null) {
            LOGGER.debug("Loading single from cache {}", singleEntry.uri);
            audioLoader.setCachedEntriesToLoad(List.of(singleEntry));
            try {
                audioPlayerManager.loadItem(singleEntry.uri, audioLoader).get();
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error("Blocking item loading failed", e);
            }
            audioLoader.setCachedEntriesToLoad(null);
            return;
        }

        // missing from cache, load from source
        LOGGER.debug("Cache miss {}", url);
        audioPlayerManager.loadItem(url, audioLoader);
        // cache for next time
        try {
            downloader.playlistOrSingle(url);
        } catch (Downloader.YoutubeDLException e) {
            LOGGER.error("Could not cache " + url, e);
        }
    }
}
