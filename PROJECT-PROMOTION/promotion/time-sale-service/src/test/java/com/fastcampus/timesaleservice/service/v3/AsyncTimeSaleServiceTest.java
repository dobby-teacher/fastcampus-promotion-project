package com.fastcampus.timesaleservice.service.v3;

import com.fastcampus.timesaleservice.dto.TimeSaleDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AsyncTimeSaleServiceTest {

    @Mock
    private TimeSaleProducer timeSaleProducer;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RBucket<String> rBucket;

    private AsyncTimeSaleService asyncTimeSaleService;

    @BeforeEach
    void setUp() {
        asyncTimeSaleService = new AsyncTimeSaleService(null, timeSaleProducer, redissonClient);
    }

    @Test
    @DisplayName("비동기 구매 요청 성공")
    void purchaseTimeSale_Success() {
        // given
        String expectedRequestId = "test-request-id";
        TimeSaleDto.PurchaseRequest request = TimeSaleDto.PurchaseRequest.builder()
                .userId(1L)
                .quantity(1L)
                .build();
        given(timeSaleProducer.sendPurchaseRequest(1L, 1L, 1L)).willReturn(expectedRequestId);

        // when
        String requestId = asyncTimeSaleService.purchaseTimeSale(1L, request);

        // then
        assertThat(requestId).isEqualTo(expectedRequestId);
        verify(timeSaleProducer).sendPurchaseRequest(1L, 1L, 1L);
    }

    @Test
    @DisplayName("구매 결과 조회 - 대기 중")
    void getPurchaseResult_Pending() {
        // given
        Long timeSaleId = 1L;
        String requestId = "test-request-id";
        given(redissonClient.<String>getBucket(anyString())).willReturn(rBucket);
        given(rBucket.get()).willReturn(null);
        given(timeSaleProducer.getQueuePosition(timeSaleId, requestId)).willReturn(5);
        given(timeSaleProducer.getTotalWaiting(timeSaleId)).willReturn(10L);

        // when
        TimeSaleDto.AsyncPurchaseResponse response = asyncTimeSaleService.getPurchaseResult(timeSaleId, requestId);

        // then
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getQueuePosition()).isEqualTo(5);
        assertThat(response.getTotalWaiting()).isEqualTo(10L);
    }

    @Test
    @DisplayName("구매 결과 조회 - 성공")
    void getPurchaseResult_Success() {
        // given
        Long timeSaleId = 1L;
        String requestId = "test-request-id";
        given(redissonClient.<String>getBucket(anyString())).willReturn(rBucket);
        given(rBucket.get()).willReturn("SUCCESS");

        // when
        TimeSaleDto.AsyncPurchaseResponse response = asyncTimeSaleService.getPurchaseResult(timeSaleId, requestId);

        // then
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getQueuePosition()).isNull();
        assertThat(response.getTotalWaiting()).isEqualTo(0L);
    }

    @Test
    @DisplayName("구매 결과 조회 - 실패")
    void getPurchaseResult_Failed() {
        // given
        Long timeSaleId = 1L;
        String requestId = "test-request-id";
        given(redissonClient.<String>getBucket(anyString())).willReturn(rBucket);
        given(rBucket.get()).willReturn("FAILED:Out of stock");

        // when
        TimeSaleDto.AsyncPurchaseResponse response = asyncTimeSaleService.getPurchaseResult(timeSaleId, requestId);

        // then
        assertThat(response.getStatus()).isEqualTo("FAILED:Out of stock");
        assertThat(response.getQueuePosition()).isNull();
        assertThat(response.getTotalWaiting()).isEqualTo(0L);
    }
}
