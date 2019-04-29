package com.bots.shura;

import discord4j.core.DiscordClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 3222528 permission integer <br>
 * https://discordapp.com/oauth2/authorize?client_id=571907899513896992&permissions=3222528&scope=bot
 */
@SpringBootApplication
public class ShuraApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext cac = SpringApplication.run(ShuraApplication.class, args);

        DiscordClient client = cac.getBean(DiscordClient.class);
        client.login().block();
    }
}
