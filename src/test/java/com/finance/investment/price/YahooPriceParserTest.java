package com.finance.investment.price;

import com.finance.investment.generated.model.PriceResponse;
import com.finance.investment.generated.model.PriceSource;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YahooPriceParserTest {

    private final JsonMapper jsonMapper = JsonMapper.shared();

    @Test
    void prefersYahooRegularMarketPrice() throws Exception {
        JsonNode payload = jsonMapper.readTree("""
                {
                  "chart": {
                    "result": [{
                      "meta": {
                        "currency": "EUR",
                        "regularMarketPrice": 127.865,
                        "regularMarketTime": 1789140966
                      },
                      "indicators": {"quote": [{"close": [127.80]}]}
                    }],
                    "error": null
                  }
                }
                """);

        PriceResponse response = YahooPriceParser.parse("VUAA.DE", payload);

        assertEquals(new BigDecimal("127.865"), response.getPrice());
        assertEquals("EUR", response.getCurrency());
        assertEquals("2026-09-11T15:36:06Z", response.getAsOf());
        assertEquals(PriceSource.YAHOO_FINANCE, response.getSource());
    }

    @Test
    void fallsBackToLastCloseWhenMarketPriceIsMissing() throws Exception {
        JsonNode payload = jsonMapper.readTree("""
                {
                  "chart": {
                    "result": [{
                      "meta": {"currency": "EUR"},
                      "timestamp": [1789110000, 1789140966],
                      "indicators": {"quote": [{"close": [126.54, 127.865]}]}
                    }],
                    "error": null
                  }
                }
                """);

        PriceResponse response = YahooPriceParser.parse("VWCE.DE", payload);

        assertEquals(new BigDecimal("127.865"), response.getPrice());
        assertEquals("2026-09-11T15:36:06Z", response.getAsOf());
    }
}
