package com.bots.shura;

import com.bots.shura.audio.LavaPlayerAudioProvider;
import com.bots.shura.audio.TrackPlayer;
import com.bots.shura.audio.TrackScheduler;
import com.bots.shura.commands.Command;
import com.bots.shura.commands.CommandProcessor;
import com.bots.shura.commands.CommandProcessor.CommandName;
import com.bots.shura.commands.Utils;
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.http.client.config.RequestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
class Config {
    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

    @Bean
    DataSource shuraDataSource(@Value("${shura.datasource.url}") String url,
                               @Value("${shura.datasource.driver}") String driver) {
        return DataSourceBuilder.create().url(url).driverClassName(driver).build();
    }

    @Bean
    AudioPlayerManager playerManager() {
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

    @Bean
    TrackPlayer trackPlayer(AudioPlayerManager playerManager, TrackScheduler trackScheduler) {
        // Create an AudioPlayer so Discord4J can receive audio data
        AudioPlayer player = playerManager.createPlayer();
        player.addListener(trackScheduler);
        player.setVolume(20);

        TrackPlayer trackPlayer = new TrackPlayer();
        trackPlayer.setAudioPlayer(player);

        return trackPlayer;
    }

    @Bean
    LavaPlayerAudioProvider audioProvider(TrackPlayer trackPlayer) {
        return new LavaPlayerAudioProvider(trackPlayer.getAudioPlayer());
    }

    @Bean
    JDABuilder discordClient(@Value("${discord.token}") String token,
                             @Value("${shura.drunk-mode}") boolean drunkMode,
                             @Value("${shura.thresh-hold}") int threshHold,
                             CommandProcessor commandProcessor,
                             Map<CommandName, List<String>> commandAliases) {
        JDABuilder client = JDABuilder.createDefault(token)
                .setAudioSendFactory(new NativeAudioSendFactory())
                .addEventListeners(new ListenerAdapter() {
                    @Override
                    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
                        final String content = StringUtils.trimToEmpty(event.getMessage().getContentRaw());
                        if (StringUtils.isNoneBlank(content)) {
                            if (drunkMode) {
                                List<String> input = Utils.parseCommands(content, 2);
                                CommandName cmd = bestFitCommand(commandAliases, input.get(0).toUpperCase(), threshHold);
                                if (cmd != null) {
                                    commandProcessor.getCommandMap().get(cmd).execute(event);
                                }
                            } else {
                                for (final Map.Entry<CommandName, Command> entry : commandProcessor.getCommandMap().entrySet()) {
                                    if (content.startsWith('!' + entry.getKey().name())) {
                                        entry.getValue().execute(event);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                });

        commandProcessor.recoverOnStartup();

        return client;
    }

    private final LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

    @Bean
    Map<CommandName, List<String>> commandAliases() {
        Map<CommandName, List<String>> commandAliases = new HashMap<>();
        commandAliases.put(CommandName.PLAY, List.of("PLAY", "ПЛЕЙ", "ИГРАТЬ"));
        commandAliases.put(CommandName.SUMMON, List.of("SUMMON", "СУММОН", "ВЫЗЫВАТЬ"));
        commandAliases.put(CommandName.LEAVE, List.of("LEAVE", "ЛИВ", "ПОКИНУТЬ"));
        commandAliases.put(CommandName.PAUSE, List.of("PAUSE", "ПАУЗ"));
        commandAliases.put(CommandName.RESUME, List.of("RESUME", "РЕЗЮМ", "ПРОДОЛЖИТЬ"));
        commandAliases.put(CommandName.SKIP, List.of("SKIP", "СКИП", "ПРОПУСК"));
        commandAliases.put(CommandName.VOLUME, List.of("VOLUME", "ВОЛЬЮМ", "ГРОМКОСТЬ"));

        return commandAliases;
    }

    private CommandName bestFitCommand(Map<CommandName, List<String>> commandAliases, String userInput, int threshHold) {
        CommandName result = null;
        int currentDistance = threshHold;
        for (CommandName val : CommandName.values()) {
            for (String alias : commandAliases.get(val)) {
                int distance = levenshteinDistance.apply('!' + alias, userInput);
                if (distance < currentDistance) {
                    result = val;
                    currentDistance = distance;
                }
            }
        }
        if (result != null)
            LOGGER.info("Deducted command {}", result.name());

        return result;
    }

}
