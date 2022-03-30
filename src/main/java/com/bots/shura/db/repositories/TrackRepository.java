package com.bots.shura.db.repositories;

import com.bots.shura.audio.TrackOrigin;
import com.bots.shura.db.entities.Track;
import com.bots.shura.db.entities.TrackPlayStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Component
public class TrackRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public TrackRepository(DataSource dataSource) {
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public List<Track> findAllQueuedOrPlayingOrPausedByGuildId(long guildId) {
        final String sql = "select * from track where guild_id = :guildId and (play_status = 0 or play_status = 2 or play_status = 3) order by id";
        final Map<String, ?> map = Map.of("guildId", guildId);

        return namedParameterJdbcTemplate.query(sql, map, new TrackRowMapper());
    }

    public List<Track> findAllNotSkippedOrFinishedByNameAndGuildId(String name, long guildId) {
        final String sql = "select * from track where guild_id = :guildId and name = :name and not (play_status = 1 or play_status = 4) order by id";
        final Map<String, ?> map = Map.of(
                "name", name,
                "guildId", guildId
        );

        return namedParameterJdbcTemplate.query(sql, map, new TrackRowMapper());
    }

    public boolean save(Track track) {
        final String sql = "insert into track (guild_id, link, name, origin, playlist_name, time, play_status) values (:guildId, :link, :name, :origin, :playlistName, :time, :playStatus);";
        final Map<String, ?> map = Map.of(
                "guildId", track.getGuildId(),
                "link", track.getLink(),
                "name", track.getName(),
                "origin", track.getOrigin(),
                "playlistName", track.getPlaylistName(),
                "time", track.getTime(),
                "playStatus", track.getPlayStatus().status()
        );

        return namedParameterJdbcTemplate.update(sql, map) == 1;
    }

    public boolean updateTrackStatus(Track track, TrackPlayStatus trackPlayStatus) {
        track.setPlayStatus(trackPlayStatus);
        return namedParameterJdbcTemplate.update("update track set play_status = :playStatus where id = :trackId;",
                Map.of("playStatus", trackPlayStatus.status(),
                        "trackId", track.getId())
        ) > 0;
    }

    public static class TrackRowMapper implements RowMapper<Track> {

        @Override
        public Track mapRow(ResultSet resultSet, int rowNum) throws SQLException {
            Track track = new Track();
            track.setId(resultSet.getLong("id"));
            track.setGuildId(resultSet.getLong("guild_id"));
            track.setName(resultSet.getString("name"));
            track.setLink(resultSet.getString("link"));
            track.setPlaylistName(resultSet.getString("playlist_name"));
            track.setOrigin(TrackOrigin.valueOf(resultSet.getString("origin")));
            track.setTime(resultSet.getShort("time"));
            track.setPlayStatus(TrackPlayStatus.values()[resultSet.getShort("play_status")]);

            return track;
        }
    }
}
