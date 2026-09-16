package com.finance.investment.price;

import com.finance.investment.generated.model.PriceResponse;
import com.finance.investment.generated.model.PriceSource;
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
    private static final Pattern PRICE_ELEMENT_PATTERN = Pattern.compile(
            "(?is)<div\\b[^>]*\\bclass\\s*=\\s*(['\"])[^'\"]*\\bfond-mutual\\b[^'\"]*\\1[^>]*>"
                    + ".*?<p\\b[^>]*>\\s*<strong\\b[^>]*>(?<value>.*?)</strong\\s*>"
    );
    private static final Pattern DATE_ELEMENT_PATTERN = Pattern.compile(
            "(?is)<div\\b[^>]*\\bclass\\s*=\\s*(['\"])[^'\"]*\\bfond-mutual\\b[^'\"]*\\1[^>]*>"
                    + ".*?<p\\b[^>]*\\bstyle\\s*=\\s*(['\"])[^'\"]*font-size[^'\"]*\\2[^>]*>"
                    + ".*?(?<date>\\d{4}-\\d{2}-\\d{2})"
    );
    private static final Pattern MARKUP_PATTERN = Pattern.compile("(?s)<[^>]*>");

    private ZfPriceParser() {
    }

    static PriceResponse parse(String instrument, String html) {
        Matcher priceElementMatcher = PRICE_ELEMENT_PATTERN.matcher(html);
        if (!priceElementMatcher.find()) {
            throw new PriceNotFoundException("ZF returned no fund price for " + instrument);
        }

        Matcher priceMatcher = PRICE_PATTERN.matcher(stripMarkup(priceElementMatcher.group("value")));
        if (!priceMatcher.find()) {
            throw new PriceNotFoundException("ZF returned an unreadable fund price for " + instrument);
        }

        String rawPrice = priceMatcher.group("price").replace(',', '.');
        String currency = priceMatcher.group("currency");
        String asOf = findAsOfDate(html);

        return new PriceResponse(
                instrument,
                PriceSource.ZF,
                new BigDecimal(rawPrice),
                currency == null ? null : currency.toUpperCase(Locale.ROOT),
                asOf
        );
    }

    private static String findAsOfDate(String html) {
        Matcher dateElementMatcher = DATE_ELEMENT_PATTERN.matcher(html);
        if (!dateElementMatcher.find()) {
            return null;
        }

        try {
            return LocalDate.parse(dateElementMatcher.group("date"), DateTimeFormatter.ISO_LOCAL_DATE).toString();
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private static String stripMarkup(String value) {
        return MARKUP_PATTERN.matcher(value.replace("&nbsp;", " "))
                .replaceAll(" ")
                .trim();
    }
}
