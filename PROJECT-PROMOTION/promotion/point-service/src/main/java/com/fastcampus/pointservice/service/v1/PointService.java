package com.fastcampus.pointservice.service.v1;

import com.fastcampus.pointservice.domain.Point;
import com.fastcampus.pointservice.domain.PointBalance;
import com.fastcampus.pointservice.domain.PointType;
import com.fastcampus.pointservice.repository.PointBalanceRepository;
import com.fastcampus.pointservice.repository.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointService {
    private final PointBalanceRepository pointBalanceRepository;
    private final PointRepository pointRepository;

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Point earnPoints(Long userId, Long amount, String description) {
        PointBalance pointBalance = pointBalanceRepository.findByUserId(userId)
                .orElseGet(() -> PointBalance.builder()
                        .userId(userId)
                        .balance(0L)
                        .build());

        pointBalance.addBalance(amount);
        pointBalance = pointBalanceRepository.save(pointBalance);

        Point point = Point.builder()
                .userId(userId)
                .amount(amount)
                .type(PointType.EARNED)
                .description(description)
                .balanceSnapshot(pointBalance.getBalance())
                .pointBalance(pointBalance)
                .build();
        return pointRepository.save(point);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Point usePoints(Long userId, Long amount, String description) {
        PointBalance pointBalance = pointBalanceRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        pointBalance.subtractBalance(amount);
        pointBalance = pointBalanceRepository.save(pointBalance);

        Point point = Point.builder()
                .userId(userId)
                .amount(amount)
                .type(PointType.USED)
                .description(description)
                .balanceSnapshot(pointBalance.getBalance())
                .pointBalance(pointBalance)
                .build();
        return pointRepository.save(point);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Point cancelPoints(Long pointId, String description) {
        Point originalPoint = pointRepository.findById(pointId)
                .orElseThrow(() -> new IllegalArgumentException("Point not found"));

        if (originalPoint.getType() == PointType.CANCELED) {
            throw new IllegalArgumentException("Already canceled point");
        }

        PointBalance pointBalance = pointBalanceRepository.findByUserId(originalPoint.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Long currentBalance = pointBalance.getBalance();
        Long newBalance;

        // 원래 포인트 타입에 따라 취소 처리
        if (originalPoint.getType() == PointType.EARNED) {
            if (currentBalance < originalPoint.getAmount()) {
                throw new IllegalArgumentException("Cannot cancel earned points: insufficient balance");
            }
            newBalance = currentBalance - originalPoint.getAmount();
        } else if (originalPoint.getType() == PointType.USED) {
            newBalance = currentBalance + originalPoint.getAmount();
        } else {
            throw new IllegalArgumentException("Invalid point type for cancellation");
        }

        pointBalance.setBalance(newBalance);
        pointBalance = pointBalanceRepository.save(pointBalance);

        Point cancelPoint = Point.builder()
                .userId(originalPoint.getUserId())
                .amount(originalPoint.getAmount())
                .type(PointType.CANCELED)
                .description(description)
                .balanceSnapshot(pointBalance.getBalance())
                .pointBalance(pointBalance)
                .build();

        return pointRepository.save(cancelPoint);
    }

    @Transactional(readOnly = true)
    public Long getBalance(Long userId) {
        return pointBalanceRepository.findByUserId(userId)
                .map(PointBalance::getBalance)
                .orElse(0L);
    }

    @Transactional(readOnly = true)
    public Page<Point> getPointHistory(Long userId, Pageable pageable) {
        return pointRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}
