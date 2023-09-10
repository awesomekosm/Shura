package com.bots.shura.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;

public class TrackPlayer {

    private final AudioPlayer audioPlayer;
    private final long guildId;

    public TrackPlayer(long guildId, AudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
        this.guildId = guildId;
    }

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

    public long getGuildId() {
        return guildId;
    }

}
