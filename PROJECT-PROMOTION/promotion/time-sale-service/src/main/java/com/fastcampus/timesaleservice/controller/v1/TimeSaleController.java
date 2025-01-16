package com.fastcampus.timesaleservice.controller.v1;

import com.fastcampus.timesaleservice.domain.TimeSale;
import com.fastcampus.timesaleservice.dto.TimeSaleDto;
import com.fastcampus.timesaleservice.service.v1.TimeSaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/time-sales")
@RequiredArgsConstructor
public class TimeSaleController {
    private final TimeSaleService timeSaleService;

    @PostMapping
    public ResponseEntity<TimeSaleDto.Response> createTimeSale(@Valid @RequestBody TimeSaleDto.CreateRequest request) {
        TimeSale timeSale = timeSaleService.createTimeSale(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TimeSaleDto.Response.from(timeSale));
    }

    @GetMapping("/{timeSaleId}")
    public ResponseEntity<TimeSaleDto.Response> getTimeSale(@PathVariable Long timeSaleId) {
        TimeSale timeSale = timeSaleService.getTimeSale(timeSaleId);
        return ResponseEntity.ok(TimeSaleDto.Response.from(timeSale));
    }

    @GetMapping
    public ResponseEntity<Page<TimeSaleDto.Response>> getOngoingTimeSales(@PageableDefault Pageable pageable) {
        Page<TimeSale> timeSales = timeSaleService.getOngoingTimeSales(pageable);
        return ResponseEntity.ok(timeSales.map(TimeSaleDto.Response::from));
    }

    @PostMapping("/{timeSaleId}/purchase")
    public ResponseEntity<TimeSaleDto.PurchaseResponse> purchaseTimeSale(
            @PathVariable Long timeSaleId,
            @Valid @RequestBody TimeSaleDto.PurchaseRequest request) {
        TimeSale timeSale = timeSaleService.purchaseTimeSale(timeSaleId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TimeSaleDto.PurchaseResponse.from(timeSale, request.getUserId(), request.getQuantity()));
    }
}
