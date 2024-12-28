package com.fastcampus.pointservicebatch.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "daily_point_reports")
public class DailyPointReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "earn_amount", nullable = false)
    private Long earnAmount;

    @Column(name = "use_amount", nullable = false)
    private Long useAmount;

    @Column(name = "cancel_amount", nullable = false)
    private Long cancelAmount;

    @Column(name = "net_amount", nullable = false)
    private Long netAmount;

    @Builder
    public DailyPointReport(Long userId, LocalDate reportDate, Long earnAmount, 
                          Long useAmount, Long cancelAmount) {
        this.userId = userId;
        this.reportDate = reportDate;
        this.earnAmount = earnAmount;
        this.useAmount = useAmount;
        this.cancelAmount = cancelAmount;
        this.netAmount = earnAmount - useAmount + cancelAmount;
    }
}
