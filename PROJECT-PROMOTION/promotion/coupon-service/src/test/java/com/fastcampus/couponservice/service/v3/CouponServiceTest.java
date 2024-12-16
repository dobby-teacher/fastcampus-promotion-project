package com.fastcampus.couponservice.service.v3;

import com.fastcampus.couponservice.config.UserIdInterceptor;
import com.fastcampus.couponservice.domain.Coupon;
import com.fastcampus.couponservice.domain.CouponPolicy;
import com.fastcampus.couponservice.dto.v3.CouponDto;
import com.fastcampus.couponservice.exception.CouponIssueException;
import com.fastcampus.couponservice.exception.CouponNotFoundException;
import com.fastcampus.couponservice.repository.CouponRepository;
import com.fastcampus.couponservice.service.v2.CouponPolicyService;
import com.fastcampus.couponservice.service.v2.CouponStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @InjectMocks
    private CouponService couponService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponProducer couponProducer;

    @Mock
    private CouponPolicyService couponPolicyService;

    @Mock
    private CouponStateService couponStateService;

    @Mock
    private RLock rLock;

    @Mock
    private RAtomicLong atomicLong;

    private CouponPolicy couponPolicy;
    private Coupon coupon;

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_COUPON_ID = 1L;
    private static final Long TEST_POLICY_ID = 1L;

    @BeforeEach
    void setUp() {
        couponPolicy = CouponPolicy.builder()
                .id(TEST_POLICY_ID)
                .startTime(LocalDateTime.now().minusDays(1))
                .endTime(LocalDateTime.now().plusDays(1))
                .build();

        coupon = Coupon.builder()
                .id(TEST_COUPON_ID)
                .userId(TEST_USER_ID)
                .couponPolicy(couponPolicy)
                .couponCode("TEST123")
                .build();
    }

    @Test
    @DisplayName("쿠폰 발급 성공")
    void requestCouponIssue_Success() throws InterruptedException {
        // Given
        CouponDto.IssueRequest request = new CouponDto.IssueRequest(TEST_POLICY_ID);

        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(redissonClient.getAtomicLong(anyString())).thenReturn(atomicLong);
        when(atomicLong.decrementAndGet()).thenReturn(99L);
        when(couponPolicyService.getCouponPolicy(TEST_POLICY_ID)).thenReturn(couponPolicy);

        try (MockedStatic<UserIdInterceptor> mockedStatic = mockStatic(UserIdInterceptor.class)) {
            mockedStatic.when(UserIdInterceptor::getCurrentUserId).thenReturn(TEST_USER_ID);

            // When
            assertThatCode(() -> couponService.requestCouponIssue(request)).doesNotThrowAnyException();

            // Then
            verify(couponProducer).sendCouponIssueRequest(any(CouponDto.IssueMessage.class));
            verify(rLock).unlock();
        }
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 락 획득 실패")
    void requestCouponIssue_Fail_LockNotAcquired() throws InterruptedException {
        // Given
        CouponDto.IssueRequest request = new CouponDto.IssueRequest(TEST_POLICY_ID);

        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> couponService.requestCouponIssue(request))
                .isInstanceOf(CouponIssueException.class)
                .hasMessage("쿠폰 발급 요청이 많아 처리할 수 없습니다. 잠시 후 다시 시도해주세요.");
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 수량 소진")
    void requestCouponIssue_Fail_NoQuantityLeft() throws InterruptedException {
        // Given
        CouponDto.IssueRequest request = new CouponDto.IssueRequest(TEST_POLICY_ID);

        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(redissonClient.getAtomicLong(anyString())).thenReturn(atomicLong);
        when(atomicLong.decrementAndGet()).thenReturn(-1L);
        when(couponPolicyService.getCouponPolicy(TEST_POLICY_ID)).thenReturn(couponPolicy);

        // When & Then
        assertThatThrownBy(() -> couponService.requestCouponIssue(request))
                .isInstanceOf(CouponIssueException.class)
                .hasMessage("쿠폰이 모두 소진되었습니다.");

        verify(atomicLong).incrementAndGet();
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 발급 기간 아님")
    void requestCouponIssue_Fail_InvalidPeriod() throws InterruptedException {
        // Given
        CouponDto.IssueRequest request = new CouponDto.IssueRequest(TEST_POLICY_ID);

        CouponPolicy expiredPolicy = CouponPolicy.builder()
                .id(TEST_POLICY_ID)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(2))
                .build();

        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(couponPolicyService.getCouponPolicy(TEST_POLICY_ID)).thenReturn(expiredPolicy);

        // When & Then
        assertThatThrownBy(() -> couponService.requestCouponIssue(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("쿠폰 발급 기간이 아닙니다.");

        verify(rLock).unlock();
    }

    @Test
    @DisplayName("쿠폰 사용 성공")
    void useCoupon_Success() {
        // Given
        when(couponRepository.findByIdWithLock(TEST_COUPON_ID)).thenReturn(Optional.of(coupon));

        // When
        Coupon result = couponService.useCoupon(TEST_COUPON_ID, 200L);

        // Then
        verify(couponStateService).updateCouponState(coupon);
        assertThat(result).isEqualTo(coupon);
    }

    @Test
    @DisplayName("쿠폰 사용 실패 - 쿠폰 없음")
    void useCoupon_Fail_CouponNotFound() {
        // Given
        when(couponRepository.findByIdWithLock(TEST_COUPON_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> couponService.useCoupon(TEST_COUPON_ID, 200L))
                .isInstanceOf(CouponNotFoundException.class)
                .hasMessage("쿠폰을 찾을 수 없습니다.");
    }
}

