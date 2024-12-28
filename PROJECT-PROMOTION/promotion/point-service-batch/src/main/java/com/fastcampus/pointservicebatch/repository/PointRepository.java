package com.fastcampus.pointservicebatch.repository;

import com.fastcampus.pointservicebatch.domain.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PointRepository extends JpaRepository<Point, Long> {
    @Query("SELECT p FROM Point p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<Point> findAllByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
