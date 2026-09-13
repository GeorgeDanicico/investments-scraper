package com.finance.investment.price;

import com.finance.investment.generated.model.PriceResponse;
import com.finance.investment.generated.model.PriceSource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ZfPriceParser {

    private static final Pattern PRICE_PATTERN = Pattern.compile(
            "(?<price>\\d+(?:[.,]\\d+)?)\\s*(?<currency>[A-Za-z]{3})?"
    );
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");

    private ZfPriceParser() {
    }

    static PriceResponse parse(String instrument, String html) {
        Document document = Jsoup.parse(html);
        Element priceElement = document.selectFirst("div.fond-mutual > p > strong");
        if (priceElement == null) {
            throw new PriceNotFoundException("ZF returned no fund price for " + instrument);
        }

        Matcher priceMatcher = PRICE_PATTERN.matcher(priceElement.text());
        if (!priceMatcher.find()) {
            throw new PriceNotFoundException("ZF returned an unreadable fund price for " + instrument);
        }

        String rawPrice = priceMatcher.group("price").replace(',', '.');
        String currency = priceMatcher.group("currency");
        String asOf = findAsOfDate(document);

        return new PriceResponse(
                instrument,
                PriceSource.ZF,
                new BigDecimal(rawPrice),
                currency == null ? null : currency.toUpperCase(Locale.ROOT),
                asOf
        );
    }

    private static String findAsOfDate(Document document) {
        Element dateElement = document.selectFirst("div.fond-mutual > p[style*=font-size]");
        if (dateElement == null) {
            return null;
        }

        Matcher dateMatcher = DATE_PATTERN.matcher(dateElement.text());
        if (!dateMatcher.find()) {
            return null;
        }

        try {
            return LocalDate.parse(dateMatcher.group(1), DateTimeFormatter.ISO_LOCAL_DATE).toString();
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
}
