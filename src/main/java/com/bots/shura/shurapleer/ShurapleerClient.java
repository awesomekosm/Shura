package com.bots.shura.shurapleer;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;

public class ShurapleerClient {

    private final String basePath;
    private final RestTemplate restTemplate;

    ShurapleerClient(final String basePath, RestTemplate restTemplate) {
        this.basePath = basePath;
        this.restTemplate = restTemplate;
    }

    public String getBasePath() {
        return basePath;
    }

    public List<MediaLocation> getPlaylistMediaLocations(String playlistId, String mediaId) {
        var requestBuilder = UriComponentsBuilder.fromUriString(basePath)
                .pathSegment("api", "media-location", "playlist", playlistId);

        if (mediaId != null) {
            requestBuilder.pathSegment("media", mediaId);
        }

        var request = requestBuilder.build().toUriString();
        MediaLocation[] result = restTemplate.getForObject(request, MediaLocation[].class);

        return result == null ? List.of() : Arrays.asList(result);
    }

    public MediaLocation getMediaLocation(String mediaId) {
        var request = UriComponentsBuilder.fromUriString(basePath)
                .pathSegment("api", "media-location", "media", mediaId)
                .build().toUriString();

        return restTemplate.getForObject(request, MediaLocation.class);
    }

    public List<MediaLocation> getAccountMediaLocations(String accountId) {
        var request = UriComponentsBuilder.fromUriString(basePath)
                .pathSegment("api", "media-location", "account", accountId)
                .build().toUriString();

        MediaLocation[] result = restTemplate.getForObject(request, MediaLocation[].class);

        return result == null ? List.of() : Arrays.asList(result);
    }

    static class MediaLocation {
        String publicId;
        String localUri;
        String name;
        String artist;
        String album;
        public String getPublicId() {
            return publicId;
        }

        public void setPublicId(String publicId) {
            this.publicId = publicId;
        }

        public String getLocalUri() {
            return localUri;
        }

        public void setLocalUri(String localUri) {
            this.localUri = localUri;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getArtist() {
            return artist;
        }

        public void setArtist(String artist) {
            this.artist = artist;
        }

        public String getAlbum() {
            return album;
        }

        public void setAlbum(String album) {
            this.album = album;
        }
    }
}
