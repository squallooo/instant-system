package com.ahallili.instant_system.parkings.conf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

/**
 * 
 */
// @Slf4j
@Component
public class RestClientLoggingInterceptor implements ClientHttpRequestInterceptor {

        private final Logger logger = LoggerFactory.getLogger(RestClientLoggingInterceptor.class);

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
                        throws IOException {

                logger.info("HTTP Request: {} {}",
                                request.getMethod(),
                                request.getURI());

                logger.info("Request body: {}",
                                new String(body, StandardCharsets.UTF_8));

                ClientHttpResponse response = execution.execute(request, body);

                logger.info("HTTP Response: {} {} -> {}",
                                request.getMethod(),
                                request.getURI(),
                                response.getStatusCode());

                return response;
        }

}
