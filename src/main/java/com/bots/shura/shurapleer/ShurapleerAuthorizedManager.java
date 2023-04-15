package com.bots.shura.shurapleer;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "shura.shurapleer.enabled")
public class ShurapleerAuthorizedManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShurapleerAuthorizedManager.class);

    private static final Integer ACCESS_TOKEN_KEY = 1;

    @Value(value = "${shura.shurapleer.authentication.token-uri}")
    String tokenUri;

    @Value(value = "${shura.shurapleer.authentication.client-id}")
    String clientId;

    @Value(value = "${shura.shurapleer.authentication.client-secret}")
    String clientSecret;

    @Value(value = "${shura.shurapleer.authentication.authorization-grant-type}")
    String authorizationGrantType;

    @Value(value = "${shura.shurapleer.authentication.scope}")
    String scope;

    @Value(value = "${shura.shurapleer.authentication.resource}")
    String resource;

    final RestTemplate restTemplate;

    final JwtDecoder jwtDecoder;

    final ConcurrentMapCache accessTokenCache;

    ShurapleerAuthorizedManager(@Value(value = "${shura.shurapleer.authentication.jwk-uri}") String jwkUri) {

        var jwkCache = new ConcurrentMapCache("jwkCache");

        this.restTemplate = new RestTemplateBuilder()
                .requestFactory(() -> {
                    var clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
                    clientHttpRequestFactory.setConnectTimeout(30000);
                    clientHttpRequestFactory.setConnectionRequestTimeout(30000);
                    return clientHttpRequestFactory;
                })
                .build();

        final NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkUri)
                .cache(jwkCache)
                // The decoder should support the token type: Access Token + JWT.
                .jwtProcessorCustomizer(customizer -> customizer.setJWSTypeVerifier(
                        new DefaultJOSEObjectTypeVerifier<>(new JOSEObjectType("at+jwt"))))
                .jwsAlgorithm(SignatureAlgorithm.ES384)
                .build();

        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator()));

        this.jwtDecoder = jwtDecoder;
        this.accessTokenCache = new ConcurrentMapCache("shurapleerTokenCache");
    }

    public String getAccessToken() {
        final var accessTokenValue = accessTokenCache.get(ACCESS_TOKEN_KEY);
        final String accessToken = accessTokenValue == null ? null : (String) accessTokenValue.get();
        if (StringUtils.isNotBlank(accessToken)) {
            try {
                jwtDecoder.decode(accessToken);
                return accessToken;
            } catch (JwtException ex) {
                LOGGER.info("Failed to decode client credential access token, acquiring a new one");
                LOGGER.debug("Decode error", ex);
            }
        }

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("Authorization", "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8)));

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", authorizationGrantType);
        requestBody.add("resource", resource);
        requestBody.add("scope", scope);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(requestBody, headers);

        final var response = restTemplate.exchange(tokenUri,
                HttpMethod.POST,
                entity, Map.class).getBody();

        final String newAccessToken = (String) response.get("access_token");
        accessTokenCache.put(ACCESS_TOKEN_KEY, newAccessToken);

        return newAccessToken;
    }
}
