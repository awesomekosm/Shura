package com.bots.shura.commands

import com.bots.shura.audio.AudioLoader
import com.bots.shura.audio.LavaPlayerAudioProvider
import com.bots.shura.audio.TrackPlayer
import com.bots.shura.audio.TrackScheduler
import com.bots.shura.db.repositories.TrackRepository
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.object.VoiceState
import discord4j.core.object.entity.Member
import discord4j.core.object.entity.VoiceChannel
import discord4j.voice.VoiceConnection
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct

@Component
class CommandProcessor {
    Map<CommandName, Command> commandMap = new HashMap<>()

    VoiceConnection voiceConnection
    boolean inVoice = false

    @Autowired
    LavaPlayerAudioProvider audioProvider

    @Autowired
    AudioPlayerManager playerManager

    @Autowired
    AudioLoader audioLoader

    @Autowired
    TrackPlayer trackPlayer

    @Autowired
    TrackRepository trackRepository

    @Autowired
    TrackScheduler trackScheduler

    class Pong implements Command {
        @Override
        void execute(MessageCreateEvent event) {
            event.getMessage().getChannel().block().createMessage("Pong!").block()
        }
    }

    class Play implements Command {
        @Override
        void execute(MessageCreateEvent event) {
            def commands = Utils.parseCommands(event.getMessage().getContent().get(), 2)
            if (commands.size() > 1) {
                playerManager.loadItem(commands.get(1), audioLoader)
            }
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
                        if(voiceConnection != null && inVoice){
                            voiceConnection.disconnect()
                            inVoice = false
                        }
                        voiceConnection = channel.join({ spec -> spec.setProvider(audioProvider) }).block()
                        inVoice = true
                    }
                }
            }
        }
    }

    class Leave implements Command {
        @Override
        void execute(MessageCreateEvent event) {
            if (voiceConnection != null && inVoice) {
                voiceConnection.disconnect()
                inVoice = false
            }
        }
    }

    class Pause implements Command {
        @Override
        void execute(MessageCreateEvent event) {
            trackPlayer.audioPlayer.setPaused(true)
        }
    }

    class Resume implements Command {
        @Override
        void execute(MessageCreateEvent event) {
            trackPlayer.audioPlayer.setPaused(false)
        }
    }

    class Volume implements Command {
        @Override
        void execute(MessageCreateEvent event) {
            def commands = Utils.parseCommands(event.getMessage().getContent().get(), 2)
            if (commands.size() > 1) {
                try {
                    trackPlayer.audioPlayer.setVolume(Integer.parseInt(commands.get(1)))
                } catch (NumberFormatException e) {
                }
            }
        }
    }

    class Skip implements Command {
        @Override
        void execute(MessageCreateEvent event) {
            def commands = Utils.parseCommands(event.getMessage().getContent().get(), 2)
            if (commands.size() > 1) {
                try {
                    def skipNum = Integer.parseInt(commands.get(1))
                    if (skipNum > 1) {
                        //skipping current - end event in scheduler will deduct the rest
                        trackPlayer.skipCount = skipNum - 1
                        trackPlayer.audioPlayer.stopTrack()
                    } else {
                        // skip if 1 or less then, assume bad input and just skip 1 track
                        trackPlayer.audioPlayer.stopTrack()
                    }
                } catch (NumberFormatException ex) {
                    if (StringUtils.equals(commands.get(1), 'pl')) {
                        trackScheduler.skipPlaylist()
                    } else {
                        // assume second argument is bad and just skip 1 track
                        trackPlayer.audioPlayer.stopTrack()
                    }
                }
            } else {
                trackPlayer.audioPlayer.stopTrack()
            }
        }
    }

    @PostConstruct
    public void init(){
        commandMap.put(CommandName.PLAY, new Play())
        commandMap.put(CommandName.SUMMON, new Summon())
        commandMap.put(CommandName.LEAVE, new Leave())
        commandMap.put(CommandName.PAUSE, new Pause())
        commandMap.put(CommandName.RESUME, new Resume())
        commandMap.put(CommandName.SKIP, new Skip())
        commandMap.put(CommandName.VOLUME, new Volume())
    }

    enum CommandName {
        PLAY,
        SUMMON,
        LEAVE,
        PAUSE,
        RESUME,
        SKIP,
        VOLUME,
    }

    public Map<CommandName, Command> getCommandMap() {
        return commandMap
    }

    public void recoverOnStartup(){
        // check if player didn't finish playing tracks from previous shutdown/crash
        def unPlayedTracks = trackRepository.findAll()
        Optional.of(unPlayedTracks).ifPresent({ list ->
            if (list.size() > 0) {
                // prevent saving duplicates to db upon restart - probably a better way to do this without
                // blocking on loadItem
                audioLoader.reloadingTracks = true
                list.stream().forEach({ track ->
                    playerManager.loadItem(track.link, audioLoader).get()
                })
                audioLoader.reloadingTracks = false
            }
        })
    }
}
