package com.bots.shura;

import com.bots.shura.caching.Downloader;
import com.bots.shura.caching.ShutdownDownloader;
import com.bots.shura.commands.Command;
import com.bots.shura.commands.CommandProcessor;
import com.bots.shura.commands.CommandProcessor.CommandName;
import com.bots.shura.commands.Utils;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class Config {
    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

    @Bean
    @ConditionalOnProperty(name = "shura.cache.enabled")
    public Downloader downloader(@Value("${shura.cache.updated}") boolean updated,
                                 @Value("${shura.cache.directory}") String cacheDirectory) throws Downloader.MissingDependencyException, Downloader.YoutubeDLException {
        Downloader downloader = new Downloader(cacheDirectory);
        if (updated) {
            LOGGER.info("Updating Downloader");
            LOGGER.info("{}", downloader.update());
        }

        return downloader;
    }

    @Bean
    @ConditionalOnProperty(name = "shura.cache.enabled")
    public ShutdownDownloader shutdownDownloader(@Autowired Downloader downloader) {
        return new ShutdownDownloader(downloader);
    }

    @Bean
    public JDABuilder discordClient(ShuraProperties shuraProperties,
                                    CommandProcessor commandProcessor,
                                    Map<CommandName, List<String>> commandAliases) {

        return JDABuilder.createDefault(shuraProperties.getDiscord().getToken())
                .addEventListeners(new ListenerAdapter() {
                    @Override
                    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
                        final String content = StringUtils.trimToEmpty(event.getMessage().getContentRaw());
                        if (StringUtils.isNoneBlank(content)) {
                            if (shuraProperties.isDrunkMode()) {
                                List<String> input = Utils.parseCommands(content, 2);
                                CommandName cmd = bestFitCommand(commandAliases, input.get(0).toUpperCase(), shuraProperties.getThreshHold());
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
    }

    private final LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

    @Bean
    public Map<CommandName, List<String>> commandAliases() {
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
        if (userInput != null && userInput.startsWith("!")) {
            userInput = StringUtils.substring(userInput, 1, userInput.length());
        }
        CommandName result = null;
        int currentDistance = threshHold;
        for (CommandName val : CommandName.values()) {
            for (String alias : commandAliases.get(val)) {
                int distance = levenshteinDistance.apply(alias, userInput);
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
