# Shura
Discord Music Bot

# Use

* Create an application @ https://discordapp.com/developers/applications
* In settings click Bot
* Copy token and add it to application.yml discord:token: YOUR TOKEN
* Get client id of the application
* Link to authorize shura in channels where you can invite
* https://discordapp.com/oauth2/authorize?client_id={YOUR_CLIENT_ID}&permissions=3222528&scope=bot
* Have at least jdk 8
* Execute gradlew on windows or gradlew on linux at the root of the directory
* gradlew.bat bootJar
* Output will be in shura/build/libs/shura-0.0.1-SNAPSHOT.jar
* This is a self contained jar, can be executed
* java -jar shura-0.0.1-SNAPSHOT.jar

# Commands

## PLAY
* Can play anything supported by https://github.com/sedmelluq/lavaplayer
* example
* !play https://www.youtube.com/watch?v=z8pknnncODo
## SUMMON
* calls bot to the same voice channel as the user typing in command
* !summon
## LEAVE
* disconnects from voice
## PAUSE
## RESUME
## SKIP
* can skip a single song
* !skip
* or multiple
* !skip 3
* or a whole playlist that's queued
* !skip pl
## VOLUME
* volume goes from 0 to 1000
* default is
* !volume 20

# Features
* Shura saves all of your inputs and starts where it left off incase it's turned off / crashes
* Shura has drunk mode enabled by default in application.yml, this means you don't have to type commands exactly
* skop pley and !summie and volum will all work as if you typed it correctly

# Thanks
Great libraries that made this fun
* https://github.com/sedmelluq/lavaplayer
* https://github.com/Discord4J/Discord4J
