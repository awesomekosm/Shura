package com.bots.shura;

import net.dv8tion.jda.api.JDABuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.security.auth.login.LoginException;

/**
 * 3222528 permission integer <br>
 * https://discordapp.com/oauth2/authorize?client_id=169479851839848449&permissions=3222528&scope=bot
 */
@SpringBootApplication
public class ShuraApplication {

    public static void main(String[] args) throws LoginException {
        ConfigurableApplicationContext cac = SpringApplication.run(ShuraApplication.class, args);

        JDABuilder client = cac.getBean(JDABuilder.class);
        client.build(); // login
    }
}
