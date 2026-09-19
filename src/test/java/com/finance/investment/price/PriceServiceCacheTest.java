package com.finance.investment.price;

import com.finance.investment.generated.model.PriceResponse;
import com.finance.investment.generated.model.PriceSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
class PriceServiceCacheTest {

    @MockitoBean
    private YahooPriceClient yahooPriceClient;

    @MockitoBean
    private ZfPriceClient zfPriceClient;

    @Autowired
    private PriceController priceController;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCache() {
        cacheManager.getCache(PriceService.PRICE_CACHE).clear();
    }

    @Test
    void cachesNormalizedInstrumentForThreeHours() {
        PriceResponse expected = new PriceResponse(
                "VUAA.DE",
                PriceSource.YAHOO_FINANCE,
                new BigDecimal("127.865"),
                "EUR",
                "2026-09-11T15:36:06Z"
        );
        when(yahooPriceClient.fetch("VUAA.DE")).thenReturn(expected);

        assertEquals(expected, priceController.getPrice("vuaa.de"));
        assertEquals(expected, priceController.getPrice("VUAA.DE"));

        verify(yahooPriceClient, times(1)).fetch("VUAA.DE");

        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
                (com.github.benmanes.caffeine.cache.Cache<Object, Object>) cacheManager
                        .getCache(PriceService.PRICE_CACHE)
                        .getNativeCache();
        assertEquals(32, nativeCache.policy().eviction().orElseThrow().getMaximum());
        assertTrue(nativeCache.policy().expireAfterWrite().isPresent());
        assertEquals(java.time.Duration.ofHours(3), nativeCache.policy().expireAfterWrite().orElseThrow()
                .getExpiresAfter());
    }
}
