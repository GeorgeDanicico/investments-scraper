package com.finance.investment.price;

import com.finance.investment.generated.model.PriceResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class ZfPriceClient {

    private static final Logger logger = LoggerFactory.getLogger(ZfPriceClient.class);

    private final RestClient restClient;
    private final String url;

    public ZfPriceClient(RestClient.Builder restClientBuilder, MarketDataProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();
        this.url = properties.zfBtEuroClasicUrl();
    }

    public PriceResponse fetch(String instrument) {
        long startedAt = System.nanoTime();
        logger.info("Fetching price from ZF for {}", instrument);
        try {
            String html = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            if (html == null || html.isBlank()) {
                throw new PriceNotFoundException("ZF returned an empty page for " + instrument);
            }
            PriceResponse response = ZfPriceParser.parse(instrument, html);
            logger.info("Fetched price from ZF for {} in {} ms", instrument, elapsedMillis(startedAt));
            return response;
        } catch (PriceNotFoundException exception) {
            logger.warn("ZF returned no price for {} after {} ms", instrument, elapsedMillis(startedAt));
            throw exception;
        } catch (RuntimeException exception) {
            logger.warn("Unable to read ZF for {} after {} ms", instrument, elapsedMillis(startedAt), exception);
            throw new UpstreamPriceException("Unable to read ZF for " + instrument, exception);
        }
    }

    private static long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }
}
