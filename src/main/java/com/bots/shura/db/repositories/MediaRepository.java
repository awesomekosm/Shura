package com.bots.shura.db.repositories;

import com.bots.shura.db.entities.Media;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MediaRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public MediaRepository(DataSource dataSource) {
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public Media getNonFinishedByGuildId(long guildId) {
        final String sql = "select * from media where guild_id = :guildId and finish_time IS NULL order by id limit 1";
        final Map<String, ?> map = Map.of("guildId", guildId);

        var result = namedParameterJdbcTemplate.query(sql, map, new MediaRepository.MediaRowMapper());
        if (!result.isEmpty()) {
            return result.get(0);
        } else {
            return null;
        }
    }

    public Media getCurrentMedia(long guildId) {
        final String sql = "select * from media m join current_media cm where m.id = cm.media_id and cm.guild_id = :guildId";
        final Map<String, ?> map = Map.of("guildId", guildId);

        var result = namedParameterJdbcTemplate.query(sql, map, new MediaRepository.MediaRowMapper());
        if (!result.isEmpty()) {
            return result.get(0);
        } else {
            return null;
        }
    }

    public void removeCurrentMedia(long guildId) {
        final String sql = "delete from current_media where guild_id = :guildId";
        namedParameterJdbcTemplate.update(sql, Map.of("guildId", guildId));
    }

    public boolean setCurrentMedia(long guildId, long mediaId) {
        Media result = getCurrentMedia(guildId);
        if (result == null) {
            final String sql = "insert into current_media (guild_id, media_id) values (:guildId, :mediaId);";
            final Map<String, Object> map = new HashMap<>();
            map.put("guildId", guildId);
            map.put("mediaId", mediaId);

            return namedParameterJdbcTemplate.update(sql, map) == 1;
        } else {
            final String sql = "update current_media set media_id = :mediaId where guild_id = :guildId;";
            return namedParameterJdbcTemplate.update(sql,
                    Map.of("guildId", guildId,
                            "mediaId", mediaId)
            ) > 0;
        }
    }

    public boolean updateMediaStartTime(Long mediaId, LocalDateTime startTime) {
        final String sql = "update media set start_time = :startTime where id = :mediaId;";
        return namedParameterJdbcTemplate.update(sql,
                Map.of("startTime", startTime,
                        "mediaId", mediaId)
        ) > 0;
    }

    public boolean updateMediaFinishTime(Long mediaId, LocalDateTime finishTime) {
        final String sql = "update media set finish_time = :finishTime where id = :mediaId;";
        return namedParameterJdbcTemplate.update(sql,
                Map.of("finishTime", finishTime,
                        "mediaId", mediaId)
        ) > 0;
    }

    public List<Media> getCurrentPlaylist(long guildId) {
        final String sql = """
                select playlistMedias.id,
                       playlistMedias.guild_id,
                       playlistMedias.name,
                       playlistMedias.artist,
                       playlistMedias.album,
                       playlistMedias.link,
                       playlistMedias.guid,
                       playlistMedias.source,
                       playlistMedias.request_guid,
                       playlistMedias.request_time,
                       playlistMedias.start_time,
                       playlistMedias.finish_time
                from media as playlistMedias
                         join (select source, request_guid
                               from media as m
                                        join current_media as cm
                               where m.id = cm.media_id
                                 and cm.guild_id = :guildId) currentMedia
                where playlistMedias.source = currentMedia.source
                  and playlistMedias.request_guid = currentMedia.request_guid
                  and playlistMedias.finish_time is null
                order by id;
                """;
        final Map<String, ?> map = Map.of("guildId", guildId);

        return namedParameterJdbcTemplate.query(sql, map, new MediaRepository.MediaRowMapper());
    }

    public boolean skipMedia(long guildId, int totalToSkip) {
        final String sql = """
                update media
                set finish_time = :finishTime
                where id in (select id
                             from media as playlistMedias
                                      join (select source, request_guid
                                            from media as m
                                                     join current_media as cm
                                            where m.id = cm.media_id
                                              and cm.guild_id = :guildId)
                             where finish_time is null
                             limit :skip);
                """;
        return namedParameterJdbcTemplate.update(sql,
                Map.of("finishTime", LocalDateTime.now(),
                        "skip", totalToSkip,
                        "guildId", guildId)
        ) > 0;
    }

    public boolean skipCurrentPlaylist(long guildId) {
        final String sql = """
                update media
                set finish_time = :finishTime
                where id in (select id
                             from media as playlistMedias
                                      join (select source, request_guid
                                            from media as m
                                                     join current_media as cm
                                            where m.id = cm.media_id
                                              and cm.guild_id = :guildId) currentMedia
                             where playlistMedias.source = currentMedia.source
                               and playlistMedias.request_guid = currentMedia.request_guid
                               and finish_time is null);
                """;
        return namedParameterJdbcTemplate.update(sql,
                Map.of("finishTime", LocalDateTime.now(),
                        "guildId", guildId)
        ) > 0;
    }

    public boolean save(Media media) {
        final String sql = "insert into media (guild_id, name, artist, album, link, guid, source, request_guid, request_time, finish_time) values (:guildId, :name, :artist, :album, :link, :guid, :source, :requestGuid, :requestTime, :finishTime);";
        final Map<String, Object> map = new HashMap<>();
        map.put("guildId", media.getGuildId());
        map.put("name", media.getName());
        map.put("artist", media.getArtist());
        map.put("album", media.getAlbum());
        map.put("link", media.getLink());
        map.put("guid", media.getGuid());
        map.put("source", media.getSource());
        map.put("requestGuid", media.getRequestGuid());
        map.put("requestTime", media.getRequestTime());
        map.put("startTime", media.getStartTime());
        map.put("finishTime", media.getFinishTime());

        return namedParameterJdbcTemplate.update(sql, map) == 1;
    }

    public static class MediaRowMapper implements RowMapper<Media> {

        @Override
        public Media mapRow(ResultSet resultSet, int rowNum) throws SQLException {
            Media media = new Media();
            media.setId(resultSet.getLong("id"));
            media.setGuildId(resultSet.getLong("guild_id"));
            media.setName(resultSet.getString("name"));
            media.setArtist(resultSet.getString("artist"));
            media.setAlbum(resultSet.getString("album"));
            media.setLink(resultSet.getString("link"));
            media.setGuid(resultSet.getString("guid"));
            media.setSource(resultSet.getString("source"));
            media.setRequestGuid(resultSet.getString("request_guid"));
            media.setRequestTime(LocalDateTime.parse(resultSet.getString("request_time")));
            final String startTimeString = resultSet.getString("start_time");
            if (startTimeString != null) {
                media.setFinishTime(LocalDateTime.parse(startTimeString));
            }
            final String finishTimeString = resultSet.getString("finish_time");
            if (finishTimeString != null) {
                media.setFinishTime(LocalDateTime.parse(finishTimeString));
            }

            return media;
        }
    }
}
