package com.bots.shura.commands

import com.bots.shura.audio.LavaPlayerAudioProvider
import com.bots.shura.audio.AudioLoader
import com.bots.shura.audio.TrackScheduler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.object.VoiceState
import discord4j.core.object.entity.Member
import discord4j.core.object.entity.VoiceChannel
import discord4j.voice.VoiceConnection
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct

@Component
class CommandProcessor {
    Map<String, Command> commandMap = new HashMap<>()

    VoiceConnection voiceConnection

    @Autowired
    LavaPlayerAudioProvider audioProvider

    @Autowired
    AudioPlayerManager playerManager

    @Autowired
    AudioLoader audioLoader

    @Autowired
    AudioPlayer audioPlayer

    class Pong implements Command {
        @Override
        void execute(MessageCreateEvent event) {
            event.getMessage().getChannel().block().createMessage("Pong!").block()
        }
    }

    class Play implements Command {

        @Override
        void execute(MessageCreateEvent event) {
            final String content = event.getMessage().getContent().get()
            final List<String> command = Arrays.asList(content.split(" "))
            playerManager.loadItem(command.get(1), audioLoader)
        }
    }

    class Summon implements Command {
        @Override
        void execute(MessageCreateEvent event) {
            final Member member = event.getMember().orElse(null)
            if (member != null) {
                final VoiceState voiceState = member.getVoiceState().block()
                if (voiceState != null) {
                    final VoiceChannel channel = voiceState.getChannel().block()
                    if (channel != null) {
                        if(voiceConnection != null){
                            voiceConnection.disconnect()
                        }
                        voiceConnection = channel.join({ spec -> spec.setProvider(audioProvider) }).block()
                    }
                }
            }
        }
    }

    class Leave implements Command {
        @Override
        void execute(MessageCreateEvent event) {
            if (voiceConnection != null) {
                voiceConnection.disconnect()
            }
        }
    }

    class Pause implements Command {
        @Override
        void execute(MessageCreateEvent event) {
            audioPlayer.setPaused(true)
        }
    }

    class Resume implements Command {
        @Override
        void execute(MessageCreateEvent event) {
            audioPlayer.setPaused(false)
        }
    }

    class Skip implements Command {
        @Override
        void execute(MessageCreateEvent event) {
            audioPlayer.stopTrack()
        }
    }

    @PostConstruct
    public void init(){
        commandMap.put('ping', new Pong())
        commandMap.put('play', new Play())
        commandMap.put('summon', new Summon())
        commandMap.put('leave', new Leave())
        commandMap.put('pause', new Pause())
        commandMap.put('resume', new Resume())
        commandMap.put('skip', new Skip())
    }

    public Map<String, Command> getCommandMap() {
        return commandMap
    }
}
