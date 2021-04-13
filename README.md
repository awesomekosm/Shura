<h1 align="center">ШУРА</h1>
<p align="center">Discord Music Bot</p>

<p align="center">
  <a href="https://hub.docker.com/r/shurapleer/shura"><img alt="Docker Pulls" src="https://img.shields.io/docker/pulls/shurapleer/shura"></a>
  <a href="https://github.com/Z-EMB/Shura/releases"><img alt="GitHub release (latest SemVer)" src="https://img.shields.io/github/v/release/z-emb/shura"></a>
</p>

### Create Discord Token

* Create an application https://discord.com/developers/applications
* In settings click Bot
* Get client id of the application
* Link to authorize shura in channels where you can invite
    - `https://discord.com/oauth2/authorize?client_id={YOUR_CLIENT_ID}&permissions=3222528&scope=bot`

### Running

Use discord token from the step above.  
Image tags corresponds to the release.  More tags can be found at [Docker Hub](https://hub.docker.com/r/shurapleer/shura)

```
docker run -d \
        --name shura \
        --env JAVA_OPTS="-Dshura.discord.token=$DISCORD_TOKEN" \
        shurapleer/shura:latest
```

### Building

#### Maven Build
* Have at least jdk 11
* Execute `mvnw.cmd` on windows or `mvnw` on linux at the root of the directory
* `./mvnw package`
* Output will be in shura/target/shura-***.jar
* This is a self contained jar, can be executed
* `java -jar -Dshura.discord.token=YOUR_DISCORD_TOKEN shura-***.jar`

#### Maven + Docker Build
* Have at least jdk 11 and docker 18+
* `./run YOUR_DISCORD_TOKEN` from shura
  - will build latest using bundled maven
  - remove old *Shura* container if exists
  - start docker container at that point can use invite link above

### Commands

#### PLAY
* Can play anything supported by https://github.com/sedmelluq/lavaplayer
* example
* !play https://www.youtube.com/watch?v=miomuSGoPzI
#### SUMMON
* calls bot to the same voice channel as the user typing in command
* !summon
#### LEAVE
* disconnects from voice
#### PAUSE
#### RESUME
#### SKIP
* can skip a single song
* !skip
* or multiple
* !skip 3
* or a whole playlist that's queued
* !skip pl
#### VOLUME
* volume goes from 0 to 1000
* default is
* !volume 20

### Features
* Shura saves all of your inputs and starts where it left off incase it's turned off / crashes
* Songs and playlists from YT are cached on disk. Requires `youtube-dl` and `ffmpeg` on the path.
* Shura has drunk mode enabled by default in application.yml, this means you don't have to type commands exactly
* skop pley and !summie and volum will all work as if you typed it correctly даже поймет по руский

### Properties

Defaults
```
shura:
  cache:
    enabled: true
    updated: true
    directory: cache
  datasource:
    url: jdbc:sqlite:shura.db
    driver: org.sqlite.JDBC
  drunk-mode: true
  thresh-hold: 3
  discord:
    token: 
```

### Platforms
* Windows (x86 and x64)
* Linux (x86 and x64, glibc >= 2.15)

### Local Docker Build
* Build
`./mvnw package`
* Create container
`docker build --tag local/shura:latest .`
* Run
`docker run --name shura --env JAVA_OPTS="-Dshura.discord.token=YOUR_TOKEN" local/shura`

# Thanks
Great libraries that made this fun
* https://github.com/sedmelluq/lavaplayer
* https://github.com/DV8FromTheWorld/JDA
