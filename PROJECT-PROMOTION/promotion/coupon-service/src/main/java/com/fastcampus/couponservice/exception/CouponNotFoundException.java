package com.fastcampus.couponservice.exception;

public class CouponNotFoundException extends RuntimeException {
    public CouponNotFoundException(String message) {
        super(message);
    }

    public CouponNotFoundException(Long couponId) {
        super("쿠폰을 찾을 수 없습니다: " + couponId);
    }
}
