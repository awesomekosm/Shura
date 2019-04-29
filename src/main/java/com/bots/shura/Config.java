package com.bots.shura;

import com.bots.shura.audio.LavaPlayerAudioProvider;
import com.bots.shura.audio.TrackScheduler;
import com.bots.shura.commands.Command;
import com.bots.shura.commands.CommandProcessor;
import com.bots.shura.db.DBType;
import com.bots.shura.db.DSWrapper;
import com.bots.shura.db.DataSourceRouter;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.message.MessageCreateEvent;
import org.apache.http.client.config.RequestConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
class Config {

    @Bean
    DSWrapper shuraWrapper(@Value("${shura.datasource.url}") String url,
                           @Value("${shura.datasource.driver}") String driver) {
        DataSourceBuilder builder = DataSourceBuilder.create().url(url).driverClassName(driver);
        return new DSWrapper(builder.build());
    }

    @Bean
    @Primary
    DataSource dataSource(@Qualifier("shuraWrapper") DSWrapper shuraWrapper) {
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DBType.SHURA, shuraWrapper.getDataSource());

        DataSourceRouter routingDataSource = new DataSourceRouter();
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(shuraWrapper.getDataSource());

        return routingDataSource;
    }

    @Bean
    AudioPlayerManager playerManager() {
        // Creates AudioPlayer instances and translates URLs to AudioTrack instances
        final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        // Give 10 seconds to connect before timing out
        playerManager.setHttpRequestConfigurator(requestConfig ->
                RequestConfig.copy(requestConfig).setConnectTimeout(10000).build());
        // This is an optimization strategy that Discord4J can utilize.
        playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        // Allow playerManager to parse remote sources like YouTube links
        AudioSourceManagers.registerRemoteSources(playerManager);
        return playerManager;
    }

    @Bean
    AudioPlayer audioPlayer(AudioPlayerManager playerManager, TrackScheduler trackScheduler) {
        // Create an AudioPlayer so Discord4J can receive audio data
        AudioPlayer player = playerManager.createPlayer();
        player.addListener(trackScheduler);

        return player;
    }

    @Bean
    LavaPlayerAudioProvider audioProvider(AudioPlayer audioPlayer) {
        return new LavaPlayerAudioProvider(audioPlayer);
    }

    @Bean
    DiscordClient discordClient(@Value("${discord.token}") String token, CommandProcessor commandProcessor) {
        DiscordClient client = new DiscordClientBuilder(token).build();
        client.getEventDispatcher().on(MessageCreateEvent.class)
                // subscribe is like block, in that it will *request* for action
                // to be done, but instead of blocking the thread, waiting for it
                // to finish, it will just execute the results asynchronously.
                .subscribe(event -> {
                    final String content = event.getMessage().getContent().orElse("");
                    for (final Map.Entry<String, Command> entry : commandProcessor.getCommandMap().entrySet()) {
                        // We will be using ! as our "prefix" to any command in the system.
                        if (content.startsWith('!' + entry.getKey())) {
                            entry.getValue().execute(event);
                            break;
                        }
                    }
                });

        return client;
    }


}
