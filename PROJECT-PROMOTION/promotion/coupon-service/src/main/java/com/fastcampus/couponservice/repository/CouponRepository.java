package com.fastcampus.couponservice.repository;

import com.fastcampus.couponservice.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.couponPolicy.id = :policyId")
    Long countByCouponPolicyId(@Param("policyId") Long policyId);

    Page<Coupon> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Coupon.Status status, Pageable pageable);

    /**
     * PESSIMISTIC_WRITE 를 사용하는 이유는 데이터의 일관성을 보장하기 위함
     * 동시에 여러 트랜잭션이 동일한 데이터를 수정하려고 할 때 충돌을 방지하고, 데이터 무결성을 유지하기 위해 사용
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.id = :id")
    Optional<Coupon> findByIdWithLock(@Param("id") Long id);

}
