package com.fastcampus.pointservice.service.v1;

import com.fastcampus.pointservice.domain.Point;
import com.fastcampus.pointservice.domain.PointBalance;
import com.fastcampus.pointservice.domain.PointType;
import com.fastcampus.pointservice.repository.PointBalanceRepository;
import com.fastcampus.pointservice.repository.PointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private PointBalanceRepository pointBalanceRepository;

    @Mock
    private PointRepository pointRepository;

    @InjectMocks
    private PointService pointService;

    private Long userId;
    private Long amount;
    private String description;
    private PointBalance pointBalance;
    private Point point;

    @BeforeEach
    void setUp() {
        userId = 1L;
        amount = 1000L;
        description = "Test points";
        
        pointBalance = PointBalance.builder()
                .userId(userId)
                .balance(1000L) // 초기 잔액을 1000으로 설정
                .build();
        
        point = Point.builder()
                .userId(userId)
                .amount(amount)
                .type(PointType.EARNED)
                .description(description)
                .balanceSnapshot(amount)
                .build();
    }

    @Test
    @DisplayName("포인트 적립 성공 테스트")
    void earnPointsSuccess() {
        // given
        given(pointBalanceRepository.findByUserId(userId))
                .willReturn(Optional.of(pointBalance));
        given(pointBalanceRepository.save(any(PointBalance.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(pointRepository.save(any(Point.class)))
                .willAnswer(invocation -> {
                    Point savedPoint = invocation.getArgument(0);
                    return Point.builder()
                            .userId(savedPoint.getUserId())
                            .amount(savedPoint.getAmount())
                            .type(savedPoint.getType())
                            .description(savedPoint.getDescription())
                            .balanceSnapshot(savedPoint.getBalanceSnapshot())
                            .pointBalance(savedPoint.getPointBalance())
                            .build();
                });

        // when
        Point result = pointService.earnPoints(userId, amount, description);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getType()).isEqualTo(PointType.EARNED);
        verify(pointBalanceRepository, times(1)).save(any(PointBalance.class));
        verify(pointRepository, times(1)).save(any(Point.class));
    }

    @Test
    @DisplayName("새로운 사용자의 포인트 적립 성공 테스트")
    void earnPointsNewUserSuccess() {
        // given
        given(pointBalanceRepository.findByUserId(userId))
                .willReturn(Optional.empty());
        given(pointBalanceRepository.save(any(PointBalance.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(pointRepository.save(any(Point.class)))
                .willAnswer(invocation -> {
                    Point savedPoint = invocation.getArgument(0);
                    return Point.builder()
                            .userId(savedPoint.getUserId())
                            .amount(savedPoint.getAmount())
                            .type(savedPoint.getType())
                            .description(savedPoint.getDescription())
                            .balanceSnapshot(savedPoint.getBalanceSnapshot())
                            .pointBalance(savedPoint.getPointBalance())
                            .build();
                });

        // when
        Point result = pointService.earnPoints(userId, amount, description);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        verify(pointBalanceRepository, times(1)).save(any(PointBalance.class));
        verify(pointRepository, times(1)).save(any(Point.class));
    }

    @Test
    @DisplayName("포인트 사용 성공 테스트")
    void usePointsSuccess() {
        // given
        pointBalance.addBalance(amount); // 충분한 잔액 설정
        given(pointBalanceRepository.findByUserId(userId))
                .willReturn(Optional.of(pointBalance));
        given(pointBalanceRepository.save(any(PointBalance.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(pointRepository.save(any(Point.class)))
                .willAnswer(invocation -> {
                    Point savedPoint = invocation.getArgument(0);
                    return Point.builder()
                            .userId(savedPoint.getUserId())
                            .amount(savedPoint.getAmount())
                            .type(savedPoint.getType())
                            .description(savedPoint.getDescription())
                            .balanceSnapshot(savedPoint.getBalanceSnapshot())
                            .pointBalance(savedPoint.getPointBalance())
                            .build();
                });

        // when
        Point result = pointService.usePoints(userId, amount, description);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(PointType.USED);
        assertThat(result.getAmount()).isEqualTo(amount);
        verify(pointBalanceRepository, times(1)).save(any(PointBalance.class));
        verify(pointRepository, times(1)).save(any(Point.class));
    }

    @Test
    @DisplayName("잔액 부족으로 포인트 사용 실패 테스트")
    void usePointsInsufficientBalance() {
        // given
        given(pointBalanceRepository.findByUserId(userId))
                .willReturn(Optional.of(pointBalance)); // 초기 잔액 1000

        // when & then
        assertThatThrownBy(() -> pointService.usePoints(userId, amount * 2, description))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Insufficient point balance");
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 포인트 사용 시도 테스트")
    void usePointsUserNotFound() {
        // given
        given(pointBalanceRepository.findByUserId(userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> pointService.usePoints(userId, amount, description))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("포인트 잔액 조회 테스트")
    void getBalanceSuccess() {
        // given
        PointBalance balance = PointBalance.builder()
                .userId(userId)
                .balance(1000L)
                .build();
        
        given(pointBalanceRepository.findByUserId(userId))
                .willReturn(Optional.of(balance));

        // when
        Long result = pointService.getBalance(userId);

        // then
        assertThat(result).isEqualTo(1000L);
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 포인트 잔액 조회 테스트")
    void getBalanceUserNotFound() {
        // given
        given(pointBalanceRepository.findByUserId(userId))
                .willReturn(Optional.empty());

        // when
        Long result = pointService.getBalance(userId);

        // then
        assertThat(result).isZero();
    }

    @Test
    @DisplayName("포인트 내역 조회 테스트")
    void getPointHistorySuccess() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<Point> points = Arrays.asList(
                Point.builder()
                        .userId(userId)
                        .amount(amount)
                        .type(PointType.EARNED)
                        .description("First earn")
                        .balanceSnapshot(amount)
                        .build(),
                Point.builder()
                        .userId(userId)
                        .amount(amount / 2)
                        .type(PointType.USED)
                        .description("First spend")
                        .balanceSnapshot(amount / 2)
                        .build()
        );
        Page<Point> pointPage = new PageImpl<>(points, pageable, points.size());
        
        given(pointRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable))
                .willReturn(pointPage);

        // when
        Page<Point> result = pointService.getPointHistory(userId, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getType()).isEqualTo(PointType.EARNED);
        assertThat(result.getContent().get(1).getType()).isEqualTo(PointType.USED);
    }

    @Test
    @DisplayName("포인트 적립 취소 성공 테스트")
    void cancelEarnedPointsSuccess() {
        // given
        Point originalPoint = Point.builder()
                .userId(userId)
                .amount(amount)
                .type(PointType.EARNED)
                .description("Original earn")
                .balanceSnapshot(1000L)
                .pointBalance(pointBalance)
                .build();

        given(pointRepository.findById(1L))
                .willReturn(Optional.of(originalPoint));
        given(pointBalanceRepository.findByUserId(userId))
                .willReturn(Optional.of(pointBalance));
        given(pointBalanceRepository.save(any(PointBalance.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(pointRepository.save(any(Point.class)))
                .willAnswer(invocation -> {
                    Point savedPoint = invocation.getArgument(0);
                    return Point.builder()
                            .userId(savedPoint.getUserId())
                            .amount(savedPoint.getAmount())
                            .type(savedPoint.getType())
                            .description(savedPoint.getDescription())
                            .balanceSnapshot(savedPoint.getBalanceSnapshot())
                            .pointBalance(savedPoint.getPointBalance())
                            .build();
                });

        // when
        Point result = pointService.cancelPoints(1L, "Cancel test");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(PointType.CANCELED);
        assertThat(result.getAmount()).isEqualTo(amount);
        verify(pointBalanceRepository, times(1)).save(any(PointBalance.class));
        verify(pointRepository, times(1)).save(any(Point.class));
    }

    @Test
    @DisplayName("포인트 사용 취소 성공 테스트")
    void cancelSpentPointsSuccess() {
        // given
        Point originalPoint = Point.builder()
                .userId(userId)
                .amount(amount)
                .type(PointType.USED)
                .description("Original spend")
                .balanceSnapshot(1000L)
                .pointBalance(pointBalance)
                .build();

        given(pointRepository.findById(1L))
                .willReturn(Optional.of(originalPoint));
        given(pointBalanceRepository.findByUserId(userId))
                .willReturn(Optional.of(pointBalance));
        given(pointBalanceRepository.save(any(PointBalance.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(pointRepository.save(any(Point.class)))
                .willAnswer(invocation -> {
                    Point savedPoint = invocation.getArgument(0);
                    return Point.builder()
                            .userId(savedPoint.getUserId())
                            .amount(savedPoint.getAmount())
                            .type(savedPoint.getType())
                            .description(savedPoint.getDescription())
                            .balanceSnapshot(savedPoint.getBalanceSnapshot())
                            .pointBalance(savedPoint.getPointBalance())
                            .build();
                });

        // when
        Point result = pointService.cancelPoints(1L, "Cancel test");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(PointType.CANCELED);
        assertThat(result.getAmount()).isEqualTo(amount);
        verify(pointBalanceRepository, times(1)).save(any(PointBalance.class));
        verify(pointRepository, times(1)).save(any(Point.class));
    }

    @Test
    @DisplayName("이미 취소된 포인트 취소 시도 테스트")
    void cancelAlreadyCanceledPoints() {
        // given
        Point canceledPoint = Point.builder()
                .userId(userId)
                .amount(amount)
                .type(PointType.CANCELED)
                .description("Already canceled")
                .balanceSnapshot(0L)
                .pointBalance(pointBalance)
                .build();

        given(pointRepository.findById(1L))
                .willReturn(Optional.of(canceledPoint));

        // when & then
        assertThatThrownBy(() -> pointService.cancelPoints(1L, "Cancel test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Already canceled point");
    }

    @Test
    @DisplayName("존재하지 않는 포인트 취소 시도 테스트")
    void cancelNonExistentPoints() {
        // given
        given(pointRepository.findById(1L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> pointService.cancelPoints(1L, "Cancel test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Point not found");
    }
}
