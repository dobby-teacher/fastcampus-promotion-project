package com.fastcampus.timesaleservice.service.v1;

import com.fastcampus.timesaleservice.domain.Product;
import com.fastcampus.timesaleservice.domain.TimeSale;
import com.fastcampus.timesaleservice.domain.TimeSaleOrder;
import com.fastcampus.timesaleservice.domain.TimeSaleStatus;
import com.fastcampus.timesaleservice.dto.TimeSaleDto;
import com.fastcampus.timesaleservice.repository.ProductRepository;
import com.fastcampus.timesaleservice.repository.TimeSaleOrderRepository;
import com.fastcampus.timesaleservice.repository.TimeSaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeSaleServiceTest {

    @InjectMocks
    private TimeSaleService timeSaleService;

    @Mock
    private TimeSaleRepository timeSaleRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TimeSaleOrderRepository timeSaleOrderRepository;

    private Product product;
    private TimeSale timeSale;
    private TimeSaleDto.CreateRequest createRequest;
    private TimeSaleDto.PurchaseRequest purchaseRequest;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        
        product = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(10000L)
                .description("Test Description")
                .build();

        createRequest = TimeSaleDto.CreateRequest.builder()
                .productId(1L)
                .quantity(100L)
                .discountPrice(8000L)
                .startAt(now.plusHours(1))
                .endAt(now.plusDays(1))
                .build();

        timeSale = TimeSale.builder()
                .product(product)
                .quantity(createRequest.getQuantity())
                .discountPrice(createRequest.getDiscountPrice())
                .startAt(createRequest.getStartAt())
                .endAt(createRequest.getEndAt())
                .build();

        purchaseRequest = TimeSaleDto.PurchaseRequest.builder()
                .userId(1L)
                .quantity(1L)
                .build();
    }

    @Test
    @DisplayName("타임세일 생성 성공")
    void createTimeSale_Success() {
        // given
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(timeSaleRepository.save(any(TimeSale.class))).thenReturn(timeSale);

        // when
        TimeSale result = timeSaleService.createTimeSale(createRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getProduct()).isEqualTo(product);
        assertThat(result.getQuantity()).isEqualTo(createRequest.getQuantity());
        verify(productRepository, times(1)).findById(1L);
        verify(timeSaleRepository, times(1)).save(any(TimeSale.class));
    }

    @Test
    @DisplayName("존재하지 않는 상품으로 타임세일 생성시 예외 발생")
    void createTimeSale_ProductNotFound() {
        // given
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> timeSaleService.createTimeSale(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Product not found");
        verify(productRepository, times(1)).findById(1L);
        verify(timeSaleRepository, never()).save(any(TimeSale.class));
    }

    @Test
    @DisplayName("진행중인 타임세일 목록 조회 성공")
    void getOngoingTimeSales_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<TimeSale> timeSalePage = new PageImpl<>(List.of(timeSale));
        when(timeSaleRepository.findAllByStartAtBeforeAndEndAtAfterAndStatus(
                any(LocalDateTime.class), eq(TimeSaleStatus.ACTIVE), eq(pageRequest)))
                .thenReturn(timeSalePage);

        // when
        Page<TimeSale> result = timeSaleService.getOngoingTimeSales(pageRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(timeSale);
        verify(timeSaleRepository, times(1))
                .findAllByStartAtBeforeAndEndAtAfterAndStatus(
                        any(LocalDateTime.class), eq(TimeSaleStatus.ACTIVE), eq(pageRequest));
    }

    @Test
    @DisplayName("타임세일 구매 성공")
    void purchaseTimeSale_Success() {
        // given
        Product product = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(10000L)
                .description("Test Description")
                .build();

        TimeSale timeSale = TimeSale.builder()
                .id(1L)
                .product(product)
                .quantity(100L)
                .remainingQuantity(100L)
                .discountPrice(5000L)
                .startAt(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusHours(1))
                .status(TimeSaleStatus.ACTIVE)
                .build();

        TimeSaleDto.PurchaseRequest request = TimeSaleDto.PurchaseRequest.builder()
                .userId(1L)
                .quantity(2L)
                .build();

        TimeSaleOrder order = TimeSaleOrder.builder()
                .id(1L)
                .userId(1L)
                .timeSale(timeSale)
                .quantity(2L)
                .discountPrice(5000L)
                .build();

        when(timeSaleRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(timeSale));
        when(timeSaleOrderRepository.save(any(TimeSaleOrder.class))).thenReturn(order);

        // when
        TimeSale response = timeSaleService.purchaseTimeSale(1L, request);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(timeSale.getRemainingQuantity()).isEqualTo(98L);

        verify(timeSaleRepository).findByIdWithPessimisticLock(1L);
        verify(timeSaleOrderRepository).save(any(TimeSaleOrder.class));
    }

    @Test
    @DisplayName("존재하지 않는 타임세일 구매시 예외 발생")
    void purchaseTimeSale_NotFound() {
        // given
        TimeSaleDto.PurchaseRequest request = TimeSaleDto.PurchaseRequest.builder()
                .userId(1L)
                .quantity(2L)
                .build();

        when(timeSaleRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.empty());

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> timeSaleService.purchaseTimeSale(1L, request));

        verify(timeSaleRepository).findByIdWithPessimisticLock(1L);
        verify(timeSaleOrderRepository, never()).save(any(TimeSaleOrder.class));
    }

    @Test
    @DisplayName("재고가 부족한 경우 구매 실패")
    void purchaseTimeSale_InsufficientQuantity() {
        // given
        Product product = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(10000L)
                .description("Test Description")
                .build();

        TimeSale timeSale = TimeSale.builder()
                .id(1L)
                .product(product)
                .quantity(100L)
                .remainingQuantity(1L)
                .discountPrice(5000L)
                .startAt(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusHours(1))
                .build();

        TimeSaleDto.PurchaseRequest request = TimeSaleDto.PurchaseRequest.builder()
                .userId(1L)
                .quantity(2L)
                .build();

        when(timeSaleRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(timeSale));

        // when & then
        assertThrows(IllegalStateException.class,
                () -> timeSaleService.purchaseTimeSale(1L, request));

        verify(timeSaleRepository).findByIdWithPessimisticLock(1L);
        verify(timeSaleOrderRepository, never()).save(any(TimeSaleOrder.class));
    }

    @Test
    @DisplayName("타임세일 기간이 아닌 경우 구매 실패")
    void purchaseTimeSale_NotInProgress() {
        // given
        TimeSale notStartedTimeSale = TimeSale.builder()
                .product(product)
                .quantity(100L)
                .discountPrice(8000L)
                .startAt(now.plusHours(1))
                .endAt(now.plusDays(1))
                .build();

        when(timeSaleRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(notStartedTimeSale));

        // when & then
        assertThatThrownBy(() -> timeSaleService.purchaseTimeSale(1L, purchaseRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Time sale is not active");
        verify(timeSaleRepository, times(1)).findByIdWithPessimisticLock(1L);
        verify(timeSaleRepository, never()).save(any(TimeSale.class));
    }
}
