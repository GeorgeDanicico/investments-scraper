package com.finance.investment.price;

import com.finance.investment.generated.model.PriceResponse;
import tools.jackson.databind.JsonNode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import java.time.Duration;
import java.nio.charset.StandardCharsets;

@Component
public class YahooPriceClient {

    private static final Logger logger = LoggerFactory.getLogger(YahooPriceClient.class);

    private final RestClient restClient;

    public YahooPriceClient(RestClient.Builder restClientBuilder, MarketDataProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .baseUrl(properties.yahooBaseUrl())
                .build();
    }

    public PriceResponse fetch(String instrument) {
        long startedAt = System.nanoTime();
        logger.info("Fetching price from Yahoo Finance for {}", instrument);
        try {
            JsonNode payload = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v8/finance/chart/{instrument}")
                            .queryParam("range", "1d")
                            .queryParam("interval", "1d")
                            .build(UriUtils.encodePathSegment(instrument, StandardCharsets.UTF_8)))
                    .retrieve()
                    .body(JsonNode.class);

            if (payload == null) {
                throw new PriceNotFoundException("Yahoo Finance returned an empty response for " + instrument);
            }
            PriceResponse response = YahooPriceParser.parse(instrument, payload);
            logger.info("Fetched price from Yahoo Finance for {} in {} ms", instrument, elapsedMillis(startedAt));
            return response;
        } catch (PriceNotFoundException exception) {
            logger.warn("Yahoo Finance returned no price for {} after {} ms", instrument, elapsedMillis(startedAt));
            throw exception;
        } catch (RuntimeException exception) {
            logger.warn("Unable to read Yahoo Finance for {} after {} ms", instrument, elapsedMillis(startedAt), exception);
            throw new UpstreamPriceException("Unable to read Yahoo Finance for " + instrument, exception);
        }
    }

    private static long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }
}
