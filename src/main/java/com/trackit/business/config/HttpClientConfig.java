package com.trackit.business.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class HttpClientConfig {

    private static HttpClient httpClient(TrackitProperties.Http http) {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, http.connectTimeoutMs())
                .responseTimeout(Duration.ofMillis(http.responseTimeoutMs()));
    }

    private static WebClient build(String baseUrl, TrackitProperties.Http http) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient(http)))
                .build();
    }

    @Bean
    public WebClient authServiceWebClient(TrackitProperties props) {
        return build(props.auth().baseUrl(), props.http());
    }

    @Bean
    public WebClient dataServiceWebClient(TrackitProperties props) {
        return build(props.data().baseUrl(), props.http());
    }
}
