package com.finance.investment.price;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "market-data")
public record MarketDataProperties(
        String yahooBaseUrl,
        String zfBtEuroClasicUrl
) {
}
