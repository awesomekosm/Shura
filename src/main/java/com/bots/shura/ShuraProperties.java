package com.bots.shura;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;

public class ShuraProperties {
    private boolean drunkMode;
    private int threshHold;
    private Discord discord;
    DataSourceProperties dataSourceProperties;

    public static class Discord {
        private String token;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    public static class DataSourceProperties {
        private String url;
        private String driver;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getDriver() {
            return driver;
        }

        public void setDriver(String driver) {
            this.driver = driver;
        }
    }

    public boolean isDrunkMode() {
        return drunkMode;
    }

    public void setDrunkMode(boolean drunkMode) {
        this.drunkMode = drunkMode;
    }

    public int getThreshHold() {
        return threshHold;
    }

    public void setThreshHold(int threshHold) {
        this.threshHold = threshHold;
    }

    public Discord getDiscord() {
        return discord;
    }

    public void setDiscord(Discord discord) {
        this.discord = discord;
    }

    public DataSourceProperties getDataSourceProperties() {
        return dataSourceProperties;
    }

    public void setDataSourceProperties(DataSourceProperties dataSourceProperties) {
        this.dataSourceProperties = dataSourceProperties;
    }

    public ShuraProperties load() {
        Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("application.yml");
        ShuraProperties shuraProperties = yaml.load(inputStream);

        String envDiscordToken = System.getenv("DISCORD_TOKEN");
        String propDiscordToken = System.getProperty("discord.token");
        String discordToken = envDiscordToken;
        if (propDiscordToken != null) {
            discordToken = propDiscordToken;
        }
        if (discordToken != null) {
            if (shuraProperties.getDiscord() != null) {
                shuraProperties.getDiscord().setToken(discordToken);
            } else {
                Discord discord = new Discord();
                discord.setToken(discordToken);
            }
        }

        return shuraProperties;
    }
}
