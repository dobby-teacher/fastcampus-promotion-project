package com.fastcampus.couponservice.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserIdInterceptorTest {

    @InjectMocks
    private UserIdInterceptor interceptor;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Test
    @DisplayName("X-USER-ID 헤더가 있을 경우 성공")
    void preHandle_Success() {
        // Given
        when(request.getHeader("X-USER-ID")).thenReturn("1");

        // When
        boolean result = interceptor.preHandle(request, response, null);

        // Then
        assertThat(result).isTrue();
        assertThat(UserIdInterceptor.getCurrentUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("X-USER-ID 헤더가 없을 경우 실패")
    void preHandle_Fail_NoHeader() {
        // Given
        when(request.getHeader("X-USER-ID")).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> interceptor.preHandle(request, response, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("X-USER-ID header is required");
    }

    @Test
    @DisplayName("X-USER-ID 헤더가 잘못된 형식일 경우 실패")
    void preHandle_Fail_InvalidFormat() {
        // Given
        when(request.getHeader("X-USER-ID")).thenReturn("invalid");

        // When & Then
        assertThatThrownBy(() -> interceptor.preHandle(request, response, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invalid X-USER-ID format");
    }

    @Test
    @DisplayName("afterCompletion 호출 시 ThreadLocal 정리")
    void afterCompletion_CleansThreadLocal() {
        // Given
        when(request.getHeader("X-USER-ID")).thenReturn("1");
        interceptor.preHandle(request, response, null);

        // When
        interceptor.afterCompletion(request, response, null, null);

        // Then
        assertThatThrownBy(UserIdInterceptor::getCurrentUserId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User ID not found in current context");
    }
}
