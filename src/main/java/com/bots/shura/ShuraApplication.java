package com.bots.shura;

import net.dv8tion.jda.api.JDABuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.security.auth.login.LoginException;

/**
 * 3222528 permission integer <br>
 * https://discord.com/oauth2/authorize?client_id=169479851839848449&permissions=3222528&scope=bot
 */
@SpringBootApplication
public class ShuraApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShuraApplication.class);

    public static void main(String[] args) throws LoginException, InterruptedException {
        final int cores = Runtime.getRuntime().availableProcessors();
        // jdk 17 bug https://github.com/DV8FromTheWorld/JDA/issues/1858
        if (cores <= 1) {
            LOGGER.info("Available Cores {}, setting Parallelism Flag", cores);
            System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "1");
        }

        ConfigurableApplicationContext cac = SpringApplication.run(ShuraApplication.class, args);

        JDABuilder client = cac.getBean(JDABuilder.class);
        client.build().awaitReady(); // login
    }
}
