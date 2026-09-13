package com.finance.investment.price;

import com.finance.investment.generated.model.PriceErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PriceExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<PriceErrorResponse> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new PriceErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(PriceNotFoundException.class)
    public ResponseEntity<PriceErrorResponse> notFound(PriceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new PriceErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(UpstreamPriceException.class)
    public ResponseEntity<PriceErrorResponse> upstreamFailure(UpstreamPriceException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new PriceErrorResponse(exception.getMessage()));
    }
}
