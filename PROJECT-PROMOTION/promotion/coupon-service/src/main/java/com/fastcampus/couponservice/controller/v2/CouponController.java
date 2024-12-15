package com.fastcampus.couponservice.controller.v2;

import com.fastcampus.couponservice.dto.v1.CouponDto;
import com.fastcampus.couponservice.service.v2.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController("couponControllerV2")
@RequestMapping("/api/v2/coupons")
@RequiredArgsConstructor
public class CouponController {
    
    private final CouponService couponService;
    
    @PostMapping("/issue")
    public ResponseEntity<CouponDto.Response> issueCoupon(@RequestBody CouponDto.IssueRequest request) {
        return ResponseEntity.ok(couponService.issueCoupon(request));
    }
    
    @PostMapping("/{couponId}/use")
    public ResponseEntity<CouponDto.Response> useCoupon(
            @PathVariable Long couponId,
            @RequestBody CouponDto.UseRequest request) {
        return ResponseEntity.ok(couponService.useCoupon(couponId, request.getOrderId()));
    }
    
    @PostMapping("/{couponId}/cancel")
    public ResponseEntity<CouponDto.Response> cancelCoupon(@PathVariable Long couponId) {
        return ResponseEntity.ok(couponService.cancelCoupon(couponId));
    }
}
