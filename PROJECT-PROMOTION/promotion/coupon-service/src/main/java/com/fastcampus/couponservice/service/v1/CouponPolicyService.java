package com.fastcampus.couponservice.service.v1;

import com.fastcampus.couponservice.domain.CouponPolicy;
import com.fastcampus.couponservice.dto.v1.CouponPolicyDto;
import com.fastcampus.couponservice.exception.CouponPolicyNotFoundException;
import com.fastcampus.couponservice.repository.CouponPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponPolicyService {

    private final CouponPolicyRepository couponPolicyRepository;

    @Transactional
    public CouponPolicyDto.Response createCouponPolicy(CouponPolicyDto.CreateRequest request) {
        CouponPolicy couponPolicy = request.toEntity();
        CouponPolicy savedPolicy = couponPolicyRepository.save(couponPolicy);

        return CouponPolicyDto.Response.from(savedPolicy);
    }

    @Transactional(readOnly = true)
    public CouponPolicyDto.Response getCouponPolicy(Long id) {
        CouponPolicy couponPolicy = couponPolicyRepository.findById(id)
                .orElseThrow(() -> new CouponPolicyNotFoundException("쿠폰 정책을 찾을 수 없습니다."));
        return CouponPolicyDto.Response.from(couponPolicy);
    }

    @Transactional(readOnly = true)
    public List<CouponPolicyDto.Response> getAllCouponPolicies() {
        return couponPolicyRepository.findAll().stream()
                .map(CouponPolicyDto.Response::from)
                .collect(Collectors.toList());
    }
}
