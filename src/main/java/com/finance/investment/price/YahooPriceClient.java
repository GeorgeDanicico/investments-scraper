package com.finance.investment.price;

import com.finance.investment.generated.model.PriceResponse;
import tools.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Component
public class YahooPriceClient {

    private final RestClient restClient;

    public YahooPriceClient(RestClient.Builder restClientBuilder, MarketDataProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl(properties.yahooBaseUrl())
                .defaultHeader("User-Agent", "investment-price-service/1.0")
                .build();
    }

    public PriceResponse fetch(String instrument) {
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
            return YahooPriceParser.parse(instrument, payload);
        } catch (PriceNotFoundException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new UpstreamPriceException("Unable to read Yahoo Finance for " + instrument, exception);
        }
    }
}
