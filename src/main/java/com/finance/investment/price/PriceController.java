package com.finance.investment.price;

import com.finance.investment.generated.api.PricesApi;
import com.finance.investment.generated.model.PriceResponse;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PriceController implements PricesApi {

    private final PriceService priceService;

    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    @Override
    public PriceResponse getPrice(String instrument) {
        return priceService.getPrice(PriceService.normalize(instrument));
    }
}
