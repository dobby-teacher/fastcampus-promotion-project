package com.fastcampus.couponservice.service.v3;

import com.fastcampus.couponservice.aop.CouponMetered;
import com.fastcampus.couponservice.config.UserIdInterceptor;
import com.fastcampus.couponservice.domain.Coupon;
import com.fastcampus.couponservice.domain.CouponPolicy;
import com.fastcampus.couponservice.dto.v3.CouponDto;
import com.fastcampus.couponservice.exception.CouponIssueException;
import com.fastcampus.couponservice.exception.CouponNotFoundException;
import com.fastcampus.couponservice.repository.CouponRepository;
import com.fastcampus.couponservice.service.v2.CouponPolicyService;
import com.fastcampus.couponservice.service.v2.CouponStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service("couponServiceV3")
@RequiredArgsConstructor
public class CouponService {
    private static final String COUPON_QUANTITY_KEY = "coupon:quantity:";
    private static final String COUPON_LOCK_KEY = "coupon:lock:";
    private static final long LOCK_WAIT_TIME = 3L;
    private static final long LOCK_LEASE_TIME = 5L;

    private final RedissonClient redissonClient;
    private final CouponRepository couponRepository;
    private final CouponProducer couponProducer;
    private final CouponStateService couponStateService;
    private final CouponPolicyService couponPolicyService;

    @Transactional(readOnly = true)
    @CouponMetered(version = "v3")
    public void requestCouponIssue(CouponDto.IssueRequest request) {
        String quantityKey = COUPON_QUANTITY_KEY + request.getCouponPolicyId();
        String lockKey = COUPON_LOCK_KEY + request.getCouponPolicyId();
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            boolean isLocked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new CouponIssueException("쿠폰 발급 요청이 많아 처리할 수 없습니다. 잠시 후 다시 시도해주세요.");
            }
            
            CouponPolicy couponPolicy = couponPolicyService.getCouponPolicy(request.getCouponPolicyId());
            if (couponPolicy == null) {
                throw new IllegalArgumentException("쿠폰 정책을 찾을 수 없습니다.");
            }
            
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(couponPolicy.getStartTime()) || now.isAfter(couponPolicy.getEndTime())) {
                throw new IllegalStateException("쿠폰 발급 기간이 아닙니다.");
            }
            
            // 수량 체크 및 감소
            RAtomicLong atomicQuantity = redissonClient.getAtomicLong(quantityKey);
            long remainingQuantity = atomicQuantity.decrementAndGet();
            
            if (remainingQuantity < 0) {
                atomicQuantity.incrementAndGet();
                throw new CouponIssueException("쿠폰이 모두 소진되었습니다.");
            }

            // Kafka로 쿠폰 발급 요청 전송
            couponProducer.sendCouponIssueRequest(
                CouponDto.IssueMessage.builder()
                    .policyId(request.getCouponPolicyId())
                    .userId(UserIdInterceptor.getCurrentUserId())
                    .build()
            );
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CouponIssueException("쿠폰 발급 중 오류가 발생했습니다.");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional
    public void issueCoupon(CouponDto.IssueMessage message) {
        try {
            CouponPolicy policy = couponPolicyService.getCouponPolicy(message.getPolicyId());
            if (policy == null) {
                throw new IllegalArgumentException("쿠폰 정책을 찾을 수 없습니다.");
            }
            
            Coupon coupon = couponRepository.save(Coupon.builder()
                    .couponPolicy(policy)
                    .userId(message.getUserId())
                    .couponCode(generateCouponCode())
                    .build());
            
            log.info("Coupon issued successfully: policyId={}, userId={}", message.getPolicyId(), message.getUserId());
            
        } catch (Exception e) {
            log.error("Failed to issue coupon: {}", e.getMessage());
            throw e;
        }
    }

    @Transactional
    public Coupon useCoupon(Long couponId, Long orderId) {
        Coupon coupon = couponRepository.findByIdWithLock(couponId)
                .orElseThrow(() -> new CouponNotFoundException("쿠폰을 찾을 수 없습니다."));

        coupon.use(orderId);
        couponStateService.updateCouponState(coupon);

        return coupon;
    }

    @Transactional
    public Coupon cancelCoupon(Long couponId) {
        Coupon coupon = couponRepository.findByIdAndUserId(couponId, UserIdInterceptor.getCurrentUserId())
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));

        if (!coupon.isUsed()) {
            throw new IllegalStateException("사용되지 않은 쿠폰은 취소할 수 없습니다.");
        }

        coupon.cancel();
        couponStateService.updateCouponState(coupon);

        return coupon;
    }

    private String generateCouponCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
