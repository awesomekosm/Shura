package com.bots.shura.commands

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent


interface Command {
    void execute(GuildMessageReceivedEvent event);
}