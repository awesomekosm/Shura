package com.bots.shura.commands;

import com.bots.shura.audio.MediaAction;
import com.bots.shura.audio.DiscordStatusService;
import com.bots.shura.caching.Downloader;
import com.bots.shura.db.repositories.MediaRepository;
import com.bots.shura.db.entities.Media;
import com.bots.shura.guild.GuildMusic;
import com.bots.shura.shurapleer.ShurapleerClient;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class CommandProcessor {
    public static final String CONTROL_PREFIX = "ctl:";
    public static final String CONTROL_PREVIOUS = CONTROL_PREFIX + "previous";
    public static final String CONTROL_LEAVE = CONTROL_PREFIX + "leave";
    public static final String CONTROL_PAUSE = CONTROL_PREFIX + "pause";
    public static final String CONTROL_SKIP = CONTROL_PREFIX + "skip";
    public static final String CONTROL_SKIP_PL = CONTROL_PREFIX + "skippl";
    public static final String CONTROL_VOL_DOWN = CONTROL_PREFIX + "voldown";
    public static final String CONTROL_VOL_UP = CONTROL_PREFIX + "volup";
    private static final int VOLUME_STEP = 10;
    private static final int MIN_VOLUME = 0;
    private static final int MAX_VOLUME = 150;

    private final Map<Long, GuildMusic> guildMusicConnections = new HashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandProcessor.class);

    public enum CommandName {
        PLAY,
        SUMMON,
        LEAVE,
        PAUSE,
        RESUME,
        SKIP,
        VOLUME,
        PREVIOUS
    }

    private final Map<CommandName, Command> commandMap = new HashMap<>();

    private final MediaRepository mediaRepository;
    private final Downloader downloader;
    private final MediaAction mediaAction;
    private final DiscordStatusService discordStatusService;
    private final ShurapleerClient shurapleerClient;

    public CommandProcessor(MediaAction mediaAction,
                            MediaRepository mediaRepository,
                            DiscordStatusService discordStatusService,
                            @Autowired(required = false) ShurapleerClient shurapleerClient,
                            @Autowired(required = false) Downloader downloader) {

        this.mediaAction = mediaAction;
        this.mediaRepository = mediaRepository;
        this.discordStatusService = discordStatusService;
        this.downloader = downloader;
        this.shurapleerClient = shurapleerClient;

        commandMap.put(CommandName.PLAY, new Play());
        commandMap.put(CommandName.SUMMON, new Summon());
        commandMap.put(CommandName.LEAVE, new Leave());
        commandMap.put(CommandName.PAUSE, new Pause());
        commandMap.put(CommandName.RESUME, new Resume());
        commandMap.put(CommandName.SKIP, new Skip());
        commandMap.put(CommandName.VOLUME, new Volume());
        commandMap.put(CommandName.PREVIOUS, new Previous());
    }

    public Map<CommandName, Command> getCommandMap() {
        return commandMap;
    }

    public void sendSummonControls(MessageChannel channel) {
        if (!(channel instanceof GuildMessageChannel gmc)) {
            return;
        }
        long guildId = gmc.getGuild().getIdLong();
        gmc.sendMessage("**Shura controls**\nIdle")
                .setComponents(controlActionRows(guildId))
                .queue(m -> discordStatusService.registerControlPanel(guildId, m.getChannel().getIdLong(), m.getIdLong()));
    }

    public List<ActionRow> controlActionRows(long guildId) {
        return discordStatusService.controlActionRows(guildId);
    }

    public void handleControlButton(ButtonInteractionEvent event) {
        final String componentId = event.getComponentId();
        final Guild guild = event.getGuild();
        if (guild == null) {
            event.deferEdit().queue();
            return;
        }

        event.deferEdit().queue();
        final long guildId = guild.getIdLong();
        switch (componentId) {
            case CONTROL_PREVIOUS -> executePrevious(guildId);
            case CONTROL_LEAVE -> executeLeave(guildId);
            case CONTROL_PAUSE -> {
                if (isGuildPaused(guildId)) {
                    executeResume(guildId);
                } else {
                    executePause(guildId);
                }
            }
            case CONTROL_SKIP -> executeSkip(guildId, Optional.empty());
            case CONTROL_SKIP_PL -> executeSkip(guildId, Optional.of("pl"));
            case CONTROL_VOL_DOWN -> executeVolumeDelta(guildId, -VOLUME_STEP);
            case CONTROL_VOL_UP -> executeVolumeDelta(guildId, VOLUME_STEP);
            default -> LOGGER.debug("Unknown control button {}", componentId);
        }
    }

    private boolean executeSummon(Member member) {
        if (member != null) {
            final GuildVoiceState voiceState = member.getVoiceState();
            if (voiceState != null && voiceState.getChannel() != null) {
                final VoiceChannel channel = voiceState.getChannel().asVoiceChannel();
                final long guildId = channel.getGuild().getIdLong();
                final GuildMusic guildMusic = guildMusicConnections.get(guildId);
                if (guildMusic == null) {
                    guildMusicConnections.put(guildId, new GuildMusic(channel, mediaRepository, downloader, shurapleerClient, mediaAction, discordStatusService));
                } else {
                    guildMusic.reconnectVoiceChannel(channel);
                }
                return true;
            }
        }
        return false;
    }

    private void executeLeave(long guildId) {
        safeGuildOperation(guildId, GuildMusic::leave);
        guildMusicConnections.remove(guildId);
    }

    private void executePause(long guildId) {
        safeGuildOperation(guildId, GuildMusic::pause);
    }

    private void executeResume(long guildId) {
        safeGuildOperation(guildId, GuildMusic::resume);
    }

    private void executeVolume(long guildId, int volume) {
        safeGuildOperation(guildId, guildMusic -> guildMusic.volume(volume));
    }

    private void executeVolumeDelta(long guildId, int delta) {
        safeGuildOperation(guildId, guildMusic -> {
            final int current = guildMusic.getVolume();
            final int updated = Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, current + delta));
            guildMusic.volume(updated);
        });
    }

    private void executePrevious(long guildId) {
        final Media previousMedia = mediaRepository.getLastFinishedMedia(guildId);
        if (previousMedia != null) {
            safeGuildOperation(guildId, guildMusic -> guildMusic.replayPrevious(previousMedia));
        }
    }

    private void executeSkip(long guildId, Optional<String> argument) {
        if (argument.isPresent()) {
            final String value = argument.get();
            try {
                int skipNum = Integer.parseInt(value);
                safeGuildOperation(guildId, guildMusic -> guildMusic.skip(skipNum));
            } catch (NumberFormatException ex) {
                if (StringUtils.equals(value, "pl")) {
                    safeGuildOperation(guildId, GuildMusic::skipPlaylist);
                }
            }
        } else {
            safeGuildOperation(guildId, guildMusic -> guildMusic.skip(1));
        }
    }

    public class Play implements Command {
        @Override
        public void execute(MessageReceivedEvent event) {
            List<String> commands = Utils.parseCommands(event.getMessage().getContentRaw(), 2);
            if (commands.size() > 1) {
                safeGuildOperation(event.getGuild().getIdLong(), (guildMusic) -> guildMusic.play(commands.get(1)));
            }
        }
    }

    public class Summon implements Command {
        @Override
        public void execute(MessageReceivedEvent event) {
            if (executeSummon(event.getMember())) {
                sendSummonControls(event.getChannel());
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

    private boolean isGuildPaused(long guildId) {
        final GuildMusic guildMusic = guildMusicConnections.get(guildId);
        return guildMusic != null && guildMusic.isPaused();
    }

    public class Leave implements Command {
        @Override
        public void execute(MessageReceivedEvent event) {
            executeLeave(event.getGuild().getIdLong());
        }
    }

    public class Pause implements Command {
        @Override
        public void execute(MessageReceivedEvent event) {
            executePause(event.getGuild().getIdLong());
        }
    }

    public class Resume implements Command {
        @Override
        public void execute(MessageReceivedEvent event) {
            executeResume(event.getGuild().getIdLong());
        }
    }

    public class Volume implements Command {
        @Override
        public void execute(MessageReceivedEvent event) {
            List<String> commands = Utils.parseCommands(event.getMessage().getContentRaw(), 2);
            if (commands.size() > 1) {
                try {
                    executeVolume(event.getGuild().getIdLong(), Integer.parseInt(commands.get(1)));
                } catch (NumberFormatException ex) {
                    LOGGER.error("Failed setting volume", ex);
                }
            }
        }
    }

    public class Skip implements Command {
        @Override
        public void execute(MessageReceivedEvent event) {
            List<String> commands = Utils.parseCommands(event.getMessage().getContentRaw(), 2);
            executeSkip(event.getGuild().getIdLong(), commands.size() > 1 ? Optional.of(commands.get(1)) : Optional.empty());
        }
    }

    public class Previous implements Command {
        @Override
        public void execute(MessageReceivedEvent event) {
            executePrevious(event.getGuild().getIdLong());
        }
    }
}
