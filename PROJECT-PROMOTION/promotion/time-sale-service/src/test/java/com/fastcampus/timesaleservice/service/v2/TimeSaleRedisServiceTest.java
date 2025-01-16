package com.fastcampus.timesaleservice.service.v2;

import com.fastcampus.timesaleservice.domain.Product;
import com.fastcampus.timesaleservice.domain.TimeSale;
import com.fastcampus.timesaleservice.domain.TimeSaleOrder;
import com.fastcampus.timesaleservice.domain.TimeSaleStatus;
import com.fastcampus.timesaleservice.dto.TimeSaleDto;
import com.fastcampus.timesaleservice.exception.TimeSaleException;
import com.fastcampus.timesaleservice.repository.ProductRepository;
import com.fastcampus.timesaleservice.repository.TimeSaleOrderRepository;
import com.fastcampus.timesaleservice.repository.TimeSaleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeSaleRedisServiceTest {

    @InjectMocks
    private TimeSaleRedisService timeSaleRedisService;

    @Mock
    private TimeSaleRepository timeSaleRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private TimeSaleOrderRepository timeSaleOrderRepository;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private RLock rLock;
    @Mock
    private RBucket<String> rBucket;

    private TimeSale timeSale;
    private Product product;
    private TimeSaleDto.PurchaseRequest purchaseRequest;
    private TimeSaleOrder order;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        
        product = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(10000L)
                .build();

        timeSale = TimeSale.builder()
                .id(1L)
                .product(product)
                .quantity(100L)
                .remainingQuantity(100L)
                .discountPrice(5000L)
                .startAt(now.minusHours(1))
                .endAt(now.plusHours(1))
                .status(TimeSaleStatus.ACTIVE)
                .build();

        purchaseRequest = TimeSaleDto.PurchaseRequest.builder()
                .userId(1L)
                .quantity(2L)
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
    @DisplayName("타임세일 생성 성공")
    void createTimeSale_Success() throws Exception {
        // given
        TimeSaleDto.CreateRequest request = TimeSaleDto.CreateRequest.builder()
                .productId(1L)
                .quantity(100L)
                .discountPrice(5000L)
                .startAt(now.minusHours(1))
                .endAt(now.plusHours(1))
                .build();

        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(timeSaleRepository.save(any(TimeSale.class))).willReturn(timeSale);
        given(objectMapper.writeValueAsString(any(TimeSale.class))).willReturn("json");
        given(redissonClient.<String>getBucket(anyString())).willReturn(rBucket);

        // when
        TimeSale result = timeSaleRedisService.createTimeSale(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getProduct().getId()).isEqualTo(1L);
        verify(timeSaleRepository).save(any(TimeSale.class));
        verify(rBucket).set(anyString());
    }

    @Test
    @DisplayName("진행 중인 타임세일 조회 성공")
    void getOngoingTimeSales_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);
        given(timeSaleRepository.findAllByStartAtBeforeAndEndAtAfterAndStatus(
                any(LocalDateTime.class), eq(TimeSaleStatus.ACTIVE), eq(pageRequest)))
                .willReturn(new PageImpl<>(List.of(timeSale)));

        // when
        var result = timeSaleRedisService.getOngoingTimeSales(pageRequest);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("타임세일 조회 성공 - Redis Cache Hit")
    void getTimeSale_Success_CacheHit() throws Exception {
        // given
        given(redissonClient.<String>getBucket(anyString())).willReturn(rBucket);
        given(rBucket.get()).willReturn("json");
        given(objectMapper.readValue(anyString(), eq(TimeSale.class))).willReturn(timeSale);

        // when
        TimeSale result = timeSaleRedisService.getTimeSale(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(timeSaleRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("타임세일 조회 성공 - Redis Cache Miss")
    void getTimeSale_Success_CacheMiss() throws Exception {
        // given
        given(redissonClient.<String>getBucket(anyString())).willReturn(rBucket);
        given(rBucket.get()).willReturn(null);
        given(timeSaleRepository.findById(1L)).willReturn(Optional.of(timeSale));
        given(objectMapper.writeValueAsString(any(TimeSale.class))).willReturn("json");

        // when
        TimeSale result = timeSaleRedisService.getTimeSale(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(timeSaleRepository).findById(1L);
        verify(rBucket).set(anyString());
    }

    @Test
    @DisplayName("타임세일 구매 성공")
    void purchaseTimeSale_Success() throws Exception {
        // given
        given(redissonClient.getLock(anyString())).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        given(redissonClient.<String>getBucket(anyString())).willReturn(rBucket);
        given(rBucket.get()).willReturn(null); // Redis에 데이터가 없는 상황
        given(timeSaleRepository.findById(1L)).willReturn(Optional.of(timeSale)); // DB에서 조회
        given(timeSaleRepository.save(any(TimeSale.class))).willReturn(timeSale);
        given(timeSaleOrderRepository.save(any(TimeSaleOrder.class))).willReturn(order);

        // when
        TimeSale result = timeSaleRedisService.purchaseTimeSale(1L, purchaseRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getRemainingQuantity()).isEqualTo(98L);
        verify(timeSaleOrderRepository).save(any(TimeSaleOrder.class));
        verify(rLock).unlock();
        verify(timeSaleRepository).findById(1L); // DB 조회 검증
    }

    @Test
    @DisplayName("타임세일 구매 실패 - 락 획득 실패")
    void purchaseTimeSale_LockFailed() throws Exception {
        // given
        given(redissonClient.getLock(anyString())).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(false);

        // when & then
        assertThatThrownBy(() -> timeSaleRedisService.purchaseTimeSale(1L, purchaseRequest))
                .isInstanceOf(TimeSaleException.class)
                .hasMessage("Failed to acquire lock");

        verify(rLock, never()).unlock();
        verify(timeSaleOrderRepository, never()).save(any(TimeSaleOrder.class));
    }

    @Test
    @DisplayName("타임세일 구매 실패 - 타임세일 없음")
    void purchaseTimeSale_NotFound() throws Exception {
        // given
        given(redissonClient.getLock(anyString())).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        given(redissonClient.<String>getBucket(anyString())).willReturn(rBucket);
        given(rBucket.get()).willReturn(null);
        given(timeSaleRepository.findById(anyLong())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> timeSaleRedisService.purchaseTimeSale(1L, purchaseRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("TimeSale not found");

        verify(rLock).unlock();
        verify(timeSaleOrderRepository, never()).save(any(TimeSaleOrder.class));
    }
}
