package com.bots.shura.caching;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoutubeUrlCorrection {

    private static final Logger LOGGER = LoggerFactory.getLogger(YoutubeUrlCorrection.class);

    private static final String PROTOCOL_REGEX = "(?:http://|https://|)";
    private static final String DOMAIN_REGEX = "(?:www\\.|m\\.|music\\.|)youtube\\.com";
    private static final String SHORT_DOMAIN_REGEX = "(?:www\\.|)youtu\\.be";

    private static final Pattern youtubeLongPattern = Pattern.compile("^" + PROTOCOL_REGEX + DOMAIN_REGEX + "/.*");
    private static final Pattern youtubeShortPattern = Pattern.compile("^" + PROTOCOL_REGEX + SHORT_DOMAIN_REGEX + "/.*");

    public String correctUrl(final String url) {
        Matcher matcherLong = youtubeLongPattern.matcher(url);
        Matcher matcherShort = youtubeShortPattern.matcher(url);
        boolean matchedLong = matcherLong.find();
        boolean matchedShort = matcherShort.find();
        if (matchedLong || matchedShort) {
            String youtubeUrl = matchedLong ? matcherLong.group(0) : matcherShort.group(0);
            try {
                URIBuilder uriBuilder = new URIBuilder(youtubeUrl);
                if (StringUtils.contains(uriBuilder.getPath(), "watch")) {
                    String listParamValue = null;
                    String vParam = null;
                    for (var queryParamNaveValue : uriBuilder.getQueryParams()) {
                        if (queryParamNaveValue.getName().equals("list")) {
                            listParamValue = queryParamNaveValue.getValue();
                        }
                        if (queryParamNaveValue.getName().equals("v")) {
                            vParam = queryParamNaveValue.getValue();
                        }
                    }
                    if (StringUtils.isNotBlank(listParamValue) && StringUtils.isNotBlank(vParam)) {
                        URIBuilder finalUriBuilder = new URIBuilder("https://www.youtube.com");
                        finalUriBuilder.setPath("watch");
                        finalUriBuilder.setParameter("v", vParam);

                        return finalUriBuilder.build().toString();
                    }
                }
            } catch (URISyntaxException e) {
                LOGGER.error("Failed to parse url", e);
                return url;
            }
        }

        return url;
    }
}
