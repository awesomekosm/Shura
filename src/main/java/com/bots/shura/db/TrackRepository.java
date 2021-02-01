package com.bots.shura.db;

import com.bots.shura.ShuraProperties;
import org.javalite.activejdbc.Base;

import java.util.List;

@SuppressWarnings("unchecked")
public class TrackRepository {

    private final ShuraProperties.DataSourceProperties dataSourceProperties;

    public TrackRepository(ShuraProperties.DataSourceProperties dataSourceProperties) {
        this.dataSourceProperties = dataSourceProperties;
    }

    public List<Track> findAllByGuildId(long guildId) {
        return (List<Track>) session(() -> Track.where("guild_id = ?", guildId).load());
    }

    public List<Track> findAllByNameAndGuildId(String name, long guildId) {
        return (List<Track>) session(() -> Track.where("name = ? and guild_id = ?", name, guildId).load());
    }

    public interface Session {
        Object execute();
    }

    public Object session(Session session) {
        Base.open(dataSourceProperties.getDriver(), dataSourceProperties.getUrl(), "sa", null);
        Object result = session.execute();
        Base.close();
        return result;
    }
}
