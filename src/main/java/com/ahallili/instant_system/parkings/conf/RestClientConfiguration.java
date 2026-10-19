package com.ahallili.instant_system.parkings.conf;

import java.time.Duration;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfiguration {

    private final RestClientLoggingInterceptor loggingInterceptor;

    public RestClientConfiguration(RestClientLoggingInterceptor loggingInterceptor) {
        this.loggingInterceptor = loggingInterceptor;
    }

    @Bean
    public RestClient restClient(RestClient.Builder builder) {

        // handling of timeout
        // 10 seconds is a lot but it's a small application not supposed for production
        HttpClientSettings settings = HttpClientSettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(10))
                .withReadTimeout(Duration.ofSeconds(10));

        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect()
                .build(settings);

        return builder.requestFactory(requestFactory).requestInterceptor(loggingInterceptor).build();
    }

}
