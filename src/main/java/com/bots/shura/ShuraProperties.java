package com.bots.shura;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "shura")
public class ShuraProperties {

    private DataSourceProperties datasource;
    private boolean drunkMode;
    private int threshHold;
    private Discord discord;

    public static class Discord {
        private String token;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    public DataSourceProperties getDatasource() {
        return datasource;
    }

    public void setDatasource(DataSourceProperties datasource) {
        this.datasource = datasource;
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
}
