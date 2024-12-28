package com.fastcampus.pointservice.service.v1;

import com.fastcampus.pointservice.domain.PointBalance;
import com.fastcampus.pointservice.repository.PointBalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class PointServiceIntegrationTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private PointBalanceRepository pointBalanceRepository;

    @Test
    @Transactional()
    void testOptimisticLock() throws InterruptedException {
        // Given
        Long userId = 1L;

        // When
        Runnable task1 = () -> {
            pointService.earnPoints(userId, 30L, "Task1");
        };
        Runnable task2 = () -> {
            pointService.earnPoints(userId, 50L, "Task2");
        };

        Thread thread1 = new Thread(task1);
        Thread thread2 = new Thread(task2);

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        // Then
        PointBalance pointBalance = pointBalanceRepository.findByUserId(userId).orElseThrow();
        assertThat(pointBalance.getBalance()).isEqualTo(80); // 최종 잔액 확인
    }
}
