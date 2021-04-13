package com.bots.shura.caching;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Downloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(Downloader.class);

    private static final Pattern singlePattern = Pattern.compile("^*?(?<v>[a-zA-Z0-9_-]{11})$");
    private static final Pattern playlistPattern = Pattern.compile("^*?(?<list>(PL|LL|FL|UU)[a-zA-Z0-9_-]+)$");

    private final boolean isWindows;

    private final String cacheDirectory;

    public static void main(String[] args) throws MissingDependencyException, YoutubeDLException {
        Downloader downloader = new Downloader("cache");
        downloader.update();

        String playlistUrl = "https://www.youtube.com/playlist?list=PL52ssDO0VaT85OxE9RMVWZpA0AnYScnV-";
        List<String> playlistSongDirectories = downloader.getPlaylistSongs(playlistUrl);

        LOGGER.info("{}", playlistSongDirectories);

        String singleUrl = "https://www.youtube.com/watch?v=mTKvEwdmu_w";
        String singleSongDirectory = downloader.getSingleSong(singleUrl);
        LOGGER.info("{}", singleSongDirectory);

//        downloader.playlist(songUrl);
//        downloader.single("https://www.youtube.com/watch?v=mTKvEwdmu_w");
//        downloader.channel("https://www.youtube.com/channel/UCou1-tqCZ5tLKK225f9iHkg/playlists");
    }

    public Downloader(String cacheDirectory) throws MissingDependencyException {
        this.cacheDirectory = StringUtils.trimToEmpty(cacheDirectory);
        this.isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        try {
            run("--version");
        } catch (YoutubeDLException e) {
            throw new MissingDependencyException("Missing youtube-dl in the environment. Add youtube-dl and ffmpeg to environment.");
        }
    }

    private String getId(String url, Pattern pattern) {
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(0);
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

    public List<String> getPlaylistSongs(String playlistUrl) {
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
    public String single(String url) throws YoutubeDLException {
        return run("--download-archive", cacheDirectory + "/archive.txt",
                "--ignore-errors",
                "--http-chunk-size", "1M",
                "-o",
                cacheDirectory + "/singles/%(id)s-%(title)s.%(ext)s",
                "-x",
                "-f", "best",
                "--audio-quality", "0",
                "--audio-format", "best",
                url);
    }

    public String playlist(String url) throws YoutubeDLException {
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


    /**
     * Downloads playlist, if url is for a playlist<br>
     * If url is not playlist, attempts to download a single<br>
     * Order playlist first, single second matters since some playlists on youtube have both v= and list= query parameters
     * @param url playlist or single url
     * @throws YoutubeDLException
     */
    public void playlistOrSingle(String url) throws YoutubeDLException {
        String playlistId = getId(url, playlistPattern);
        if (playlistId != null) {
            playlist(url);
        } else {
            String singleId = getId(url, singlePattern);
            if (singleId == null) {
                single(url);
            }
        }
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
            new StreamGobbler(process.getErrorStream(), LOGGER::debug).get();

            process.waitFor(60, TimeUnit.SECONDS);

            if (process.exitValue() != 0) {
                throw new YoutubeDLException("Unsuccessful exit code: " + process.exitValue());
            }

            return successResult;
        } catch (Exception ex) {
            throw new YoutubeDLException("Process call failed", ex);
        }
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
