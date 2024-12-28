package com.fastcampus.pointservice.repository;

import com.fastcampus.pointservice.domain.PointBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface PointBalanceRepository extends JpaRepository<PointBalance, Long> {
    @Lock(LockModeType.OPTIMISTIC)
    Optional<PointBalance> findByUserId(Long userId);
}
