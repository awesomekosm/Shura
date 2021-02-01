package com.bots.shura;

import com.bots.shura.commands.CommandProcessor;
import com.bots.shura.db.TrackRepository;
import org.flywaydb.core.Flyway;

import javax.security.auth.login.LoginException;

/**
 * 3222528 permission integer <br>
 * https://discord.com/oauth2/authorize?client_id=169479851839848449&permissions=3222528&scope=bot
 */
public class ShuraApplication {

    public static void main(String[] args) throws LoginException, InterruptedException {
        // load properties and migrate db
        ShuraProperties shuraProperties = new ShuraProperties().load();
        migrateDatabase(shuraProperties.getDataSourceProperties());
        // common object initialization
        Config config = new Config();

        // login and block
        config.discordClient(shuraProperties,
                new CommandProcessor(new TrackRepository(shuraProperties.getDataSourceProperties())),
                config.commandAliases())
                .build()
                .awaitReady();
    }

    private static void migrateDatabase(ShuraProperties.DataSourceProperties datasourceProperties) {
        // Create the Flyway instance and point it to the database
        Flyway flyway = Flyway.configure().dataSource(datasourceProperties.getUrl(), "sa", null).load();

        // Start the migration
        flyway.migrate();
    }
}
