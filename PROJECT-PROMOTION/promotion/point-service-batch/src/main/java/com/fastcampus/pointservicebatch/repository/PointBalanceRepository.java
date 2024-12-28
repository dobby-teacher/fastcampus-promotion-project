package com.fastcampus.pointservicebatch.repository;

import com.fastcampus.pointservicebatch.domain.PointBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointBalanceRepository extends JpaRepository<PointBalance, Long> {
    Optional<PointBalance> findByUserId(Long userId);
}
