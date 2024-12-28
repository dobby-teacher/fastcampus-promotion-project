package com.fastcampus.pointservice.controller.v1;

import com.fastcampus.pointservice.config.UserIdInterceptor;
import com.fastcampus.pointservice.domain.Point;
import com.fastcampus.pointservice.dto.PointDto;
import com.fastcampus.pointservice.service.v1.PointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointController {
    
    private final PointService pointService;
    
    @PostMapping("/earn")
    public ResponseEntity<PointDto.Response> earnPoints(@Valid @RequestBody PointDto.EarnRequest request) {
        Long userId = UserIdInterceptor.getCurrentUserId();
        Point point = pointService.earnPoints(userId, request.getAmount(), request.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PointDto.Response.from(point));
    }
    
    @PostMapping("/use")
    public ResponseEntity<PointDto.Response> usePoints(@Valid @RequestBody PointDto.UseRequest request) {
        Long userId = UserIdInterceptor.getCurrentUserId();
        Point point = pointService.usePoints(userId, request.getAmount(), request.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PointDto.Response.from(point));
    }
    
    @PostMapping("/{pointId}/cancel")
    public ResponseEntity<PointDto.Response> cancelPoints(
            @PathVariable Long pointId,
            @Valid @RequestBody PointDto.CancelRequest request) {
        Point point = pointService.cancelPoints(pointId, request.getDescription());
        return ResponseEntity.ok(PointDto.Response.from(point));
    }
    
    @GetMapping("/users/{userId}/balance")
    public ResponseEntity<PointDto.BalanceResponse> getBalance(@PathVariable Long userId) {
        Long balance = pointService.getBalance(userId);
        return ResponseEntity.ok(PointDto.BalanceResponse.of(userId, balance));
    }
    
    @GetMapping("/users/{userId}/history")
    public ResponseEntity<Page<PointDto.Response>> getPointHistory(
            @PathVariable Long userId,
            Pageable pageable) {
        Page<Point> points = pointService.getPointHistory(userId, pageable);
        Page<PointDto.Response> responses = points.map(PointDto.Response::from);
        return ResponseEntity.ok(responses);
    }
}
