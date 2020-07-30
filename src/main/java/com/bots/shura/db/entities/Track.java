package com.bots.shura.db.entities;

import com.bots.shura.audio.TrackOrigin;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class Track {
    @Id
    @GeneratedValue
    private Long id;
    private long guildId;
    private String name;
    private String link;
    private String playlistName;
    private TrackOrigin origin;
    private int time;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getGuildId() {
        return guildId;
    }

    public void setGuildId(long guildId) {
        this.guildId = guildId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getPlaylistName() {
        return playlistName;
    }

    public void setPlaylistName(String playlistName) {
        this.playlistName = playlistName;
    }

    public TrackOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(TrackOrigin origin) {
        this.origin = origin;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }
}
