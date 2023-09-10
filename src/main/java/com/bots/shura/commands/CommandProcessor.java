package com.bots.shura.commands;

import com.bots.shura.audio.MediaAction;
import com.bots.shura.caching.Downloader;
import com.bots.shura.db.repositories.MediaRepository;
import com.bots.shura.db.repositories.TrackRepository;
import com.bots.shura.guild.GuildMusic;
import com.bots.shura.shurapleer.ShurapleerClient;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CommandProcessor {

    private final Map<Long, GuildMusic> guildMusicConnections = new HashMap<>();

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

    private final TrackRepository trackRepository;
    private final MediaRepository mediaRepository;
    private final Downloader downloader;
    private final MediaAction mediaAction;
    private final ShurapleerClient shurapleerClient;

    public CommandProcessor(MediaAction mediaAction,
                            TrackRepository trackRepository,
                            MediaRepository mediaRepository,
                            @Autowired(required = false) ShurapleerClient shurapleerClient,
                            @Autowired(required = false) Downloader downloader) {

        this.mediaAction = mediaAction;
        this.trackRepository = trackRepository;
        this.mediaRepository = mediaRepository;
        this.downloader = downloader;
        this.shurapleerClient = shurapleerClient;

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

    public class Play implements Command {
        @Override
        public void execute(GuildMessageReceivedEvent event) {
            List<String> commands = Utils.parseCommands(event.getMessage().getContentRaw(), 2);
            if (commands.size() > 1) {
                safeGuildOperation(event.getGuild().getIdLong(), (guildMusic) -> guildMusic.play(commands.get(1)));
            }
        }
    }

    public class Summon implements Command {
        @Override
        public void execute(GuildMessageReceivedEvent event) {
            final Member member = event.getMember();
            if (member != null) {
                final GuildVoiceState voiceState = member.getVoiceState();
                if (voiceState != null) {
                    final VoiceChannel channel = voiceState.getChannel();
                    if (channel != null) {
                        final long guildId = channel.getGuild().getIdLong();
                        final GuildMusic guildMusic = guildMusicConnections.get(guildId);
                        // if not connected, connect
                        if (guildMusic == null) {
                            guildMusicConnections.put(guildId, new GuildMusic(channel, trackRepository, mediaRepository, downloader, shurapleerClient, mediaAction));
                        } else {
                            safeGuildOperation(guildId, (gm) -> gm.reconnectVoiceChannel(channel));
                        }
                    }
                }
            }
        }
    }

    public interface GuildOperation {
        void execute(GuildMusic guildMusic);
    }

    public void safeGuildOperation(long guildIdLong, GuildOperation guildOperation) {
        final GuildMusic guildMusic = guildMusicConnections.get(guildIdLong);
        if (guildMusic != null) {
            guildOperation.execute(guildMusic);
        }
    }

    public class Leave implements Command {
        @Override
        public void execute(GuildMessageReceivedEvent event) {
            safeGuildOperation(event.getGuild().getIdLong(), (GuildMusic::leave));
            safeGuildOperation(event.getGuild().getIdLong(), (guildMusic -> guildMusicConnections.remove(event.getGuild().getIdLong())));
        }
    }

    public class Pause implements Command {
        @Override
        public void execute(GuildMessageReceivedEvent event) {
            safeGuildOperation(event.getGuild().getIdLong(), (GuildMusic::pause));
        }
    }

    public class Resume implements Command {
        @Override
        public void execute(GuildMessageReceivedEvent event) {
            safeGuildOperation(event.getGuild().getIdLong(), (GuildMusic::resume));
        }
    }

    public class Volume implements Command {
        @Override
        public void execute(GuildMessageReceivedEvent event) {
            List<String> commands = Utils.parseCommands(event.getMessage().getContentRaw(), 2);
            if (commands.size() > 1) {
                try {
                    safeGuildOperation(event.getGuild().getIdLong(), (guildMusic) -> guildMusic.volume(Integer.parseInt(commands.get(1))));
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
                    safeGuildOperation(event.getGuild().getIdLong(), (guildMusic -> guildMusic.skip(skipNum)));
                } catch (NumberFormatException ex) {
                    if (StringUtils.equals(commands.get(1), "pl")) {
                        safeGuildOperation(event.getGuild().getIdLong(), (GuildMusic::skipPlaylist));
                    }
                }
            } else {
                safeGuildOperation(event.getGuild().getIdLong(), (guildMusic -> guildMusic.skip(1)));
            }
        }
    }
}
