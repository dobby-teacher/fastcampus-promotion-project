package com.fastcampus.timesaleservice.controller.v2;

import com.fastcampus.timesaleservice.domain.TimeSale;
import com.fastcampus.timesaleservice.dto.TimeSaleDto;
import com.fastcampus.timesaleservice.service.v2.TimeSaleRedisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController("TimeSaleControllerV2")
@RequestMapping("/api/v2/time-sales")
@RequiredArgsConstructor
public class TimeSaleController {
    private final TimeSaleRedisService timeSaleRedisService;

    @PostMapping
    public ResponseEntity<TimeSaleDto.Response> createTimeSale(@Valid @RequestBody TimeSaleDto.CreateRequest request) {
        TimeSale timeSale = timeSaleRedisService.createTimeSale(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TimeSaleDto.Response.from(timeSale));
    }

    @GetMapping("/{timeSaleId}")
    public ResponseEntity<TimeSaleDto.Response> getTimeSale(@PathVariable Long timeSaleId) {
        TimeSale timeSale = timeSaleRedisService.getTimeSale(timeSaleId);
        return ResponseEntity.ok(TimeSaleDto.Response.from(timeSale));
    }

    @GetMapping
    public ResponseEntity<Page<TimeSaleDto.Response>> getOngoingTimeSales(@PageableDefault Pageable pageable) {
        Page<TimeSale> timeSales = timeSaleRedisService.getOngoingTimeSales(pageable);
        return ResponseEntity.ok(timeSales.map(TimeSaleDto.Response::from));
    }

    @PostMapping("/{timeSaleId}/purchase")
    public ResponseEntity<TimeSaleDto.PurchaseResponse> purchaseTimeSale(
            @PathVariable Long timeSaleId,
            @Valid @RequestBody TimeSaleDto.PurchaseRequest request) {
        TimeSale timeSale = timeSaleRedisService.purchaseTimeSale(timeSaleId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TimeSaleDto.PurchaseResponse.from(timeSale, request.getUserId(), request.getQuantity()));
    }
}
