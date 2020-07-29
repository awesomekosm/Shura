package com.bots.shura.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;

public class TrackPlayer {

    private AudioPlayer audioPlayer = null;
    private int skipCount = 0;

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

    public void setAudioPlayer(AudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
    }

    public int getSkipCount() {
        return skipCount;
    }

    public void setSkipCount(int skipCount) {
        this.skipCount = skipCount;
    }
}
