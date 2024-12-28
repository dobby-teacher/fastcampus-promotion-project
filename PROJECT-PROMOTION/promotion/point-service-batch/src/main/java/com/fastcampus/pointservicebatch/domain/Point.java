package com.fastcampus.pointservicebatch.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "points")
public class Point {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PointType type;

    private String description;

    @Column(name = "balance_snapshot", nullable = false)
    private Long balanceSnapshot;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public Point(Long userId, Long amount, PointType type, String description, Long balanceSnapshot, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.balanceSnapshot = balanceSnapshot;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
