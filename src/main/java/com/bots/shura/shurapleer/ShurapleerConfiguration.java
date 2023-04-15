package com.bots.shura.shurapleer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Configuration
@ConditionalOnProperty(name = "shura.shurapleer.enabled")
public class ShurapleerConfiguration {

    public static class OAuth2AuthorizedClientInterceptor implements ClientHttpRequestInterceptor {

        ShurapleerAuthorizedManager manager;

        public OAuth2AuthorizedClientInterceptor(ShurapleerAuthorizedManager manager) {
            this.manager = manager;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request,
                                            byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {

            HttpHeaders headers = request.getHeaders();
            headers.setBearerAuth(manager.getAccessToken());

            return execution.execute(request, body);
        }
    }

    @Bean
    public RestTemplate shurapleerRestTemplate(ShurapleerAuthorizedManager logtoAuthorizedManager) {

        return new RestTemplateBuilder()
                .requestFactory(() -> {
                    var clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
                    clientHttpRequestFactory.setConnectTimeout(30000);
                    clientHttpRequestFactory.setConnectionRequestTimeout(30000);
                    return clientHttpRequestFactory;
                })
                .additionalInterceptors(new OAuth2AuthorizedClientInterceptor(logtoAuthorizedManager))
                .setBufferRequestBody(true)
                .build();
    }

    @Bean
    public ShurapleerClient shurapleerClient(RestTemplate shurapleerRestTemplate, @Value("${shura.shurapleer.url}") String shurapleerUrl) {
        return new ShurapleerClient(shurapleerUrl, shurapleerRestTemplate);
    }
}
