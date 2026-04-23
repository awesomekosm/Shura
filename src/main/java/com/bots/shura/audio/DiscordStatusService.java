package com.bots.shura.audio;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guild-local status: updates the control-panel message for each guild only (no global bot presence).
 */
@Component
public class DiscordStatusService {
    private static final String CONTROL_PREFIX = "ctl:";
    private static final String CONTROL_PREVIOUS = CONTROL_PREFIX + "previous";
    private static final String CONTROL_PAUSE = CONTROL_PREFIX + "pause";
    private static final String CONTROL_SKIP = CONTROL_PREFIX + "skip";
    private static final String CONTROL_SKIP_PL = CONTROL_PREFIX + "skippl";
    private static final String CONTROL_VOL_DOWN = CONTROL_PREFIX + "voldown";
    private static final String CONTROL_VOL_UP = CONTROL_PREFIX + "volup";
    private static final String CONTROL_LEAVE = CONTROL_PREFIX + "leave";

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordStatusService.class);

    private record PanelRef(long channelId, long messageId) {}

    private final Map<Long, PanelRef> guildPanels = new ConcurrentHashMap<>();
    private final Map<Long, Boolean> guildPausedState = new ConcurrentHashMap<>();

    public void registerControlPanel(long guildId, long channelId, long messageId) {
        guildPanels.put(guildId, new PanelRef(channelId, messageId));
        guildPausedState.put(guildId, false);
    }

    public void unregisterGuildPanel(long guildId) {
        guildPanels.remove(guildId);
        guildPausedState.remove(guildId);
    }

    public boolean isPaused(long guildId) {
        return guildPausedState.getOrDefault(guildId, false);
    }

    public void updateGuildStatus(JDA jda, long guildId, String statusLine) {
        PanelRef ref = guildPanels.get(guildId);
        if (ref == null) {
            return;
        }
        MessageChannel channel = resolveChannel(jda, ref.channelId());
        if (channel == null) {
            LOGGER.debug("Could not resolve channel {} for guild {} status update", ref.channelId(), guildId);
            return;
        }
        String body = StringUtils.isBlank(statusLine) ? "—" : statusLine;
        String content = "**Shura controls**\n" + body;
        channel.editMessageById(ref.messageId(), content).queue(
                null,
                error -> LOGGER.debug("Failed to edit control panel for guild {}: {}", guildId, error.toString())
        );
    }

    public void refreshControls(JDA jda, long guildId) {
        PanelRef ref = guildPanels.get(guildId);
        if (ref == null) {
            return;
        }
        MessageChannel channel = resolveChannel(jda, ref.channelId());
        if (channel == null) {
            return;
        }
        channel.editMessageComponentsById(ref.messageId(), controlActionRows(guildId)).queue(
                null,
                error -> LOGGER.debug("Failed to refresh controls for guild {}: {}", guildId, error.toString())
        );
    }

    public void setPlaying(JDA jda, long guildId, String trackName) {
        guildPausedState.put(guildId, false);
        refreshControls(jda, guildId);
        if (StringUtils.isBlank(trackName)) {
            updateGuildStatus(jda, guildId, "Idle");
            return;
        }
        updateGuildStatus(jda, guildId, "Playing: " + trackName);
    }

    public void setPaused(JDA jda, long guildId, String trackName) {
        guildPausedState.put(guildId, true);
        refreshControls(jda, guildId);
        if (StringUtils.isBlank(trackName)) {
            return;
        }
        updateGuildStatus(jda, guildId, "Paused: " + trackName);
    }

    public void setIdle(JDA jda, long guildId) {
        guildPausedState.put(guildId, false);
        refreshControls(jda, guildId);
        updateGuildStatus(jda, guildId, "Idle");
    }

    public List<ActionRow> controlActionRows(long guildId) {
        final boolean paused = isPaused(guildId);
        return List.of(
                ActionRow.of(
                        Button.secondary(CONTROL_PREVIOUS, "Previous"),
                        paused ? Button.success(CONTROL_PAUSE, "Resume") : Button.secondary(CONTROL_PAUSE, "Pause"),
                        Button.secondary(CONTROL_SKIP, "Skip"),
                        Button.secondary(CONTROL_SKIP_PL, "Skip pl")
                ),
                ActionRow.of(
                        Button.secondary(CONTROL_VOL_DOWN, "Vol -"),
                        Button.secondary(CONTROL_VOL_UP, "Vol +"),
                        Button.danger(CONTROL_LEAVE, "Leave")
                )
        );
    }

    private static MessageChannel resolveChannel(JDA jda, long channelId) {
        return jda.getChannelById(MessageChannel.class, channelId);
    }
}
