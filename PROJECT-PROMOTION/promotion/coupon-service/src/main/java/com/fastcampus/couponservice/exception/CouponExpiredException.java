package com.fastcampus.couponservice.exception;

public class CouponExpiredException extends RuntimeException {
    public CouponExpiredException(String message) {
        super(message);
    }

    public CouponExpiredException(Long couponId) {
        super("만료된 쿠폰입니다: " + couponId);
    }
}
