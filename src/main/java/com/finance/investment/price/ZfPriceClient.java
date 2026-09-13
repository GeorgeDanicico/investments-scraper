package com.finance.investment.price;

import com.finance.investment.generated.model.PriceResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ZfPriceClient {

    private final RestClient restClient;
    private final String url;

    public ZfPriceClient(RestClient.Builder restClientBuilder, MarketDataProperties properties) {
        this.restClient = restClientBuilder
                .defaultHeader("User-Agent", "investment-price-service/1.0")
                .build();
        this.url = properties.zfBtEuroClasicUrl();
    }

    public PriceResponse fetch(String instrument) {
        try {
            String html = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            if (html == null || html.isBlank()) {
                throw new PriceNotFoundException("ZF returned an empty page for " + instrument);
            }
            return ZfPriceParser.parse(instrument, html);
        } catch (PriceNotFoundException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new UpstreamPriceException("Unable to read ZF for " + instrument, exception);
        }
    }
}
