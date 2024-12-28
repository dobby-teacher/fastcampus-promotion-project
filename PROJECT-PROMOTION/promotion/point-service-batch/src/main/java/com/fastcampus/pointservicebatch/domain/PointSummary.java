package com.fastcampus.pointservicebatch.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PointSummary {
    private Long userId;
    private Long earnAmount;
    private Long useAmount;
    private Long cancelAmount;

    @Builder
    public PointSummary(Long userId, Long earnAmount, Long useAmount, Long cancelAmount) {
        this.userId = userId;
        this.earnAmount = earnAmount;
        this.useAmount = useAmount;
        this.cancelAmount = cancelAmount;
    }

    public void addEarnAmount(Long amount) {
        this.earnAmount += amount;
    }

    public void addUseAmount(Long amount) {
        this.useAmount += amount;
    }

    public void addCancelAmount(Long amount) {
        this.cancelAmount += amount;
    }
}
