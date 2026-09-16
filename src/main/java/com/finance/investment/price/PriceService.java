package com.finance.investment.price;

import com.finance.investment.generated.model.PriceResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class PriceService {

    static final String PRICE_CACHE = "prices";
    private static final String BT_EURO_CLASIC = "BTEUROCLASIC";
    private static final String INSTRUMENT_PATTERN = "[A-Z0-9][A-Z0-9.-]{0,19}";

    private final YahooPriceClient yahooPriceClient;
    private final ZfPriceClient zfPriceClient;

    public PriceService(YahooPriceClient yahooPriceClient, ZfPriceClient zfPriceClient) {
        this.yahooPriceClient = yahooPriceClient;
        this.zfPriceClient = zfPriceClient;
    }

    @Cacheable(
            cacheNames = PRICE_CACHE,
            key = "#p0 == null ? null : #p0.trim().toUpperCase(T(java.util.Locale).ROOT)"
    )
    public PriceResponse getPrice(String requestedInstrument) {
        String instrument = normalize(requestedInstrument);
        if (BT_EURO_CLASIC.equals(instrument)) {
            return zfPriceClient.fetch(instrument);
        }
        return yahooPriceClient.fetch(instrument);
    }

    private static String normalize(String requestedInstrument) {
        if (requestedInstrument == null) {
            throw new IllegalArgumentException("Instrument is required");
        }

        String instrument = requestedInstrument.trim().toUpperCase(Locale.ROOT);
        if (!instrument.matches(INSTRUMENT_PATTERN)) {
            throw new IllegalArgumentException("Invalid instrument: " + requestedInstrument);
        }
        return instrument;
    }
}
