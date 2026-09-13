package com.finance.investment.price;

import com.finance.investment.generated.model.PriceResponse;
import com.finance.investment.generated.model.PriceSource;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;

final class YahooPriceParser {

    private YahooPriceParser() {
    }

    static PriceResponse parse(String requestedInstrument, JsonNode payload) {
        JsonNode chart = payload.path("chart");
        JsonNode error = chart.path("error");
        if (!error.isMissingNode() && !error.isNull() && !error.isEmpty()) {
            throw new PriceNotFoundException("Yahoo Finance could not find " + requestedInstrument);
        }

        JsonNode result = chart.path("result").path(0);
        if (result.isMissingNode() || result.isNull() || result.isEmpty()) {
            throw new PriceNotFoundException("Yahoo Finance returned no price for " + requestedInstrument);
        }

        JsonNode meta = result.path("meta");
        BigDecimal price = decimalValue(meta.path("regularMarketPrice"));
        if (price == null) {
            price = lastClose(result);
        }
        if (price == null) {
            throw new PriceNotFoundException("Yahoo Finance returned no price for " + requestedInstrument);
        }

        String currency = textValue(meta.path("currency"));
        String asOf = timestamp(meta.path("regularMarketTime"));
        if (asOf == null) {
            asOf = lastTimestamp(result);
        }

        return new PriceResponse(
                requestedInstrument,
                PriceSource.YAHOO_FINANCE,
                price,
                currency,
                asOf
        );
    }

    private static BigDecimal lastClose(JsonNode result) {
        JsonNode closes = result.path("indicators").path("quote").path(0).path("close");
        if (!closes.isArray()) {
            return null;
        }

        for (int index = closes.size() - 1; index >= 0; index--) {
            BigDecimal close = decimalValue(closes.get(index));
            if (close != null) {
                return close;
            }
        }
        return null;
    }

    private static String lastTimestamp(JsonNode result) {
        JsonNode timestamps = result.path("timestamp");
        if (!timestamps.isArray()) {
            return null;
        }

        for (int index = timestamps.size() - 1; index >= 0; index--) {
            JsonNode timestamp = timestamps.get(index);
            if (timestamp != null && timestamp.canConvertToLong()) {
                return timestamp(timestamp);
            }
        }
        return null;
    }

    private static BigDecimal decimalValue(JsonNode node) {
        return node != null && node.isNumber() ? node.decimalValue() : null;
    }

    private static String textValue(JsonNode node) {
        return node != null && node.isTextual() ? node.stringValue() : null;
    }

    private static String timestamp(JsonNode node) {
        return node != null && node.canConvertToLong()
                ? Instant.ofEpochSecond(node.longValue()).toString()
                : null;
    }
}
