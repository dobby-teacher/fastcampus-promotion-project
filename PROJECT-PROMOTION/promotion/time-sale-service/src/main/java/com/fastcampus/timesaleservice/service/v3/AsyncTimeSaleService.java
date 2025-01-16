package com.fastcampus.timesaleservice.service.v3;

import com.fastcampus.timesaleservice.domain.TimeSale;
import com.fastcampus.timesaleservice.dto.TimeSaleDto;
import com.fastcampus.timesaleservice.service.v2.TimeSaleRedisService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AsyncTimeSaleService {
    private final TimeSaleRedisService timeSaleRedisService;
    private final TimeSaleProducer timeSaleProducer;
    private final RedissonClient redissonClient;
    private static final String RESULT_PREFIX = "purchase-result:";

    public TimeSale createTimeSale(TimeSaleDto.CreateRequest request) {
        return timeSaleRedisService.createTimeSale(request);
    }

    public TimeSale getTimeSale(Long id) {
        return timeSaleRedisService.getTimeSale(id);
    }

    public Page<TimeSale> getOngoingTimeSales(Pageable pageable) {
        return timeSaleRedisService.getOngoingTimeSales(pageable);
    }

    public String purchaseTimeSale(Long timeSaleId, TimeSaleDto.PurchaseRequest request) {
        // 구매 요청을 Kafka로 전송하고 요청 ID를 반환
        return timeSaleProducer.sendPurchaseRequest(timeSaleId, request.getUserId(), request.getQuantity());
    }

    public TimeSaleDto.AsyncPurchaseResponse getPurchaseResult(Long timeSaleId, String requestId) {
        RBucket<String> resultBucket = redissonClient.getBucket(RESULT_PREFIX + requestId);
        String result = resultBucket.get();
        String status = result != null ? result : "PENDING";
        
        // 대기 순서 정보 조회
        Integer queuePosition = null;
        Long totalWaiting = 0L;
        
        if ("PENDING".equals(status)) {
            queuePosition = timeSaleProducer.getQueuePosition(timeSaleId, requestId);
            totalWaiting = timeSaleProducer.getTotalWaiting(timeSaleId);
        }

        return TimeSaleDto.AsyncPurchaseResponse.builder()
                .requestId(requestId)
                .status(status)
                .queuePosition(queuePosition)
                .totalWaiting(totalWaiting)
                .build();
    }
}
