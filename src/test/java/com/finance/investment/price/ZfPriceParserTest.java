package com.finance.investment.price;

import com.finance.investment.generated.model.PriceResponse;
import com.finance.investment.generated.model.PriceSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZfPriceParserTest {

    @Test
    void extractsFundValueAndDateFromTheFundMarkup() {
        String html = """
                <div class="fond-mutual">
                    <h1 class="articleTitle h1">BT Euro Clasic</h1>
                    <p><strong style="color: #b51517; font-size:22px">13,5200  eur</strong></p>
                    <p style="font-size:11px">Valoarea unității de fond VUAN din data de 2026-09-09</p>
                </div>
                """;

        PriceResponse response = ZfPriceParser.parse("BTEUROCLASIC", html);

        assertEquals(new BigDecimal("13.5200"), response.getPrice());
        assertEquals("EUR", response.getCurrency());
        assertEquals("2026-09-09", response.getAsOf());
        assertEquals(PriceSource.ZF, response.getSource());
    }

    @Test
    void handlesNestedElementsBeforeThePrice() {
        String html = """
                <div class="fond-mutual">
                    <div style="padding-left: 5px; float:right">
                        <div id="div_chart_1"></div>
                        <div id="div_chart_2"></div>
                    </div>
                    <p><strong>13,5180 eur</strong></p>
                    <p style="font-size:11px">VUAN din data de 2026-09-15</p>
                </div>
                """;

        PriceResponse response = ZfPriceParser.parse("BTEUROCLASIC", html);

        assertEquals(new BigDecimal("13.5180"), response.getPrice());
        assertEquals("EUR", response.getCurrency());
        assertEquals("2026-09-15", response.getAsOf());
    }
}
