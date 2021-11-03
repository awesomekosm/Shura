package com.bots.shura.caching;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Requires youtube-dl and ffmpeg
 * <p>
 * wget https://yt-dl.org/downloads/latest/youtube-dl -O /usr/local/bin/youtube-dl
 * chmod a+rx /usr/local/bin/youtube-dl
 * sudo apt install python
 * sudo apt install ffmpeg
 */
public class Downloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(Downloader.class);

    private static final Pattern singlePattern = Pattern.compile("^.*?\\?v=([a-zA-Z0-9_-]{11})$");
    private static final Pattern playlistPattern = Pattern.compile("^.*?\\?list=((PL|LL|FL|UU)[a-zA-Z0-9_-]+)$");

    private final boolean isWindows;

    private final ObjectMapper objectMapper;

    private final String cacheDirectory;

    private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

    public static void main(String[] args) throws MissingDependencyException, YoutubeDLException, ExecutionException, InterruptedException {
        Downloader downloader = new Downloader("cache");
        YoutubeUrlCorrection youtubeUrlCorrection = new YoutubeUrlCorrection();
        String result = youtubeUrlCorrection.correctUrl("https://www.youtube.com/watch?v=r-29Jj3WMkc&list=PL9Ye3Lm8rWI9uwZ3dfc8UnZulzdXlN0WV&index=1");
        result = youtubeUrlCorrection.correctUrl("https://youtu.be/watch?v=r-29Jj3WMkc&list=PL9Ye3Lm8rWI9uwZ3dfc8UnZulzdXlN0WV&index=1");

        downloader.update();

        String playlistUrl = "https://www.youtube.com/playlist?list=PL52ssDO0VaT85OxE9RMVWZpA0AnYScnV-";
        LOGGER.info("songs to play {}", downloader.getPlayListSongsAll(playlistUrl));

        String singleUrl = "https://www.youtube.com/watch?v=mTKvEwdmu_w";
        String singleSongDirectory = downloader.getSingleSong(singleUrl);
        LOGGER.info("{}", singleSongDirectory);

//        downloader.playlist(songUrl).get();
//        downloader.single("https://www.youtube.com/watch?v=mTKvEwdmu_w").get();
//        downloader.channel("https://www.youtube.com/channel/UCou1-tqCZ5tLKK225f9iHkg/playlists");

        downloader.shutdown();
    }

    public Downloader(String cacheDirectory) throws MissingDependencyException {
        this.cacheDirectory = StringUtils.trimToEmpty(cacheDirectory);
        this.isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        this.objectMapper = new ObjectMapper();
        try {
            run("--version");
        } catch (YoutubeDLException e) {
            throw new MissingDependencyException("Missing youtube-dl in the environment. Add youtube-dl and ffmpeg to environment.");
        }
    }

    private String getId(String url, Pattern pattern) {
        Matcher matcher = pattern.matcher(url);
        if (matcher.find() && matcher.groupCount() == 2) {
            return matcher.group(1);
        }

        return null;
    }

    public String getSingleSong(String singleUrl) {
        String singleId = getId(singleUrl, singlePattern);
        if (singleId == null) {
            return null;
        }

        File directoryPath = Paths.get(cacheDirectory, "singles").toFile();
        File[] singles = directoryPath.listFiles((dir, name) -> name.startsWith(singleId));
        if (singles != null && singles.length == 1) {
            return singles[0].getPath();
        }

        return null;
    }

    public List<String> getPlayListSongsAll(String playlistUrl) throws YoutubeDLException, ExecutionException, InterruptedException {
        List<String> playlistSongs = getPlaylistSongs(playlistUrl);
        LOGGER.info("cached playlist songs {}", playlistSongs.size());

        if (playlistSongs.isEmpty()) {
            // nothing is cached
            return List.of();
        }

        final String playListSyncJSON = playlist(playlistUrl, true).get();
        if (playListSyncJSON == null) {
            LOGGER.error("Failed to get playlist --dump-single-json");
            throw new YoutubeDLException("Failed to load playlist " + playlistUrl + " from youtube");
        }
        try {
            // merge cached and newly added tracks
            Map<String, Object> playListSync = objectMapper.readValue(playListSyncJSON, Map.class);
            List<HashMap<String, Object>> newEntriesToSync = (List<HashMap<String, Object>>) playListSync.get("entries");
            if (newEntriesToSync != null && !newEntriesToSync.isEmpty()) {
                LOGGER.info("will sync {} new playlist entries",  newEntriesToSync.size());
                newEntriesToSync.forEach(s -> {
                    Integer playlistIndex = (Integer) s.get("playlist_index");
                    // youtube starts count from 1
                    playlistIndex = playlistIndex - 1;
                    String originalUrl = (String) s.get("original_url");
                    if (playlistIndex < playlistSongs.size()) {
                        playlistSongs.add(playlistIndex, originalUrl);
                    }
                });
            }
        } catch (JsonProcessingException e) {
            LOGGER.error("Can't object map --dump-single-json", e);
        }

        return playlistSongs;
    }

    private List<String> getPlaylistSongs(String playlistUrl) {
        String playlistId = getId(playlistUrl, playlistPattern);
        if (playlistId == null) {
            return List.of();
        }

        File directoryPath = new File(cacheDirectory);
        String[] directories = directoryPath.list();
        if (directories != null) {
            for (String directory : directories) {
                if (directory.startsWith(playlistId)) {
                    File playlistDirectoryFile = Paths.get(cacheDirectory, directory).toFile();
                    String[] singlesNames = playlistDirectoryFile.list();
                    final String playlistDirectory = directory;

                    if (singlesNames != null) {
                        // sort numerically
                        return Arrays.stream(singlesNames).sorted((o1, o2) -> {
                                    String noIdFirst = o1.substring(12);
                                    String noIdSecond = o2.substring(12);
                                    return noIdFirst.compareTo(noIdSecond);
                                }).collect(Collectors.toList())
                                // prepend directory for full path - can be absolute or relative depending on cacheDirectory
                                .stream().map(single -> Paths.get(cacheDirectory, playlistDirectory, single).toString())
                                .collect(Collectors.toList());
                    }
                    break;
                }
            }
        }

        return List.of();
    }

    //youtube-dl -o '%(uploader)s/%(playlist)s/%(playlist_index)s - %(title)s.%(ext)s' https://www.youtube.com/channel/UCou1-tqCZ5tLKK225f9iHkg/playlists
    public Future<String> single(String url) {
        return singleThreadExecutor.submit(() -> {
            try {
                return run("--download-archive", cacheDirectory + "/singles/archive.txt",
                        "--ignore-errors",
                        "--http-chunk-size", "1M",
                        "-o",
                        cacheDirectory + "/singles/%(id)s-%(title)s.%(ext)s",
                        "-x",
                        "-f", "best[filesize<50M]",
                        "--audio-quality", "0",
                        "--audio-format", "best",
                        url);
            } catch (YoutubeDLException e) {
                LOGGER.error("Downloading single failed", e);
            }
            return null;
        });
    }

    public Future<String> playlist(String url, boolean simulate, String chunkSize) {
        return singleThreadExecutor.submit(() -> {
            try {
                List<String> ytdlArgs = new ArrayList<>(Arrays.asList(
                        "--download-archive", cacheDirectory + "/archive.txt",
                        "--ignore-errors",
                        "--http-chunk-size", chunkSize,
                        "-o",
                        cacheDirectory + "/%(playlist_id)s-%(playlist)s/%(id)s-%(playlist_index)s-%(title)s.%(ext)s",
                        "-x",
                        "-f", "best[filesize<50M]",
                        "--audio-quality", "0",
                        "--audio-format", "best",
                        url
                ));

                if (simulate) {
                    ytdlArgs.addAll(ytdlArgs.size() - 1,
                            Arrays.asList(
                                    "--simulate",
                                    "--dump-single-json"
                            ));
                }
                return run(ytdlArgs.toArray(new String[0]));
            } catch (YoutubeDLException e) {
                LOGGER.error("Downloading playlist failed. simulated: " + simulate , e);
            }
            return null;
        });
    }

    public Future<String> playlist(String url) {
        return playlist(url, false, "1M");
    }

    public Future<String> playlist(String url, boolean simulate) {
        return playlist(url, simulate, "1M");
    }

    /**
     * Downloads playlist, if url is for a playlist<br>
     * If url is not playlist, attempts to download a single<br>
     * Order playlist first, single second matters since some playlists on youtube have both v= and list= query parameters
     *
     * @param url playlist or single url
     * @throws YoutubeDLException
     */
    public Future<String> playlistOrSingle(String url) throws YoutubeDLException {
        String playlistId = getId(url, playlistPattern);
        if (playlistId != null) {
            return playlist(url);
        } else {
            String singleId = getId(url, singlePattern);
            if (singleId != null) {
                return single(url);
            }
        }

        throw new YoutubeDLException("Failed to identify playlist or single url: " + url);
    }

    public String channel(String url) throws YoutubeDLException {
        return run("--download-archive", cacheDirectory + "/archive.txt",
                "--ignore-errors",
                "--http-chunk-size", "1M",
                "-o",
                cacheDirectory + "/%(playlist_id)s-%(playlist)s/%(id)s-%(playlist_index)s-%(title)s.%(ext)s",
                "-x",
                "-f", "best",
                "--audio-quality", "0",
                "--audio-format", "best",
                url);
    }

    private String getInfo(String url) throws YoutubeDLException {
        return run("--simulate",
                "--dump-single-json",
                url);
    }

    public String update() throws YoutubeDLException {
        return run("-U");
    }

    public String run(String... commands) throws YoutubeDLException {
        ProcessBuilder builder = new ProcessBuilder();
        final String[] processCommands = new String[commands.length + 1];
        processCommands[0] = "youtube-dl";
        if (isWindows) {
            processCommands[0] = "youtube-dl.exe";
        }
        System.arraycopy(commands, 0, processCommands, 1, commands.length);

        builder.command(processCommands);

        LOGGER.debug("Running: {}", String.join(" ", builder.command()));

        try {
            Process process = builder.start();
            String successResult = new StreamGobbler(process.getInputStream(), LOGGER::info).get();
            String errorResult = new StreamGobbler(process.getErrorStream(), LOGGER::error).get();

            process.waitFor(60, TimeUnit.SECONDS);

            if (process.exitValue() != 0) {
                throw new YoutubeDLException("Unsuccessful exit code: " + process.exitValue() + " " + errorResult);
            }

            return successResult;
        } catch (Exception ex) {
            throw new YoutubeDLException("Process call failed", ex);
        }
    }

    public void shutdown() {
        singleThreadExecutor.shutdown();
    }

    public static class YoutubeDLException extends Exception {

        public YoutubeDLException(String exceptionMessage) {
            super(exceptionMessage);
        }

        public YoutubeDLException(String exceptionMessage, Throwable throwable) {
            super(exceptionMessage, throwable);
        }
    }

    public static class MissingDependencyException extends Exception {

        public MissingDependencyException(String exceptionMessage) {
            super(exceptionMessage);
        }
    }

    private static class StreamGobbler {
        private final InputStream inputStream;
        private final Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> stringConsumer) {
            this.inputStream = inputStream;
            this.consumer = stringConsumer;
        }

        public String get() {
            Stream<String> stream = new BufferedReader(new InputStreamReader(inputStream)).lines();
            StringBuilder stringBuilder = new StringBuilder();
            stream.forEach(str -> {
                consumer.accept(str);
                stringBuilder.append(str);
            });
            return stringBuilder.toString();
        }
    }
}
