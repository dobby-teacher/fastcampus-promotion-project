package com.fastcampus.couponservice.exception;

public class CouponNotUsedException extends RuntimeException {
    public CouponNotUsedException(String message) {
        super(message);
    }

    public CouponNotUsedException(String message, Throwable cause) {
        super(message, cause);
    }
}
