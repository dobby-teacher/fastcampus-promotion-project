package com.fastcampus.couponservice.exception;

public class CouponAlreadyUsedException extends RuntimeException {
    public CouponAlreadyUsedException(String message) {
        super(message);
    }

    public CouponAlreadyUsedException(Long couponId) {
        super("이미 사용된 쿠폰입니다: " + couponId);
    }
}
