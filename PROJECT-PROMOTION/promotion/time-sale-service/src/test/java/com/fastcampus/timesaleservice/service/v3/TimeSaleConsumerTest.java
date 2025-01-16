package com.fastcampus.timesaleservice.service.v3;

import com.fastcampus.timesaleservice.domain.Product;
import com.fastcampus.timesaleservice.domain.TimeSale;
import com.fastcampus.timesaleservice.domain.TimeSaleOrder;
import com.fastcampus.timesaleservice.domain.TimeSaleStatus;
import com.fastcampus.timesaleservice.dto.PurchaseRequestMessage;
import com.fastcampus.timesaleservice.repository.TimeSaleOrderRepository;
import com.fastcampus.timesaleservice.repository.TimeSaleRepository;
import com.fastcampus.timesaleservice.service.v2.TimeSaleRedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeSaleConsumerTest {

    @Mock
    private TimeSaleRedisService timeSaleRedisService;

    @Mock
    private TimeSaleOrderRepository timeSaleOrderRepository;

    @Mock
    private TimeSaleRepository timeSaleRepository;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RBucket<String> resultBucket;

    @Mock
    private RAtomicLong totalCounter;

    @InjectMocks
    private TimeSaleConsumer timeSaleConsumer;

    private TimeSale timeSale;
    private TimeSaleOrder order;
    private Product product;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        
        when(redissonClient.<String>getBucket(anyString())).thenReturn(resultBucket);
        when(redissonClient.getAtomicLong(anyString())).thenReturn(totalCounter);

        product = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(10000L)
                .build();

        timeSale = TimeSale.builder()
                .id(1L)
                .product(product)
                .status(TimeSaleStatus.ACTIVE)
                .quantity(100L)
                .remainingQuantity(100L)
                .discountPrice(5000L)
                .startAt(now.minusHours(1))
                .endAt(now.plusHours(1))
                .build();

        order = TimeSaleOrder.builder()
                .id(1L)
                .userId(1L)
                .timeSale(timeSale)
                .quantity(2L)
                .discountPrice(5000L)
                .build();
    }

    @Test
    @DisplayName("구매 요청 처리 성공")
    void consumePurchaseRequest_Success() {
        // given
        PurchaseRequestMessage message = PurchaseRequestMessage.builder()
                .requestId("test-request-id")
                .timeSaleId(1L)
                .userId(1L)
                .quantity(2L)
                .build();

        when(timeSaleRepository.findById(1L)).thenReturn(Optional.of(timeSale));
        when(timeSaleRepository.save(any(TimeSale.class))).thenReturn(timeSale);
        when(timeSaleOrderRepository.save(any(TimeSaleOrder.class))).thenReturn(order);

        // when
        timeSaleConsumer.consumePurchaseRequest(message);

        // then
        verify(resultBucket).set("SUCCESS");
        verify(timeSaleRedisService).saveToRedis(timeSale);
        verify(totalCounter).decrementAndGet();
        verify(timeSaleRepository).findById(1L);
        verify(timeSaleRepository).save(any(TimeSale.class));
    }

    @Test
    @DisplayName("구매 요청 처리 실패 - 타임세일 없음")
    void consumePurchaseRequest_TimeSaleNotFound() {
        // given
        PurchaseRequestMessage message = PurchaseRequestMessage.builder()
                .requestId("test-request-id")
                .timeSaleId(1L)
                .userId(1L)
                .quantity(2L)
                .build();

        when(timeSaleRepository.findById(1L)).thenReturn(Optional.empty());

        // when
        timeSaleConsumer.consumePurchaseRequest(message);

        // then
        verify(resultBucket).set("FAIL");
        verify(totalCounter).decrementAndGet();
        verify(timeSaleOrderRepository, never()).save(any(TimeSaleOrder.class));
        verify(timeSaleRepository).findById(1L);
        verify(timeSaleRepository, never()).save(any(TimeSale.class));
    }

    @Test
    @DisplayName("구매 요청 처리 실패 - 재고 부족")
    void consumePurchaseRequest_OutOfStock() {
        // given
        TimeSale timeSaleWithLowStock = TimeSale.builder()
                .id(1L)
                .product(product)
                .status(TimeSaleStatus.ACTIVE)
                .quantity(100L)
                .remainingQuantity(1L)
                .discountPrice(5000L)
                .startAt(now.minusHours(1))
                .endAt(now.plusHours(1))
                .build();

        PurchaseRequestMessage message = PurchaseRequestMessage.builder()
                .requestId("test-request-id")
                .timeSaleId(1L)
                .userId(1L)
                .quantity(2L)
                .build();

        when(timeSaleRepository.findById(1L)).thenReturn(Optional.of(timeSaleWithLowStock));

        // when
        timeSaleConsumer.consumePurchaseRequest(message);

        // then
        verify(resultBucket).set("FAIL");
        verify(totalCounter).decrementAndGet();
        verify(timeSaleOrderRepository, never()).save(any(TimeSaleOrder.class));
        verify(timeSaleRepository).findById(1L);
        verify(timeSaleRepository, never()).save(any(TimeSale.class));
    }
}
