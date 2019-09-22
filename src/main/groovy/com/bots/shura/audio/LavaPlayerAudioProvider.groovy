package com.bots.shura.audio

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import net.dv8tion.jda.api.audio.AudioSendHandler

import java.nio.ByteBuffer

class LavaPlayerAudioProvider implements AudioSendHandler {

    private final ByteBuffer buffer
    private final AudioPlayer audioPlayer
    private final MutableAudioFrame frame

    public LavaPlayerAudioProvider(final AudioPlayer audioPlayer) {
        this.buffer = ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize())
        this.audioPlayer = audioPlayer
        this.frame = new MutableAudioFrame()
        this.frame.setBuffer(buffer)
    }

    @Override
    boolean canProvide() {
        return audioPlayer.provide(frame)
    }

    @Override
    ByteBuffer provide20MsAudio() {
        return buffer.flip()
    }

    @Override
    boolean isOpus() {
        return true
    }
}