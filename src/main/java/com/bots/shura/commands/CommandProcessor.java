package com.bots.shura.commands;

import com.bots.shura.audio.AudioLoader;
import com.bots.shura.audio.LavaPlayerAudioProvider;
import com.bots.shura.audio.TrackPlayer;
import com.bots.shura.audio.TrackScheduler;
import com.bots.shura.db.entities.Track;
import com.bots.shura.db.repositories.TrackRepository;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Component
public class CommandProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandProcessor.class);

    public enum CommandName {
        PLAY,
        SUMMON,
        LEAVE,
        PAUSE,
        RESUME,
        SKIP,
        VOLUME
    }

    private final Map<CommandName, Command> commandMap = new HashMap<>();
    private final LavaPlayerAudioProvider audioProvider;
    private final AudioPlayerManager playerManager;
    private final AudioLoader audioLoader;
    private final TrackPlayer trackPlayer;
    private final TrackRepository trackRepository;
    private final TrackScheduler trackScheduler;

    public CommandProcessor(LavaPlayerAudioProvider audioProvider, AudioPlayerManager playerManager, AudioLoader audioLoader, TrackPlayer trackPlayer, TrackRepository trackRepository, TrackScheduler trackScheduler) {
        this.audioProvider = audioProvider;
        this.playerManager = playerManager;
        this.audioLoader = audioLoader;
        this.trackPlayer = trackPlayer;
        this.trackRepository = trackRepository;
        this.trackScheduler = trackScheduler;

        commandMap.put(CommandName.PLAY, new Play());
        commandMap.put(CommandName.SUMMON, new Summon());
        commandMap.put(CommandName.LEAVE, new Leave());
        commandMap.put(CommandName.PAUSE, new Pause());
        commandMap.put(CommandName.RESUME, new Resume());
        commandMap.put(CommandName.SKIP, new Skip());
        commandMap.put(CommandName.VOLUME, new Volume());
    }

    public Map<CommandName, Command> getCommandMap() {
        return commandMap;
    }

    public void recoverOnStartup() {
        // check if player didn't finish playing tracks from previous shutdown/crash
        List<Track> unPlayedTracks = trackRepository.findAll();
        Optional.of(unPlayedTracks).ifPresent(tracks -> {
            if (tracks.size() > 0) {
                // prevent saving duplicates to db upon restart - probably a better way to do this without
                // blocking on loadItem
                audioLoader.setReloadingTracks(true);
                tracks.forEach(track -> {
                    try {
                        playerManager.loadItem(track.getLink(), audioLoader).get();
                    } catch (InterruptedException | ExecutionException e) {
                        LOGGER.error("Startup recovery failed", e);
                    }
                });
                audioLoader.setReloadingTracks(false);
            }
        });
    }

    public class Play implements Command {
        @Override
        public void execute(GuildMessageReceivedEvent event) {
            List<String> commands = Utils.parseCommands(event.getMessage().getContentRaw(), 2);
            if (commands.size() > 1) {
                playerManager.loadItem(commands.get(1), audioLoader);
            }
        }
    }

    public class Summon implements Command {

        private VoiceChannel currentChannel = null;

        @Override
        public void execute(GuildMessageReceivedEvent event) {
            final Member member = event.getMember();
            if (member != null) {
                final GuildVoiceState voiceState = member.getVoiceState();
                if (voiceState != null) {
                    final VoiceChannel channel = voiceState.getChannel();
                    if (channel != null) {
                        // close existing connection
                        if (currentChannel != null) {
                            currentChannel.getGuild().getAudioManager().closeAudioConnection();
                        }
                        currentChannel = channel;
                        connectTo(currentChannel, audioProvider);
                    }
                }
            }
        }

        private void connectTo(VoiceChannel channel, LavaPlayerAudioProvider audioProvider) {
            Guild guild = channel.getGuild();
            // Get an audio manager for this guild, this will be created upon first use for each guild
            AudioManager audioManager = guild.getAudioManager();
            // The order of the following instructions does not matter!
            // Set the sending handler to our echo system
            audioManager.setSendingHandler(audioProvider);
            // Connect to the voice channel
            audioManager.openAudioConnection(channel);
        }
    }

    public static class Leave implements Command {
        @Override
        public void execute(GuildMessageReceivedEvent event) {
            event.getGuild().getAudioManager().closeAudioConnection();
        }
    }

    public class Pause implements Command {
        @Override
        public void execute(GuildMessageReceivedEvent event) {
            trackPlayer.getAudioPlayer().setPaused(true);
        }
    }

    public class Resume implements Command {
        @Override
        public void execute(GuildMessageReceivedEvent event) {
            trackPlayer.getAudioPlayer().setPaused(false);
        }
    }

    public class Volume implements Command {
        @Override
        public void execute(GuildMessageReceivedEvent event) {
            List<String> commands = Utils.parseCommands(event.getMessage().getContentRaw(), 2);
            if (commands.size() > 1) {
                try {
                    trackPlayer.getAudioPlayer().setVolume(Integer.parseInt(commands.get(1)));
                } catch (NumberFormatException ex) {
                    LOGGER.error("Failed setting volume", ex);
                }
            }
        }
    }

    public class Skip implements Command {
        @Override
        public void execute(GuildMessageReceivedEvent event) {
            List<String> commands = Utils.parseCommands(event.getMessage().getContentRaw(), 2);
            if (commands.size() > 1) {
                try {
                    int skipNum = Integer.parseInt(commands.get(1));
                    if (skipNum > 1) {
                        //skipping current - end event in scheduler will deduct the rest
                        trackPlayer.setSkipCount(skipNum - 1);
                    }
                    trackPlayer.getAudioPlayer().stopTrack();

                } catch (NumberFormatException ex) {
                    if (StringUtils.equals(commands.get(1), "pl")) {
                        trackScheduler.skipPlaylist();
                    } else {
                        // assume second argument is bad and just skip 1 track
                        trackPlayer.getAudioPlayer().stopTrack();
                    }

                }

            } else {
                trackPlayer.getAudioPlayer().stopTrack();
            }
        }
    }
}
